package pe.edu.upeu.bomerp.compras.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.bomerp.compras.dto.*;
import pe.edu.upeu.bomerp.compras.entity.*;
import pe.edu.upeu.bomerp.compras.repository.CompraRepository;
import pe.edu.upeu.bomerp.compras.repository.InsumoRepository;
import pe.edu.upeu.bomerp.compras.repository.PagoCompraRepository;
import pe.edu.upeu.bomerp.compras.repository.ProveedorRepository;
import pe.edu.upeu.bomerp.exception.BusinessConflictException;
import pe.edu.upeu.bomerp.exception.ResourceNotFoundException;
import pe.edu.upeu.bomerp.finanzas.api.FinanzasApi;
import pe.edu.upeu.bomerp.finanzas.entity.MetodoPago;
import pe.edu.upeu.bomerp.inventario.service.InventarioService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompraServiceImpl implements CompraService {

    private final CompraRepository compraRepository;
    private final ProveedorRepository proveedorRepository;
    private final PagoCompraRepository pagoCompraRepository;
    private final InsumoRepository insumoRepository;
    private final InventarioService inventarioService;
    private final FinanzasApi finanzasApi;

    @PostConstruct
    public void seedInsumosIniciales() {
        if (insumoRepository.count() == 0) {
            log.info("Sembrando catálogo maestro de insumos para Agro Peruvian Foods...");
            List<Insumo> insumos = List.of(
                    Insumo.builder().codigo("INS-CAC-01").nombre("Cacao Nativo Chuncho en Grano Seleccionado").unidadMedida("KG").precioSugerido(new BigDecimal("20.00")).estado("ACTIVO").build(),
                    Insumo.builder().codigo("INS-COC-01").nombre("Cocos Frescos Orgánicos del Chira para Prensado").unidadMedida("UNID").precioSugerido(new BigDecimal("2.20")).estado("ACTIVO").build(),
                    Insumo.builder().codigo("INS-COP-01").nombre("Copra Blanca Deshidratada Grado Alimentario").unidadMedida("KG").precioSugerido(new BigDecimal("9.50")).estado("ACTIVO").build(),
                    Insumo.builder().codigo("INS-ENV-30").nombre("Frasco de Vidrio Grado Alimentario 30ml + Tapa").unidadMedida("UNID").precioSugerido(new BigDecimal("1.10")).estado("ACTIVO").build(),
                    Insumo.builder().codigo("INS-ENV-120").nombre("Frasco de Vidrio Grado Alimentario 120ml + Tapa").unidadMedida("UNID").precioSugerido(new BigDecimal("1.60")).estado("ACTIVO").build(),
                    Insumo.builder().codigo("INS-ENV-250").nombre("Frasco de Vidrio Grado Alimentario 250ml + Tapa Twist-Off").unidadMedida("UNID").precioSugerido(new BigDecimal("2.10")).estado("ACTIVO").build(),
                    Insumo.builder().codigo("INS-ENV-460").nombre("Frasco de Vidrio Grado Alimentario 460ml + Tapa Twist-Off").unidadMedida("UNID").precioSugerido(new BigDecimal("2.65")).estado("ACTIVO").build(),
                    Insumo.builder().codigo("INS-ENV-1L").nombre("Frasco de Vidrio Grado Alimentario 1L Boca Ancha").unidadMedida("UNID").precioSugerido(new BigDecimal("3.80")).estado("ACTIVO").build(),
                    Insumo.builder().codigo("INS-ENV-4L").nombre("Balde HDPE Grado Alimentario 4L con Asa y Tapa Precinto").unidadMedida("UNID").precioSugerido(new BigDecimal("8.50")).estado("ACTIVO").build(),
                    Insumo.builder().codigo("INS-ENV-20L").nombre("Balde Industrial HDPE 20L Grado Alimentario").unidadMedida("UNID").precioSugerido(new BigDecimal("24.00")).estado("ACTIVO").build(),
                    Insumo.builder().codigo("INS-EMP-DP").nombre("Bolsa Doypack Kraft Bilaminada con Zipper 250g/500g").unidadMedida("MILLAR").precioSugerido(new BigDecimal("420.00")).estado("ACTIVO").build(),
                    Insumo.builder().codigo("INS-ETQ-BOPP").nombre("Rollo Etiquetas Impermeables BOPP + Precintos Termocontraíbles").unidadMedida("ROLLO").precioSugerido(new BigDecimal("95.00")).estado("ACTIVO").build(),
                    Insumo.builder().codigo("INS-CAJ-24").nombre("Caja de Cartón Corrugado Bandeja 24/36 Unidades Exportación").unidadMedida("UNID").precioSugerido(new BigDecimal("3.40")).estado("ACTIVO").build()
            );
            insumoRepository.saveAll(insumos);
            log.info("Catálogo maestro de insumos inicializado con {} ítems.", insumos.size());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsumoDto> listarInsumos() {
        return insumoRepository.findAll().stream()
                .map(i -> InsumoDto.builder()
                        .id(i.getId())
                        .codigo(i.getCodigo())
                        .nombre(i.getNombre())
                        .unidadMedida(i.getUnidadMedida())
                        .precioSugerido(i.getPrecioSugerido())
                        .estado(i.getEstado())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public ProveedorResponse crearProveedor(ProveedorRequest request) {
        proveedorRepository.findByRuc(request.getRuc()).ifPresent(p -> {
            throw new BusinessConflictException("Ya existe un proveedor registrado con el RUC: " + request.getRuc());
        });

        Proveedor proveedor = Proveedor.builder()
                .ruc(request.getRuc())
                .razonSocial(request.getRazonSocial())
                .telefono(request.getTelefono())
                .email(request.getEmail())
                .direccion(request.getDireccion())
                .build();

        return toProveedorResponse(proveedorRepository.save(proveedor));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorResponse> listarProveedores() {
        return proveedorRepository.findAll().stream()
                .map(this::toProveedorResponse)
                .toList();
    }

    @Override
    @Transactional
    public CompraResponse registrarCompra(CompraRequest request) {
        Proveedor proveedor = proveedorRepository.findById(request.getProveedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con ID: " + request.getProveedorId()));

        String serie = (request.getSerieComprobante() != null && !request.getSerieComprobante().isBlank())
                ? request.getSerieComprobante().trim().toUpperCase()
                : (request.getNumeroComprobante().contains("-") ? request.getNumeroComprobante().split("-")[0] : "F001");

        String condicion = (request.getCondicionPago() != null && !request.getCondicionPago().isBlank())
                ? request.getCondicionPago().trim().toUpperCase()
                : "CONTADO";

        LocalDate fechaEmision = request.getFechaEmision() != null ? request.getFechaEmision() : LocalDate.now();
        LocalDate vencimientoPago = request.getFechaVencimientoPago();
        if (vencimientoPago == null) {
            vencimientoPago = condicion.contains("CREDITO") ? fechaEmision.plusDays(30) : fechaEmision;
        }

        BigDecimal flete = request.getCostoFlete() != null && request.getCostoFlete().compareTo(BigDecimal.ZERO) > 0
                ? request.getCostoFlete().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Compra compra = Compra.builder()
                .serieComprobante(serie)
                .numeroComprobante(request.getNumeroComprobante())
                .proveedor(proveedor)
                .fechaEmision(fechaEmision)
                .condicionPago(condicion)
                .fechaVencimientoPago(vencimientoPago)
                .costoFlete(flete)
                .estado(EstadoCompra.REGISTRADA)
                .detalles(new ArrayList<>())
                .pagos(new ArrayList<>())
                .build();

        BigDecimal subtotalAcumulado = BigDecimal.ZERO;

        for (DetalleCompraRequest item : request.getDetalles()) {
            BigDecimal factor = (item.getFactorConversion() != null && item.getFactorConversion().compareTo(BigDecimal.ZERO) > 0)
                    ? item.getFactorConversion()
                    : BigDecimal.ONE;

            BigDecimal cantidadIngreso = item.getCantidad().multiply(factor).setScale(3, RoundingMode.HALF_UP);
            BigDecimal subtotalItem = item.getPrecioUnitario().multiply(item.getCantidad()).setScale(2, RoundingMode.HALF_UP);

            DetalleCompra detalle = DetalleCompra.builder()
                    .compra(compra)
                    .insumoId(item.getInsumoId())
                    .nombreInsumo(item.getNombreInsumo())
                    .loteProveedor(item.getLoteProveedor())
                    .fechaVencimiento(item.getFechaVencimiento())
                    .factorConversion(factor)
                    .cantidad(item.getCantidad())
                    .cantidadIngreso(cantidadIngreso)
                    .precioUnitario(item.getPrecioUnitario())
                    .subtotal(subtotalItem)
                    .build();

            compra.getDetalles().add(detalle);
            subtotalAcumulado = subtotalAcumulado.add(subtotalItem);

            // RN-COM-01: Ingreso automático del lote de insumo a Almacén y Kardex Físico con trazabilidad
            if (inventarioService != null) {
                inventarioService.registrarIngresoCompraInsumo(
                        item.getInsumoId(),
                        item.getNombreInsumo(),
                        cantidadIngreso,
                        item.getPrecioUnitario(),
                        request.getNumeroComprobante(),
                        item.getLoteProveedor(),
                        item.getFechaVencimiento()
                );
            }
        }

        BigDecimal igv = subtotalAcumulado.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotalAcumulado.add(igv).add(flete);

        compra.setSubtotal(subtotalAcumulado);
        compra.setIgv(igv);
        compra.setTotal(total);
        compra.setSaldoPendiente(total);

        Compra guardada = compraRepository.save(compra);

        // Integración Financiera: Asentar cuenta por pagar (EGRESO PENDIENTE) en Finanzas
        if (finanzasApi != null) {
            finanzasApi.registrarFacturaCompraPendiente(
                    guardada.getNumeroComprobante(),
                    proveedor.getRazonSocial(),
                    total,
                    guardada.getFechaEmision()
            );
        }

        return toCompraResponse(guardada);
    }

    @Override
    @Transactional
    public CompraResponse amortizarPago(Long compraId, AmortizarPagoRequest request) {
        Compra compra = compraRepository.findById(compraId)
                .orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada con ID: " + compraId));

        if (compra.getEstado() == EstadoCompra.PAGADA) {
            throw new BusinessConflictException("La compra ya se encuentra totalmente cancelada/pagada.");
        }

        if (compra.getEstado() == EstadoCompra.ANULADA) {
            throw new BusinessConflictException("No se puede amortizar una compra que ha sido anulada.");
        }

        if (request.getMontoPago().compareTo(compra.getSaldoPendiente()) > 0) {
            throw new BusinessConflictException("El monto a pagar (" + request.getMontoPago()
                    + ") no puede ser superior al saldo pendiente (" + compra.getSaldoPendiente() + ").");
        }

        BigDecimal nuevoSaldo = compra.getSaldoPendiente().subtract(request.getMontoPago());
        boolean liquidadaTotalmente = nuevoSaldo.compareTo(BigDecimal.ZERO) == 0;

        // 1. Integración Financiera primero: Registrar salida en Sesión de Caja Activa (con control RN-FIN-01) y Finanzas
        if (finanzasApi != null) {
            MetodoPago mp = resolverMetodoPago(request.getMetodoPago());
            finanzasApi.registrarPagoProveedorCompra(
                    compra.getNumeroComprobante(),
                    compra.getProveedor().getRazonSocial(),
                    request.getMontoPago(),
                    mp,
                    liquidadaTotalmente
            );
        }

        // 2. Registro del comprobante individual de pago (Historial de Amortizaciones)
        PagoCompra pago = PagoCompra.builder()
                .compra(compra)
                .fechaPago(LocalDate.now())
                .montoPagado(request.getMontoPago())
                .metodoPago(request.getMetodoPago() != null ? request.getMetodoPago() : "TRANSFERENCIA")
                .numeroOperacion(request.getReferencia() != null && !request.getReferencia().isBlank() ? request.getReferencia() : "OP-AMORT-" + System.currentTimeMillis() % 100000)
                .estado("APLICADO")
                .fechaCreacion(LocalDateTime.now())
                .build();

        pagoCompraRepository.save(pago);
        if (compra.getPagos() == null) {
            compra.setPagos(new ArrayList<>());
        }
        compra.getPagos().add(pago);

        // 3. Actualizar saldo y estado de la compra únicamente tras confirmar solvencia financiera
        compra.setSaldoPendiente(nuevoSaldo);
        if (liquidadaTotalmente) {
            compra.setEstado(EstadoCompra.PAGADA);
        }

        return toCompraResponse(compraRepository.save(compra));
    }

    @Override
    @Transactional
    public CompraResponse anularCompra(Long id) {
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada con ID: " + id));

        if (compra.getEstado() == EstadoCompra.ANULADA) {
            throw new BusinessConflictException("La compra ya se encuentra anulada.");
        }

        compra.setEstado(EstadoCompra.ANULADA);
        compra.setSaldoPendiente(BigDecimal.ZERO);

        if (compra.getPagos() != null) {
            for (PagoCompra p : compra.getPagos()) {
                p.setEstado("ANULADO");
                pagoCompraRepository.save(p);
            }
        }

        if (finanzasApi != null) {
            finanzasApi.anularFacturaCompra(compra.getNumeroComprobante());
        }

        return toCompraResponse(compraRepository.save(compra));
    }

    private MetodoPago resolverMetodoPago(String raw) {
        if (raw == null || raw.isBlank()) {
            return MetodoPago.TRANSFERENCIA;
        }
        try {
            return MetodoPago.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MetodoPago.TRANSFERENCIA;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CompraResponse obtener(Long id) {
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada con ID: " + id));
        return toCompraResponse(compra);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompraResponse> listarCompras() {
        return compraRepository.findAll().stream()
                .map(this::toCompraResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompraResponse> listarCompras(EstadoCompra estado, LocalDate desde, LocalDate hasta) {
        return compraRepository.buscar(estado, desde, hasta).stream()
                .map(this::toCompraResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoCompraResponse> listarPagosPorCompra(Long compraId) {
        return pagoCompraRepository.findByCompraIdOrderByFechaPagoAsc(compraId).stream()
                .map(this::toPagoResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CompraReporte reporte(EstadoCompra estado, LocalDate desde, LocalDate hasta) {
        List<Compra> compras = compraRepository.buscar(estado, desde, hasta);

        long totalCompras = compras.size();
        BigDecimal montoTotalComprado = BigDecimal.ZERO;
        BigDecimal saldoPendienteTotal = BigDecimal.ZERO;

        for (Compra c : compras) {
            if (c.getEstado() != EstadoCompra.ANULADA) {
                montoTotalComprado = montoTotalComprado.add(c.getTotal());
                saldoPendienteTotal = saldoPendienteTotal.add(c.getSaldoPendiente());
            }
        }

        BigDecimal montoTotalPagado = montoTotalComprado.subtract(saldoPendienteTotal);
        BigDecimal ticketPromedio = totalCompras > 0
                ? montoTotalComprado.divide(BigDecimal.valueOf(totalCompras), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<CompraResponse> listaResponses = compras.stream().map(this::toCompraResponse).toList();

        return CompraReporte.builder()
                .totalCompras(totalCompras)
                .montoTotalComprado(montoTotalComprado)
                .montoTotalPagado(montoTotalPagado)
                .saldoPendienteTotal(saldoPendienteTotal)
                .ticketPromedioCompra(ticketPromedio)
                .compras(listaResponses)
                .build();
    }

    private ProveedorResponse toProveedorResponse(Proveedor p) {
        return ProveedorResponse.builder()
                .id(p.getId())
                .ruc(p.getRuc())
                .razonSocial(p.getRazonSocial())
                .telefono(p.getTelefono())
                .email(p.getEmail())
                .direccion(p.getDireccion())
                .build();
    }

    private PagoCompraResponse toPagoResponse(PagoCompra p) {
        return PagoCompraResponse.builder()
                .id(p.getId())
                .compraId(p.getCompra() != null ? p.getCompra().getId() : null)
                .fechaPago(p.getFechaPago())
                .montoPagado(p.getMontoPagado())
                .metodoPago(p.getMetodoPago())
                .numeroOperacion(p.getNumeroOperacion())
                .estado(p.getEstado())
                .fechaCreacion(p.getFechaCreacion())
                .build();
    }

    private CompraResponse toCompraResponse(Compra c) {
        List<DetalleCompraResponse> detalles = c.getDetalles() != null
                ? c.getDetalles().stream().map(d -> DetalleCompraResponse.builder()
                .id(d.getId())
                .insumoId(d.getInsumoId())
                .nombreInsumo(d.getNombreInsumo())
                .loteProveedor(d.getLoteProveedor())
                .fechaVencimiento(d.getFechaVencimiento())
                .factorConversion(d.getFactorConversion())
                .cantidad(d.getCantidad())
                .cantidadIngreso(d.getCantidadIngreso())
                .precioUnitario(d.getPrecioUnitario())
                .subtotal(d.getSubtotal())
                .build()).toList()
                : List.of();

        List<PagoCompraResponse> pagos = c.getPagos() != null
                ? c.getPagos().stream().map(this::toPagoResponse).toList()
                : List.of();

        return CompraResponse.builder()
                .id(c.getId())
                .serieComprobante(c.getSerieComprobante())
                .numeroComprobante(c.getNumeroComprobante())
                .proveedor(toProveedorResponse(c.getProveedor()))
                .proveedorId(c.getProveedor() != null ? c.getProveedor().getId() : null)
                .fechaEmision(c.getFechaEmision())
                .condicionPago(c.getCondicionPago())
                .fechaVencimientoPago(c.getFechaVencimientoPago())
                .estado(c.getEstado())
                .subtotal(c.getSubtotal())
                .igv(c.getIgv())
                .costoFlete(c.getCostoFlete())
                .total(c.getTotal())
                .saldoPendiente(c.getSaldoPendiente())
                .detalles(detalles)
                .pagos(pagos)
                .build();
    }
}
