package pe.edu.upeu.bomerp.compras.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.bomerp.compras.dto.*;
import pe.edu.upeu.bomerp.compras.entity.Compra;
import pe.edu.upeu.bomerp.compras.entity.DetalleCompra;
import pe.edu.upeu.bomerp.compras.entity.EstadoCompra;
import pe.edu.upeu.bomerp.compras.entity.Proveedor;
import pe.edu.upeu.bomerp.compras.repository.CompraRepository;
import pe.edu.upeu.bomerp.compras.repository.ProveedorRepository;
import pe.edu.upeu.bomerp.exception.BusinessConflictException;
import pe.edu.upeu.bomerp.exception.ResourceNotFoundException;
import pe.edu.upeu.bomerp.finanzas.api.FinanzasApi;
import pe.edu.upeu.bomerp.finanzas.entity.MetodoPago;
import pe.edu.upeu.bomerp.inventario.service.InventarioService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompraServiceImpl implements CompraService {

    private final CompraRepository compraRepository;
    private final ProveedorRepository proveedorRepository;
    private final InventarioService inventarioService;
    private final FinanzasApi finanzasApi;

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

        Compra compra = Compra.builder()
                .numeroComprobante(request.getNumeroComprobante())
                .proveedor(proveedor)
                .fechaEmision(request.getFechaEmision())
                .estado(EstadoCompra.REGISTRADA)
                .detalles(new ArrayList<>())
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (DetalleCompraRequest item : request.getDetalles()) {
            BigDecimal subtotalItem = item.getPrecioUnitario().multiply(item.getCantidad()).setScale(2, RoundingMode.HALF_UP);

            DetalleCompra detalle = DetalleCompra.builder()
                    .compra(compra)
                    .insumoId(item.getInsumoId())
                    .nombreInsumo(item.getNombreInsumo())
                    .cantidad(item.getCantidad())
                    .precioUnitario(item.getPrecioUnitario())
                    .subtotal(subtotalItem)
                    .build();

            compra.getDetalles().add(detalle);
            total = total.add(subtotalItem);

            // RN-COM-01: Ingreso automático del lote de insumo a Almacén y Kardex Físico
            if (inventarioService != null) {
                inventarioService.registrarIngresoCompraInsumo(
                        item.getInsumoId(),
                        item.getNombreInsumo(),
                        item.getCantidad(),
                        item.getPrecioUnitario(),
                        request.getNumeroComprobante()
                );
            }
        }

        BigDecimal igv = total.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = total.subtract(igv);

        compra.setSubtotal(subtotal);
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

        // 2. Actualizar saldo y estado de la compra únicamente tras confirmar solvencia financiera
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

    private CompraResponse toCompraResponse(Compra c) {
        List<DetalleCompraResponse> detalles = c.getDetalles() != null
                ? c.getDetalles().stream().map(d -> DetalleCompraResponse.builder()
                .id(d.getId())
                .insumoId(d.getInsumoId())
                .nombreInsumo(d.getNombreInsumo())
                .cantidad(d.getCantidad())
                .precioUnitario(d.getPrecioUnitario())
                .subtotal(d.getSubtotal())
                .build()).toList()
                : List.of();

        return CompraResponse.builder()
                .id(c.getId())
                .numeroComprobante(c.getNumeroComprobante())
                .proveedor(toProveedorResponse(c.getProveedor()))
                .fechaEmision(c.getFechaEmision())
                .estado(c.getEstado())
                .subtotal(c.getSubtotal())
                .igv(c.getIgv())
                .total(c.getTotal())
                .saldoPendiente(c.getSaldoPendiente())
                .detalles(detalles)
                .build();
    }
}
