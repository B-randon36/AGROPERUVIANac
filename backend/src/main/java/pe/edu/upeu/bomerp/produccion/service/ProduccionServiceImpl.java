package pe.edu.upeu.bomerp.produccion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.bomerp.catalogo.producto.service.ProductoService;
import pe.edu.upeu.bomerp.exception.BusinessConflictException;
import pe.edu.upeu.bomerp.exception.ResourceNotFoundException;
import pe.edu.upeu.bomerp.inventario.service.InventarioService;
import pe.edu.upeu.bomerp.produccion.dto.*;
import pe.edu.upeu.bomerp.produccion.entity.DetalleConsumoInsumo;
import pe.edu.upeu.bomerp.produccion.entity.EstadoOrdenProduccion;
import pe.edu.upeu.bomerp.produccion.entity.LoteProducto;
import pe.edu.upeu.bomerp.produccion.entity.OrdenProduccion;
import pe.edu.upeu.bomerp.produccion.repository.LoteProductoRepository;
import pe.edu.upeu.bomerp.produccion.repository.OrdenProduccionRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProduccionServiceImpl implements ProduccionService {

    private final OrdenProduccionRepository ordenProduccionRepository;
    private final LoteProductoRepository loteProductoRepository;
    private final ProductoService productoService;
    private final InventarioService inventarioService;

    @Override
    @Transactional
    public OrdenProduccionResponse crearOrden(OrdenProduccionRequest request) {
        String codigo = "OP-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        OrdenProduccion orden = OrdenProduccion.builder()
                .codigoOrden(codigo)
                .productoId(request.getProductoId())
                .nombreProducto(request.getNombreProducto())
                .cantidadProgramada(request.getCantidadProgramada())
                .fechaInicio(LocalDateTime.now())
                .estado(EstadoOrdenProduccion.PLANIFICADA)
                .centroTrabajoId(request.getCentroTrabajoId())
                .centroTrabajoNombre(request.getCentroTrabajoNombre())
                .observaciones(request.getObservaciones())
                .insumosConsumidos(new ArrayList<>())
                .build();

        for (DetalleConsumoRequest item : request.getInsumos()) {
            DetalleConsumoInsumo detalle = DetalleConsumoInsumo.builder()
                    .ordenProduccion(orden)
                    .insumoId(item.getInsumoId())
                    .nombreInsumo(item.getNombreInsumo())
                    .cantidadRequerida(item.getCantidadRequerida())
                    .cantidadConsumida(item.getCantidadConsumida())
                    .unidadMedida(item.getUnidadMedida())
                    .build();
            orden.getInsumosConsumidos().add(detalle);
        }

        return toOrdenResponse(ordenProduccionRepository.save(orden));
    }

    @Override
    @Transactional
    public OrdenProduccionResponse iniciarOrden(Long id) {
        OrdenProduccion orden = ordenProduccionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de producción no encontrada con ID: " + id));

        if (orden.getEstado() != EstadoOrdenProduccion.PLANIFICADA) {
            throw new BusinessConflictException("Solo se pueden iniciar órdenes en estado PLANIFICADA.");
        }

        orden.setEstado(EstadoOrdenProduccion.EN_PROCESO);
        return toOrdenResponse(ordenProduccionRepository.save(orden));
    }

    @Override
    @Transactional
    public OrdenProduccionResponse completarOrden(Long id, CompletarOrdenRequest request) {
        OrdenProduccion orden = ordenProduccionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de producción no encontrada con ID: " + id));

        if (orden.getEstado() == EstadoOrdenProduccion.COMPLETADA) {
            throw new BusinessConflictException("La orden de producción ya ha sido completada.");
        }

        if (orden.getEstado() == EstadoOrdenProduccion.CANCELADA) {
            throw new BusinessConflictException("No se puede completar una orden cancelada.");
        }

        orden.setEstado(EstadoOrdenProduccion.COMPLETADA);
        orden.setCantidadProducida(request.getCantidadProducida());
        orden.setFechaFin(LocalDateTime.now());

        // 1. RN-PROD-01: Descontar insumos consumidos de almacén y asentar SALIDAS en Kardex
        if (inventarioService != null && orden.getInsumosConsumidos() != null) {
            for (DetalleConsumoInsumo insumo : orden.getInsumosConsumidos()) {
                inventarioService.registrarConsumoInsumoBom(
                        insumo.getInsumoId(),
                        insumo.getNombreInsumo(),
                        insumo.getCantidadConsumida() != null ? insumo.getCantidadConsumida() : insumo.getCantidadRequerida(),
                        insumo.getUnidadMedida(),
                        orden.getCodigoOrden()
                );
            }
        }

        // 2. Aumentar stock del producto terminado en el Catálogo Oficial
        productoService.aumentarStock(orden.getProductoId(), request.getCantidadProducida());

        // 3. Generar lote terminado con fecha de vencimiento (Criterio FEFO)
        String codigoLote = "LOT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + (int) (Math.random() * 900 + 100);

        LoteProducto lote = LoteProducto.builder()
                .codigoLote(codigoLote)
                .productoId(orden.getProductoId())
                .nombreProducto(orden.getNombreProducto())
                .stockDisponible(request.getCantidadProducida())
                .fechaProduccion(LocalDate.now())
                .fechaVencimiento(request.getFechaVencimiento())
                .build();

        loteProductoRepository.save(lote);

        // 4. Unificar con el módulo Inventario FEFO y asentar ENTRADA en Kardex Físico
        if (inventarioService != null) {
            inventarioService.registrarIngresoProduccion(
                    orden.getProductoId(),
                    orden.getNombreProducto(),
                    codigoLote,
                    request.getCantidadProducida(),
                    request.getFechaVencimiento(),
                    orden.getCodigoOrden()
            );
        }

        return toOrdenResponse(ordenProduccionRepository.save(orden));
    }

    @Override
    @Transactional
    public OrdenProduccionResponse cancelarOrden(Long id) {
        OrdenProduccion orden = ordenProduccionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de producción no encontrada con ID: " + id));

        if (orden.getEstado() == EstadoOrdenProduccion.COMPLETADA) {
            throw new BusinessConflictException("No se puede cancelar una orden de producción que ya fue completada.");
        }

        if (orden.getEstado() == EstadoOrdenProduccion.CANCELADA) {
            throw new BusinessConflictException("La orden de producción ya se encuentra cancelada.");
        }

        orden.setEstado(EstadoOrdenProduccion.CANCELADA);
        orden.setFechaFin(LocalDateTime.now());
        return toOrdenResponse(ordenProduccionRepository.save(orden));
    }

    @Override
    @Transactional(readOnly = true)
    public OrdenProduccionResponse obtener(Long id) {
        OrdenProduccion orden = ordenProduccionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de producción no encontrada con ID: " + id));
        return toOrdenResponse(orden);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenProduccionResponse> listarOrdenes() {
        return ordenProduccionRepository.findAll().stream()
                .map(this::toOrdenResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProduccionReporte reporte(EstadoOrdenProduccion estado, LocalDateTime desde, LocalDateTime hasta) {
        List<OrdenProduccion> ordenes = ordenProduccionRepository.buscar(estado, desde, hasta);

        long totalOrdenes = ordenes.size();
        long ordenesCompletadas = 0;
        long ordenesEnProceso = 0;
        long ordenesPlanificadas = 0;
        long totalProgramada = 0;
        long totalProducida = 0;

        for (OrdenProduccion o : ordenes) {
            if (o.getEstado() == EstadoOrdenProduccion.COMPLETADA) {
                ordenesCompletadas++;
            } else if (o.getEstado() == EstadoOrdenProduccion.EN_PROCESO) {
                ordenesEnProceso++;
            } else if (o.getEstado() == EstadoOrdenProduccion.PLANIFICADA) {
                ordenesPlanificadas++;
            }
            if (o.getCantidadProgramada() != null) {
                totalProgramada += o.getCantidadProgramada();
            }
            if (o.getCantidadProducida() != null) {
                totalProducida += o.getCantidadProducida();
            }
        }

        double eficiencia = totalProgramada > 0 ? (totalProducida * 100.0) / totalProgramada : 0.0;
        eficiencia = Math.round(eficiencia * 100.0) / 100.0;

        List<OrdenProduccionResponse> listaResponses = ordenes.stream().map(this::toOrdenResponse).toList();

        return ProduccionReporte.builder()
                .totalOrdenes(totalOrdenes)
                .ordenesCompletadas(ordenesCompletadas)
                .ordenesEnProceso(ordenesEnProceso)
                .ordenesPlanificadas(ordenesPlanificadas)
                .totalCantidadProgramada(totalProgramada)
                .totalCantidadProducida(totalProducida)
                .porcentajeEficiencia(eficiencia)
                .ordenes(listaResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoteProductoResponse> listarLotesPorProducto(Long productoId) {
        return loteProductoRepository.findByProductoId(productoId).stream()
                .map(this::toLoteResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoteProductoResponse> listarLotesDisponiblesFEFO(Long productoId) {
        return loteProductoRepository.findByProductoIdAndStockDisponibleGreaterThanOrderByFechaVencimientoAsc(productoId, 0).stream()
                .map(this::toLoteResponse)
                .toList();
    }

    @Override
    @Transactional
    public void descontarStockLoteFEFO(Long productoId, Integer cantidad) {
        List<LoteProducto> lotesDisponibles = loteProductoRepository
                .findByProductoIdAndStockDisponibleGreaterThanOrderByFechaVencimientoAsc(productoId, 0);

        int restante = cantidad;
        for (LoteProducto lote : lotesDisponibles) {
            if (restante <= 0) break;

            if (lote.getStockDisponible() <= restante) {
                restante -= lote.getStockDisponible();
                lote.setStockDisponible(0);
            } else {
                lote.setStockDisponible(lote.getStockDisponible() - restante);
                restante = 0;
            }
            loteProductoRepository.save(lote);
        }

        if (restante > 0) {
            throw new BusinessConflictException("No hay stock suficiente en los lotes vigentes para satisfacer la cantidad solicitada.");
        }
    }

    private OrdenProduccionResponse toOrdenResponse(OrdenProduccion o) {
        List<DetalleConsumoResponse> consumos = o.getInsumosConsumidos() != null
                ? o.getInsumosConsumidos().stream().map(d -> DetalleConsumoResponse.builder()
                .id(d.getId())
                .insumoId(d.getInsumoId())
                .nombreInsumo(d.getNombreInsumo())
                .cantidadRequerida(d.getCantidadRequerida())
                .cantidadConsumida(d.getCantidadConsumida())
                .unidadMedida(d.getUnidadMedida())
                .build()).toList()
                : List.of();

        return OrdenProduccionResponse.builder()
                .id(o.getId())
                .codigoOrden(o.getCodigoOrden())
                .productoId(o.getProductoId())
                .nombreProducto(o.getNombreProducto())
                .cantidadProgramada(o.getCantidadProgramada())
                .cantidadProducida(o.getCantidadProducida())
                .fechaInicio(o.getFechaInicio())
                .fechaFin(o.getFechaFin())
                .estado(o.getEstado())
                .centroTrabajoId(o.getCentroTrabajoId())
                .centroTrabajoNombre(o.getCentroTrabajoNombre())
                .observaciones(o.getObservaciones())
                .version(o.getVersion())
                .insumosConsumidos(consumos)
                .build();
    }

    private LoteProductoResponse toLoteResponse(LoteProducto l) {
        return LoteProductoResponse.builder()
                .id(l.getId())
                .codigoLote(l.getCodigoLote())
                .productoId(l.getProductoId())
                .nombreProducto(l.getNombreProducto())
                .stockDisponible(l.getStockDisponible())
                .fechaProduccion(l.getFechaProduccion())
                .fechaVencimiento(l.getFechaVencimiento())
                .build();
    }
}
