package pe.edu.upeu.bomerp.ventas.service;

import lombok.RequiredArgsConstructor;
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
import pe.edu.upeu.bomerp.ventas.entity.EstadoVenta;
import pe.edu.upeu.bomerp.ventas.entity.MetodoPagoVenta;
import pe.edu.upeu.bomerp.ventas.entity.Venta;
import pe.edu.upeu.bomerp.ventas.repository.VentaRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VentaServiceImpl implements VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoService productoService;
    private final InventarioService inventarioService;
    private final FinanzasApi finanzasApi;

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponse> buscar(EstadoVenta estado, LocalDateTime desde, LocalDateTime hasta,
                                      String ordenarPor, String direccion, String vendedorId) {
        Sort.Direction dir = "ASC".equalsIgnoreCase(direccion) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String campoOrden = (ordenarPor != null && !ordenarPor.isBlank()) ? ordenarPor : "fecha";
        Sort sort = Sort.by(dir, campoOrden);

        return ventaRepository.buscar(estado, desde, hasta, vendedorId, sort).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VentaResponse obtener(Long id, String vendedorId) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con ID: " + id));

        // Regla RN8 (ADS S8/S10): VENDEDOR solo puede consultar sus propias ventas (ABAC)
        if (vendedorId != null && !vendedorId.equals(venta.getVendedorId())) {
            throw new AccesoDenegadoException("No tienes acceso a esta venta: pertenece a otro vendedor.");
        }

        return toResponse(venta);
    }

    @Override
    @Transactional
    public VentaResponse crear(VentaRequest request, String vendedorId) {
        String ticket = "TKT-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + (int) (Math.random() * 900 + 100);

        MetodoPagoVenta metodoVenta = request.getMetodoPago() != null ? request.getMetodoPago() : MetodoPagoVenta.EFECTIVO;
        String cliente = (request.getClienteNombre() != null && !request.getClienteNombre().isBlank())
                ? request.getClienteNombre().trim()
                : "Cliente General";

        Venta venta = Venta.builder()
                .numeroTicket(ticket)
                .fecha(LocalDateTime.now())
                .estado(EstadoVenta.REGISTRADA)
                .metodoPago(metodoVenta)
                .clienteNombre(cliente)
                .vendedorId(vendedorId)
                .detalles(new ArrayList<>())
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (DetalleVentaRequest item : request.getDetalles()) {
            // 1. Verificación y obtención de datos del producto en Catálogo
            ProductoResponse prod = productoService.obtener(item.getProductoId());

            // 2. Descuento atómico en Catálogo (lanza BusinessConflictException y revierte todo si no alcanza)
            productoService.descontarStock(item.getProductoId(), item.getCantidad());

            // 3. Despacho FEFO y registro automático de SALIDA en Kardex Físico
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

        BigDecimal igv = total.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = total.subtract(igv);

        venta.setSubtotal(subtotal);
        venta.setIgv(igv);
        venta.setTotal(total);

        Venta guardada = ventaRepository.save(venta);

        // 4. Integración Financiera Automática (Sesión de Caja Activa o Cuenta por Cobrar)
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
                        cliente
                );
            }
        }

        return toResponse(guardada);
    }

    @Override
    @Transactional
    public VentaResponse anular(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con ID: " + id));

        if (venta.getEstado() != EstadoVenta.REGISTRADA) {
            throw new VentaYaAnuladaException("La venta " + id + " ya está anulada");
        }

        // 1. Reposición atómica en Catálogo, Lote FEFO y Kardex
        if (venta.getDetalles() != null) {
            for (DetalleVenta detalle : venta.getDetalles()) {
                if (detalle.getProductoId() != null && detalle.getCantidad() != null) {
                    productoService.restaurarStock(detalle.getProductoId(), detalle.getCantidad());
                    if (inventarioService != null) {
                        inventarioService.revertirSalidaVenta(
                                detalle.getProductoId(),
                                detalle.getNombreProducto(),
                                detalle.getLoteId(),
                                detalle.getCantidad(),
                                venta.getNumeroTicket()
                        );
                    }
                }
            }
        }

        // 2. Extorno financiero en Caja / Cuentas por Cobrar / Transacciones
        if (finanzasApi != null) {
            boolean eraCredito = venta.getMetodoPago() == MetodoPagoVenta.CREDITO;
            MetodoPago mp = eraCredito ? null : mapearMetodoPagoFinanzas(venta.getMetodoPago());
            finanzasApi.revertirVentaPos(venta.getNumeroTicket(), venta.getTotal(), mp, eraCredito);
        }

        venta.setEstado(EstadoVenta.ANULADA);
        Venta anulada = ventaRepository.save(venta);
        return toResponse(anulada);
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
    public VentaReporte reporte(EstadoVenta estado, LocalDateTime desde, LocalDateTime hasta) {
        VentaAgregado agregado = ventaRepository.agregados(estado, desde, hasta);
        Sort sort = Sort.by(Sort.Direction.DESC, "fecha");
        List<VentaResumen> resumenes = ventaRepository.buscarResumen(estado, desde, hasta, sort);
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
                .detalles(detalles)
                .build();
    }

    @Override
    @Transactional
    public void eliminarTodas() {
        ventaRepository.deleteAll();
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Venta v = ventaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con ID: " + id));
        ventaRepository.delete(v);
    }
}
