package pe.edu.upeu.bomerp.ventas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.bomerp.catalogo.producto.dto.ProductoResponse;
import pe.edu.upeu.bomerp.catalogo.producto.service.ProductoService;
import pe.edu.upeu.bomerp.exception.AccesoDenegadoException;
import pe.edu.upeu.bomerp.exception.ResourceNotFoundException;
import pe.edu.upeu.bomerp.exception.VentaYaAnuladaException;
import pe.edu.upeu.bomerp.finanzas.api.FinanzasApi;
import pe.edu.upeu.bomerp.finanzas.entity.MetodoPago;
import pe.edu.upeu.bomerp.inventario.service.InventarioService;
import pe.edu.upeu.bomerp.ventas.dto.*;
import pe.edu.upeu.bomerp.ventas.entity.DetalleVenta;
import pe.edu.upeu.bomerp.ventas.entity.EstadoDelivery;
import pe.edu.upeu.bomerp.ventas.entity.EstadoVenta;
import pe.edu.upeu.bomerp.ventas.entity.MetodoPagoVenta;
import pe.edu.upeu.bomerp.ventas.entity.PedidoDelivery;
import pe.edu.upeu.bomerp.ventas.entity.Venta;
import pe.edu.upeu.bomerp.ventas.repository.PedidoDeliveryRepository;
import pe.edu.upeu.bomerp.ventas.repository.VentaRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VentaServiceImpl implements VentaService {

    private final VentaRepository ventaRepository;
    private final PedidoDeliveryRepository deliveryRepository;
    private final ProductoService productoService;
    private final InventarioService inventarioService;
    private final FinanzasApi finanzasApi;
    private final AuditoriaService auditoriaService;
    private final NumeroTicketService numeroTicketService;

    @Override
    @Transactional(readOnly = true)
    public Page<VentaResponse> buscar(EstadoVenta estado, MetodoPagoVenta metodoPago, LocalDateTime desde, LocalDateTime hasta,
                                      String vendedorId, Pageable pageable) {
        return ventaRepository.buscar(estado, metodoPago, desde, hasta, vendedorId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponse> buscar(EstadoVenta estado, MetodoPagoVenta metodoPago, LocalDateTime desde, LocalDateTime hasta,
                                      String ordenarPor, String direccion, String vendedorId) {
        Sort.Direction dir = "ASC".equalsIgnoreCase(direccion) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String campoOrden = (ordenarPor != null && !ordenarPor.isBlank()) ? ordenarPor : "fecha";
        Sort sort = Sort.by(dir, campoOrden);

        return ventaRepository.buscar(estado, metodoPago, desde, hasta, vendedorId, sort).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VentaResponse obtener(Long id, String vendedorId) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con ID: " + id));

        // [RN-VEN-08] Aislamiento Multi-Tenant por Vendedor (ABAC):
        // Un VENDEDOR solo puede consultar sus propias ventas. Si intenta ver la de otro vendedor se rechaza.
        if (vendedorId != null && !vendedorId.equals(venta.getVendedorId())) {
            throw new AccesoDenegadoException("No tienes acceso a esta venta: pertenece a otro vendedor.");
        }

        return toResponse(venta);
    }

    @Override
    @Transactional
    public VentaResponse crear(VentaRequest request, String vendedorId) {
        // [RN-VEN-06] Idempotencia Transaccional (Protección Anti-Doble Clic):
        // Si el cliente envía un operacionId ya procesado, se retorna la venta existente sin duplicar cobro ni descontar stock
        if (request.getOperacionId() != null && !request.getOperacionId().isBlank()) {
            var existente = ventaRepository.findByOperacionId(request.getOperacionId().trim());
            if (existente.isPresent()) {
                log.warn("Operación duplicada detectada (operacionId={}). Se retorna la venta existente {}.",
                        request.getOperacionId(), existente.get().getNumeroTicket());
                return toResponse(existente.get());
            }
        }

        // [RN-VEN-04] Correlativo Oficial Único e Inmutable:
        // Generado mediante secuencia nativa de base de datos (SEQ_TICKET_VENTA) -> TKT-S-XXXXXXXXXX
        String ticket = numeroTicketService.siguiente();

        MetodoPagoVenta metodoVenta = request.getMetodoPago() != null ? request.getMetodoPago() : MetodoPagoVenta.EFECTIVO;
        String cliente = (request.getClienteNombre() != null && !request.getClienteNombre().isBlank())
                ? request.getClienteNombre().trim()
                : "Cliente General";

        // [RN-VEN-05] Atribución Nominal Criptográfica:
        // vendedorId se extrae obligatoriamente del claim 'sub' del JWT autenticado, nunca del body del cliente
        Venta venta = Venta.builder()
                .numeroTicket(ticket)
                .fecha(LocalDateTime.now())
                .estado(EstadoVenta.REGISTRADA)
                .metodoPago(metodoVenta)
                .clienteNombre(cliente)
                .vendedorId(vendedorId)
                .sesionCajaId(request.getSesionCajaId())
                .operacionId(request.getOperacionId() != null ? request.getOperacionId().trim() : null)
                .detalles(new ArrayList<>())
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (DetalleVentaRequest item : request.getDetalles()) {
            // 1. Verificación y obtención de datos del producto en Catálogo
            ProductoResponse prod = productoService.obtener(item.getProductoId());

            // [RN-VEN-01] Control Atómico de Existencias con Rollback:
            // Descuenta con bloqueo pesimista en BD (SELECT FOR UPDATE). Si no alcanza, lanza BusinessConflictException y aborta
            productoService.descontarStock(item.getProductoId(), item.getCantidad());

            // [RN-VEN-03] Despacho Sanitario FEFO (First Expired, First Out):
            // Descuenta prioritariamente de los lotes más próximos a vencer y asienta la SALIDA en el Kardex Físico
            Long loteConsumidoId = item.getLoteId();
            if (inventarioService != null) {
                Long loteFefoId = inventarioService.registrarSalidaVentaFEFO(
                        prod.getId(),
                        prod.getNombre(),
                        item.getCantidad(),
                        ticket
                );
                if (loteConsumidoId == null) {
                    loteConsumidoId = loteFefoId;
                }
            }

            BigDecimal subtotalItem = prod.getPrecio().multiply(BigDecimal.valueOf(item.getCantidad()));

            DetalleVenta detalle = DetalleVenta.builder()
                    .venta(venta)
                    .productoId(prod.getId())
                    .loteId(loteConsumidoId)
                    .nombreProducto(prod.getNombre())
                    .precioUnitario(prod.getPrecio())
                    .cantidad(item.getCantidad())
                    .subtotal(subtotalItem)
                    .build();

            venta.getDetalles().add(detalle);
            total = total.add(subtotalItem);
        }

        // [RN-VEN-02] Desglose Tributario Peruano (IGV 18%):
        // Precios finales con IGV incluido. Subtotal = Total / 1.18 ; IGV = Total - Subtotal (RoundingMode.HALF_UP)
        total = total.setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = total.divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(subtotal);

        venta.setSubtotal(subtotal);
        venta.setIgv(igv);
        venta.setTotal(total);

        Venta guardada = ventaRepository.save(venta);
        auditoriaService.registrar(vendedorId, "CREAR", "VENTA", guardada.getId(), "total=" + total);

        // [RN-VEN-10] Vinculación Financiera según Modalidad de Pago:
        // - Si es CREDITO: Genera Cuenta por Cobrar a plazo (15 a 60 días)
        // - Si es EFECTIVO: Exige turno de caja ABIERTO en sesionCajaId para registrar en gaveta
        if (finanzasApi != null) {
            if (metodoVenta == MetodoPagoVenta.CREDITO) {
                finanzasApi.registrarVentaACredito(
                        ticket,
                        cliente,
                        request.getClienteDocumento(),
                        total,
                        request.getDiasCredito()
                );
            } else {
                finanzasApi.registrarIngresoVentaPos(
                        total,
                        mapearMetodoPagoFinanzas(metodoVenta),
                        ticket,
                        cliente,
                        request.getSesionCajaId(),
                        vendedorId
                );
            }
        }

        return toResponse(guardada);
    }

    @Override
    @Transactional
    public VentaResponse anular(Long id) {
        return anular(id, null);
    }

    @Override
    @Transactional
    public VentaResponse anular(Long id, String vendedorId) {
        // [RN-VEN-07] Segregación de Roles y Control de Doble Anulación:
        // Bloqueo pesimista (SELECT FOR UPDATE) para evitar que dos transacciones concurrentes anulen la misma venta.
        Venta venta = ventaRepository.findByIdForUpdate(id)
                .or(() -> ventaRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con ID: " + id));

        // Idempotencia de anulación: rechazar si ya fue previamente anulada
        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new VentaYaAnuladaException("La venta " + id + " ya está anulada");
        }

        // 1. [RN-VEN-01] Reposición Atómica en Catálogo del total vendido por producto
        if (venta.getDetalles() != null) {
            for (DetalleVenta detalle : venta.getDetalles()) {
                if (detalle.getProductoId() != null && detalle.getCantidad() != null) {
                    productoService.restaurarStock(detalle.getProductoId(), detalle.getCantidad());
                }
            }
        }

        // 2. [RN-VEN-09] Reversión Multi-lote FEFO y Compensación de Kardex Físico:
        // Consulta los lotes específicos de donde salió la mercadería y genera contra-asientos ENTRADA
        if (inventarioService != null) {
            inventarioService.revertirSalidaVentaPorTicket(venta.getNumeroTicket());
        }

        // 3. [RN-VEN-10] / [RN-FIN-07] Extorno Financiero Trazable:
        // Si la sesión de caja está cerrada, no altera el arqueo histórico sellado; registra EXTORNO_POS
        if (finanzasApi != null) {
            boolean eraCredito = venta.getMetodoPago() == MetodoPagoVenta.CREDITO;
            MetodoPago mp = eraCredito ? null : mapearMetodoPagoFinanzas(venta.getMetodoPago());
            finanzasApi.revertirVentaPos(venta.getNumeroTicket(), venta.getTotal(), mp, eraCredito, venta.getSesionCajaId());
        }

        venta.setEstado(EstadoVenta.ANULADA);
        Venta anulada = ventaRepository.save(venta);
        auditoriaService.registrar(vendedorId, "ANULAR", "VENTA", anulada.getId(), "ticket=" + anulada.getNumeroTicket());
        return toResponse(anulada);
    }

    @Override
    @Transactional
    public VentaResponse solicitarAnulacion(Long id, String motivo, String actor, String vendedorIdRestringido) {
        // [RN-VEN-07] Solicitud de Anulación por Vendedor:
        // El vendedor en mostrador no puede anular directamente; debe enviar una solicitud formal con motivo justificado
        Venta venta = ventaRepository.findByIdForUpdate(id)
                .or(() -> ventaRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con ID: " + id));

        // [RN-VEN-08] Aislamiento ABAC:
        // Un vendedor solo tiene potestad para solicitar la anulación de sus propias operaciones registradas
        if (vendedorIdRestringido != null && !vendedorIdRestringido.equals(venta.getVendedorId())) {
            throw new AccesoDenegadoException("No puedes solicitar la anulación de una venta de otro vendedor.");
        }

        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new VentaYaAnuladaException("La venta " + id + " ya está anulada.");
        }
        if (venta.getEstado() == EstadoVenta.SOLICITADA_ANULACION) {
            throw new IllegalStateException("La venta " + id + " ya tiene una solicitud de anulación en curso.");
        }

        venta.setEstado(EstadoVenta.SOLICITADA_ANULACION);
        Venta guardada = ventaRepository.save(venta);
        auditoriaService.registrar(actor, "SOLICITAR_ANULACION", "VENTA", guardada.getId(),
                "ticket=" + guardada.getNumeroTicket() + (motivo != null && !motivo.isBlank() ? "; motivo=" + motivo : ""));
        return toResponse(guardada);
    }

    @Override
    @Transactional
    public VentaResponse rechazarAnulacion(Long id, String actor) {
        // [RN-VEN-07] Resolución de Solicitud por Supervisor:
        // Si el motivo no procede, el supervisor devuelve la venta al estado regular REGISTRADA sin extornar stock ni dinero
        Venta venta = ventaRepository.findByIdForUpdate(id)
                .or(() -> ventaRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con ID: " + id));

        if (venta.getEstado() != EstadoVenta.SOLICITADA_ANULACION) {
            throw new IllegalStateException("La venta " + id + " no tiene solicitud de anulación pendiente.");
        }

        venta.setEstado(EstadoVenta.REGISTRADA);
        Venta guardada = ventaRepository.save(venta);
        auditoriaService.registrar(actor, "RECHAZAR_ANULACION", "VENTA", guardada.getId(), "ticket=" + guardada.getNumeroTicket());
        return toResponse(guardada);
    }

    private MetodoPago mapearMetodoPagoFinanzas(MetodoPagoVenta mp) {
        if (mp == null) {
            return MetodoPago.EFECTIVO;
        }
        return switch (mp) {
            case EFECTIVO -> MetodoPago.EFECTIVO;
            case YAPE -> MetodoPago.YAPE;
            case PLIN -> MetodoPago.PLIN;
            case TRANSFERENCIA -> MetodoPago.TRANSFERENCIA;
            case TARJETA -> MetodoPago.TARJETA;
            case CREDITO -> MetodoPago.TRANSFERENCIA;
        };
    }

    @Override
    @Transactional(readOnly = true)
    public VentaReporte reporte(EstadoVenta estado, MetodoPagoVenta metodoPago, LocalDateTime desde, LocalDateTime hasta, String vendedorId) {
        // [RN-VEN-08] Aislamiento ABAC en Reportería:
        // Si vendedorId != null (rol VENDEDOR), los agregados y listas se filtran estrictamente a sus ventas.
        // Si vendedorId == null (rol SUPERVISOR / ADMIN), se obtienen los totales globales del ERP.
        VentaAgregado agregado = ventaRepository.agregados(estado, metodoPago, desde, hasta, vendedorId);
        Sort sort = Sort.by(Sort.Direction.DESC, "fecha");
        List<VentaResumen> resumenes = ventaRepository.buscarResumen(estado, metodoPago, desde, hasta, vendedorId, sort);
        return new VentaReporte(agregado, resumenes);
    }

    private VentaResponse toResponse(Venta v) {
        List<DetalleVentaResponse> detalles = v.getDetalles() != null
                ? v.getDetalles().stream().map(d -> DetalleVentaResponse.builder()
                .id(d.getId())
                .productoId(d.getProductoId())
                .loteId(d.getLoteId())
                .nombreProducto(d.getNombreProducto())
                .precioUnitario(d.getPrecioUnitario())
                .cantidad(d.getCantidad())
                .subtotal(d.getSubtotal())
                .build()).toList()
                : List.of();

        return VentaResponse.builder()
                .id(v.getId())
                .numeroTicket(v.getNumeroTicket())
                .fecha(v.getFecha())
                .estado(v.getEstado())
                .metodoPago(v.getMetodoPago())
                .subtotal(v.getSubtotal())
                .igv(v.getIgv())
                .total(v.getTotal())
                .clienteNombre(v.getClienteNombre())
                .vendedorId(v.getVendedorId())
                .sesionCajaId(v.getSesionCajaId())
                .operacionId(v.getOperacionId())
                .detalles(detalles)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteOmnicanalResponse obtenerReporteOmnicanal() {
        List<Venta> ventas = ventaRepository.findAll().stream()
                .filter(v -> v.getEstado() != EstadoVenta.ANULADA).toList();
        List<PedidoDelivery> pedidos = deliveryRepository.findAll().stream()
                .filter(d -> d.getEstadoDelivery() != EstadoDelivery.CANCELADO).toList();

        // Una venta vinculada a delivery se cuenta una sola vez, usando el importe de la venta.
        Set<Long> ventasDelivery = pedidos.stream().map(PedidoDelivery::getVentaId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        List<Venta> mostrador = ventas.stream().filter(v -> !ventasDelivery.contains(v.getId())).toList();
        List<Venta> vinculadas = ventas.stream().filter(v -> ventasDelivery.contains(v.getId())).toList();
        // Los pedidos independientes se reconocen al entregar, como en DeliveryServiceImpl.
        List<PedidoDelivery> independientes = pedidos.stream()
                .filter(d -> d.getVentaId() == null && d.getEstadoDelivery() == EstadoDelivery.ENTREGADO).toList();
        BigDecimal totalPos = mostrador.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDelivery = vinculadas.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(independientes.stream().map(PedidoDelivery::getMontoTotal)
                        .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal total = totalPos.add(totalDelivery);
        long cantidadDelivery = vinculadas.size() + independientes.size();

        return ReporteOmnicanalResponse.builder()
                .totalVentasOmnicanal(total)
                .totalTransacciones(mostrador.size() + cantidadDelivery)
                .canales(List.of(
                        canal("TIENDA_MOSTRADOR", "Tienda Física / Mostrador", totalPos, mostrador.size(), total),
                        canal("DELIVERY", "Canal Delivery / Envíos", totalDelivery, cantidadDelivery, total)))
                .rankingProductos(rankingProductos(ventas))
                .build();
    }

    private CanalVentaResumenDto canal(String codigo, String nombre, BigDecimal monto, long cantidad, BigDecimal total) {
        return CanalVentaResumenDto.builder().canal(codigo).canalNombre(nombre).totalVentas(monto)
                .cantidadTransacciones(cantidad)
                .ticketPromedio(cantidad == 0 ? BigDecimal.ZERO : monto.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP))
                .porcentaje(total.signum() == 0 ? BigDecimal.ZERO : monto.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP))
                .build();
    }

    private List<ProductoRentabilidadDto> rankingProductos(List<Venta> ventas) {
        Map<Long, ProductoRentabilidadDto> productos = new LinkedHashMap<>();
        for (Venta venta : ventas) {
            for (DetalleVenta detalle : venta.getDetalles()) {
                ProductoRentabilidadDto fila = productos.computeIfAbsent(detalle.getProductoId(), id ->
                        ProductoRentabilidadDto.builder().productoId(id).productoNombre(detalle.getNombreProducto())
                                .unidadesVendidas(BigDecimal.ZERO).totalIngresos(BigDecimal.ZERO).build());
                fila.setUnidadesVendidas(fila.getUnidadesVendidas().add(BigDecimal.valueOf(detalle.getCantidad())));
                fila.setTotalIngresos(fila.getTotalIngresos().add(detalle.getSubtotal()));
            }
        }
        // No hay un costo histórico por venta: no se inventan utilidades, márgenes ni categorías.
        return productos.values().stream()
                .sorted(Comparator.comparing(ProductoRentabilidadDto::getTotalIngresos).reversed()
                        .thenComparing(ProductoRentabilidadDto::getProductoId))
                .toList();
    }
}
