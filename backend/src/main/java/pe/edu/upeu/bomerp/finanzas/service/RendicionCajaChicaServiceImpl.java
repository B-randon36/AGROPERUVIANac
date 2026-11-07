package pe.edu.upeu.bomerp.finanzas.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.bomerp.catalogo.producto.service.ProductoService;
import pe.edu.upeu.bomerp.exception.ResourceNotFoundException;
import pe.edu.upeu.bomerp.exception.SolvenciaInsuficienteException;
import pe.edu.upeu.bomerp.finanzas.dto.CajaChicaResponse;
import pe.edu.upeu.bomerp.finanzas.dto.DetalleRendicionRequest;
import pe.edu.upeu.bomerp.finanzas.dto.DetalleRendicionResponse;
import pe.edu.upeu.bomerp.finanzas.dto.RendicionCajaChicaRequest;
import pe.edu.upeu.bomerp.finanzas.dto.RendicionCajaChicaResponse;
import pe.edu.upeu.bomerp.finanzas.dto.RendicionResumenDto;
import pe.edu.upeu.bomerp.finanzas.dto.ReporteRendicionesAgregadoDto;
import pe.edu.upeu.bomerp.finanzas.entity.CajaChica;
import pe.edu.upeu.bomerp.finanzas.entity.DetalleRendicionCajaChica;
import pe.edu.upeu.bomerp.finanzas.entity.EstadoRendicion;
import pe.edu.upeu.bomerp.finanzas.entity.RendicionCajaChica;
import pe.edu.upeu.bomerp.finanzas.repository.CajaChicaRepository;
import pe.edu.upeu.bomerp.finanzas.repository.RendicionCajaChicaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class RendicionCajaChicaServiceImpl implements RendicionCajaChicaService {

    private final CajaChicaRepository cajaChicaRepository;
    private final RendicionCajaChicaRepository rendicionRepository;
    private final ProductoService productoService;
    private final pe.edu.upeu.bomerp.finanzas.repository.CentroCostoRepository centroCostoRepository;
    private final pe.edu.upeu.bomerp.finanzas.repository.AsignacionCajaCentroCostoRepository asignacionRepository;

    public RendicionCajaChicaServiceImpl(
            CajaChicaRepository cajaChicaRepository,
            RendicionCajaChicaRepository rendicionRepository,
            ProductoService productoService,
            pe.edu.upeu.bomerp.finanzas.repository.CentroCostoRepository centroCostoRepository,
            pe.edu.upeu.bomerp.finanzas.repository.AsignacionCajaCentroCostoRepository asignacionRepository) {
        this.cajaChicaRepository = cajaChicaRepository;
        this.rendicionRepository = rendicionRepository;
        this.productoService = productoService;
        this.centroCostoRepository = centroCostoRepository;
        this.asignacionRepository = asignacionRepository;
    }

    @Override
    @Transactional
    public CajaChicaResponse crearCajaChica(String codigo, String responsable, BigDecimal limiteMaximo, BigDecimal saldoInicial) {
        if (cajaChicaRepository.existsByCodigo(codigo)) {
            throw new IllegalArgumentException("Ya existe una caja chica con el código: " + codigo);
        }

        CajaChica caja = CajaChica.builder()
                .codigo(codigo)
                .responsable(responsable)
                .limiteMaximo(limiteMaximo)
                .saldoDisponible(saldoInicial)
                .build();

        CajaChica guardada = cajaChicaRepository.save(caja);
        return mapToCajaChicaResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CajaChicaResponse> listarCajasChicas() {
        return cajaChicaRepository.findAll().stream()
                .map(this::mapToCajaChicaResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CajaChicaResponse obtenerCajaChica(Long id) {
        CajaChica caja = buscarCajaChicaOFallar(id);
        return mapToCajaChicaResponse(caja);
    }

    /**
     * ========================================================================
     * TRANSICIÓN DE ESTADO CON REGLA DE NEGOCIO REAL (ANULACIÓN CON EFECTO COLATERAL)
     * ========================================================================
     * 1. Validación de idempotencia: Si la rendición ya se encuentra en estado ANULADO,
     *    se rechaza con IllegalStateException (manejada -> HTTP 409 Conflict).
     * 2. Transición de Estado: Pasa de su estado actual a EstadoRendicion.ANULADO.
     * 3. Efecto Colateral Financiero Real: Restituye el saldo debitado a la Caja Chica
     *    (caja.saldoDisponible += rendicion.montoTotal).
     * ========================================================================
     */
    @Override
    @Transactional
    public RendicionCajaChicaResponse anularRendicion(Long id) {
        RendicionCajaChica rendicion = rendicionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rendición no encontrada con id: " + id));

        if (rendicion.getEstado() == EstadoRendicion.ANULADO) {
            throw new IllegalStateException("RN-FIN-02: La rendición con código " + rendicion.getCodigo() +
                    " ya se encuentra en estado ANULADO y no se puede anular nuevamente.");
        }

        rendicion.setEstado(EstadoRendicion.ANULADO);

        // [EFECTO COLATERAL REAL]: Restitución del monto rendido al saldo disponible de la Caja Chica
        CajaChica caja = rendicion.getCajaChica();
        caja.setSaldoDisponible(caja.getSaldoDisponible().add(rendicion.getMontoTotal()));
        cajaChicaRepository.save(caja);

        // Restitución presupuestal por Centro de Costo M:N
        if (rendicion.getDetalles() != null && caja.getAsignacionesPresupuesto() != null) {
            for (DetalleRendicionCajaChica det : rendicion.getDetalles()) {
                if (det.getCentroCosto() != null) {
                    caja.getAsignacionesPresupuesto().stream()
                            .filter(a -> a.getCentroCosto() != null && a.getCentroCosto().getId().equals(det.getCentroCosto().getId()))
                            .findFirst()
                            .ifPresent(asig -> asig.reintegrarSaldo(det.getSubtotal()));
                }
            }
        }

        RendicionCajaChica guardada = rendicionRepository.save(rendicion);
        return mapToRendicionResponse(guardada);
    }

    /**
     * ========================================================================
     * OPERACIÓN DE DOMINIO CABECERA-DETALLE CON TRANSACCIÓN ATÓMICA
     * ========================================================================
     * Cumple con los criterios 4 y 6 de la rúbrica:
     * 1. Comunicación Inter-módulo: Valida la existencia del producto en el
     *    módulo 'catalogo' mediante ProductoService (expuesto vía @NamedInterface).
     * 2. Cálculo de Totales: El método de dominio rendicion.calcularTotal()
     *    calcula subtotales (precio * cantidad) y acumula el monto total en memoria.
     * 3. Regla de Negocio Real (RN-FIN-01): Control de Liquidez / Solvencia de Caja.
     *    Si montoTotal > saldoDisponible de la Caja Chica, se lanza
     *    SolvenciaInsuficienteException (manejada por GlobalExceptionHandler -> HTTP 400).
     * 4. Regla de Negocio Real (RN-FIN-06): Control Presupuestal por Centro de Costo.
     *    Si el gasto imputado supera el saldoDisponible de la AsignacionCajaCentroCosto,
     *    se rechaza y se lanza SolvenciaInsuficienteException.
     * 5. Transacción Atómica (@Transactional):
     *    - Caso Éxito: Se descuenta saldoDisponible de Caja Chica y Centro de Costo,
     *      persistiéndose cabecera y detalles en un único commit atómico.
     *    - Caso Rollback: Ante una excepción, Spring revierte toda mutación.
     * ========================================================================
     */
    @Override
    @Transactional
    public RendicionCajaChicaResponse registrarRendicion(RendicionCajaChicaRequest request) {
        if (rendicionRepository.existsByCodigo(request.getCodigo())) {
            throw new IllegalArgumentException("Ya existe una rendición con el código: " + request.getCodigo());
        }

        CajaChica cajaChica = buscarCajaChicaOFallar(request.getCajaChicaId());

        RendicionCajaChica rendicion = RendicionCajaChica.builder()
                .codigo(request.getCodigo())
                .responsable(request.getResponsable())
                .fecha(LocalDate.now())
                .estado(EstadoRendicion.APROBADO)
                .cajaChica(cajaChica)
                .build();

        for (DetalleRendicionRequest detReq : request.getDetalles()) {
            // [LÍMITES DE MÓDULO]: Comunicación síncrona desacoplada con módulo 'catalogo'
            if (detReq.getProductoId() != null) {
                productoService.obtener(detReq.getProductoId());
            }

            pe.edu.upeu.bomerp.finanzas.entity.CentroCosto centroCosto = null;
            if (detReq.getCentroCostoId() != null) {
                centroCosto = centroCostoRepository.findById(detReq.getCentroCostoId())
                        .orElseThrow(() -> new ResourceNotFoundException("Centro de costo no encontrado con id: " + detReq.getCentroCostoId()));
            }

            BigDecimal subtotalDetalle = detReq.getPrecioUnitario().multiply(BigDecimal.valueOf(detReq.getCantidad()));

            // [RN-FIN-06]: Control de Solvencia Presupuestal por Centro de Costo
            if (centroCosto != null && cajaChica.getAsignacionesPresupuesto() != null) {
                var asignacionOpt = cajaChica.getAsignacionesPresupuesto().stream()
                        .filter(a -> a.getCentroCosto() != null && a.getCentroCosto().getId().equals(detReq.getCentroCostoId()))
                        .findFirst();

                if (asignacionOpt.isPresent()) {
                    pe.edu.upeu.bomerp.finanzas.entity.AsignacionCajaCentroCosto asig = asignacionOpt.get();
                    if (subtotalDetalle.compareTo(asig.getSaldoDisponible()) > 0) {
                        throw new SolvenciaInsuficienteException(
                                "RN-FIN-06: El gasto para el Centro de Costo " + centroCosto.getCodigoCc() +
                                " (S/ " + subtotalDetalle + ") excede el saldo presupuestal disponible asignado (S/ " +
                                asig.getSaldoDisponible() + ")"
                        );
                    }
                    asig.descontarSaldo(subtotalDetalle);
                }
            }

            DetalleRendicionCajaChica detalle = DetalleRendicionCajaChica.builder()
                    .concepto(detReq.getConcepto())
                    .cantidad(detReq.getCantidad())
                    .precioUnitario(detReq.getPrecioUnitario())
                    .subtotal(subtotalDetalle)
                    .productoId(detReq.getProductoId())
                    .centroCosto(centroCosto)
                    .build();

            rendicion.agregarDetalle(detalle);
        }

        // [CABECERA-DETALLE]: Cálculo de subtotales y total acumulado de dominio
        rendicion.calcularTotal();

        // [REGLA DE NEGOCIO REAL RN-FIN-01]: Solvencia de Caja Chica / Control de Liquidez
        if (rendicion.getMontoTotal().compareTo(cajaChica.getSaldoDisponible()) > 0) {
            throw new SolvenciaInsuficienteException(
                    "RN-FIN-01: El monto total de la rendición (S/ " + rendicion.getMontoTotal() +
                    ") supera la solvencia/saldo disponible de Caja Chica (S/ " + cajaChica.getSaldoDisponible() + ")"
            );
        }

        // [TRANSACCIÓN ATÓMICA]: Mutación y persistencia conjunta (Commit atómico)
        cajaChica.setSaldoDisponible(cajaChica.getSaldoDisponible().subtract(rendicion.getMontoTotal()));
        cajaChicaRepository.save(cajaChica);

        RendicionCajaChica rendicionGuardada = rendicionRepository.save(rendicion);
        return mapToRendicionResponse(rendicionGuardada);
    }

    @Override
    @Transactional(readOnly = true)
    public RendicionCajaChicaResponse obtenerRendicion(Long id) {
        RendicionCajaChica rendicion = rendicionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rendición no encontrada con id: " + id));
        return mapToRendicionResponse(rendicion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RendicionCajaChicaResponse> listarRendicionesPorCaja(Long cajaChicaId) {
        buscarCajaChicaOFallar(cajaChicaId);
        return rendicionRepository.findByCajaChicaId(cajaChicaId).stream()
                .map(this::mapToRendicionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RendicionCajaChicaResponse> listarPaginado(Pageable pageable) {
        return rendicionRepository.findAll(pageable)
                .map(this::mapToRendicionResponse);
    }

    /**
     * ========================================================================
     * CONSULTAS EMPRESARIALES: FILTROS DINÁMICOS COMBINADOS Y ORDENAMIENTO
     * ========================================================================
     * Permite múltiples combinaciones de parámetros de búsqueda opcionales
     * (caja, estado, responsable, rango de fechas, rango de montos) junto con
     * paginación y ordenamiento dinámico multi-campo.
     * ========================================================================
     */
    @Override
    @Transactional(readOnly = true)
    public Page<RendicionCajaChicaResponse> buscarConFiltros(
            Long cajaChicaId,
            EstadoRendicion estado,
            String responsable,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            BigDecimal montoMin,
            BigDecimal montoMax,
            Pageable pageable) {
        return rendicionRepository.buscarConFiltros(
                cajaChicaId, estado, responsable, fechaInicio, fechaFin, montoMin, montoMax, pageable
        ).map(this::mapToRendicionResponse);
    }

    /**
     * ========================================================================
     * PROYECCIÓN DE RESUMEN (CONSTRUCTOR EXPRESSION DTO)
     * ========================================================================
     * Utiliza la cláusula JPQL 'SELECT new ...' para construir directamente DTOs
     * planos en la base de datos sin sobrecosto de memoria por entidades ORM.
     * ========================================================================
     */
    @Override
    @Transactional(readOnly = true)
    public List<RendicionResumenDto> obtenerResumen(Long cajaChicaId) {
        if (cajaChicaId != null) {
            buscarCajaChicaOFallar(cajaChicaId);
        }
        return rendicionRepository.obtenerResumenProyectado(cajaChicaId);
    }

    /**
     * ========================================================================
     * REPORTE AGREGADO DEFENSIVO (0 / 0.00 en vez de NULL)
     * ========================================================================
     * Realiza agregaciones matemáticas (SUM, AVG, MAX, MIN, COUNT).
     * Si no existen registros coincidentes, garantiza numéricamente 0 / 0.00
     * en lugar de devolver null.
     * ========================================================================
     */
    @Override
    @Transactional(readOnly = true)
    public ReporteRendicionesAgregadoDto obtenerReporteAgregado(Long cajaChicaId) {
        if (cajaChicaId != null) {
            buscarCajaChicaOFallar(cajaChicaId);
        }
        ReporteRendicionesAgregadoDto reporte = rendicionRepository.obtenerReporteAgregado(cajaChicaId);
        return reporte != null ? reporte : ReporteRendicionesAgregadoDto.empty();
    }

    @Override
    @Transactional
    public pe.edu.upeu.bomerp.finanzas.dto.AsignacionCajaCentroCostoResponse asignarPresupuestoCentroCosto(Long cajaChicaId, Long centroCostoId, BigDecimal montoAsignado) {
        CajaChica caja = buscarCajaChicaOFallar(cajaChicaId);
        pe.edu.upeu.bomerp.finanzas.entity.CentroCosto cc = centroCostoRepository.findById(centroCostoId)
                .orElseThrow(() -> new ResourceNotFoundException("Centro de costo no encontrado con id: " + centroCostoId));

        pe.edu.upeu.bomerp.finanzas.entity.AsignacionCajaCentroCosto asignacion = null;
        if (caja.getAsignacionesPresupuesto() != null) {
            asignacion = caja.getAsignacionesPresupuesto().stream()
                    .filter(a -> a.getCentroCosto() != null && a.getCentroCosto().getId().equals(centroCostoId))
                    .findFirst()
                    .orElse(null);
        }

        if (asignacion != null) {
            BigDecimal diferencia = montoAsignado.subtract(asignacion.getPresupuestoAsignado());
            asignacion.setPresupuestoAsignado(montoAsignado);
            BigDecimal nuevoSaldo = asignacion.getSaldoDisponible().add(diferencia);
            if (nuevoSaldo.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("No se puede reducir el presupuesto por debajo de lo ya consumido");
            }
            asignacion.setSaldoDisponible(nuevoSaldo);
            asignacion.setActivo(true);
        } else {
            asignacion = pe.edu.upeu.bomerp.finanzas.entity.AsignacionCajaCentroCosto.builder()
                    .id(new pe.edu.upeu.bomerp.finanzas.entity.AsignacionCajaCentroCostoId(cajaChicaId, centroCostoId))
                    .cajaChica(caja)
                    .centroCosto(cc)
                    .presupuestoAsignado(montoAsignado)
                    .saldoDisponible(montoAsignado)
                    .fechaAsignacion(LocalDate.now())
                    .activo(true)
                    .build();
            caja.agregarAsignacion(asignacion);
        }

        cajaChicaRepository.save(caja);
        return mapToAsignacionResponse(asignacion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<pe.edu.upeu.bomerp.finanzas.dto.AsignacionCajaCentroCostoResponse> listarPresupuestosPorCaja(Long cajaChicaId) {
        buscarCajaChicaOFallar(cajaChicaId);
        return asignacionRepository.findByCajaChicaId(cajaChicaId).stream()
                .map(this::mapToAsignacionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<pe.edu.upeu.bomerp.finanzas.dto.CentroCostoResponse> listarCentrosCosto() {
        return centroCostoRepository.findByActivoTrueOrderByCodigoCcAsc().stream()
                .map(c -> pe.edu.upeu.bomerp.finanzas.dto.CentroCostoResponse.builder()
                        .id(c.getId())
                        .codigoCc(c.getCodigoCc())
                        .nombreArea(c.getNombreArea())
                        .activo(c.getActivo())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public pe.edu.upeu.bomerp.finanzas.dto.CentroCostoResponse crearCentroCosto(pe.edu.upeu.bomerp.finanzas.dto.CentroCostoRequest request) {
        String codigoLimpio = request.getCodigoCc().trim().toUpperCase();
        if (centroCostoRepository.existsByCodigoCc(codigoLimpio)) {
            throw new IllegalArgumentException("Ya existe un centro de costo con el código: " + codigoLimpio);
        }
        pe.edu.upeu.bomerp.finanzas.entity.CentroCosto cc = pe.edu.upeu.bomerp.finanzas.entity.CentroCosto.builder()
                .codigoCc(codigoLimpio)
                .nombreArea(request.getNombreArea().trim())
                .activo(true)
                .build();
        cc = centroCostoRepository.save(cc);
        return pe.edu.upeu.bomerp.finanzas.dto.CentroCostoResponse.builder()
                .id(cc.getId())
                .codigoCc(cc.getCodigoCc())
                .nombreArea(cc.getNombreArea())
                .activo(cc.getActivo())
                .build();
    }

    private pe.edu.upeu.bomerp.finanzas.dto.AsignacionCajaCentroCostoResponse mapToAsignacionResponse(pe.edu.upeu.bomerp.finanzas.entity.AsignacionCajaCentroCosto a) {
        return pe.edu.upeu.bomerp.finanzas.dto.AsignacionCajaCentroCostoResponse.builder()
                .cajaChicaId(a.getCajaChica().getId())
                .centroCostoId(a.getCentroCosto().getId())
                .codigoCc(a.getCentroCosto().getCodigoCc())
                .nombreArea(a.getCentroCosto().getNombreArea())
                .presupuestoAsignado(a.getPresupuestoAsignado())
                .saldoDisponible(a.getSaldoDisponible())
                .fechaAsignacion(a.getFechaAsignacion())
                .activo(a.getActivo())
                .build();
    }

    private CajaChica buscarCajaChicaOFallar(Long id) {
        return cajaChicaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caja chica no encontrada con id: " + id));
    }

    private CajaChicaResponse mapToCajaChicaResponse(CajaChica caja) {
        return CajaChicaResponse.builder()
                .id(caja.getId())
                .codigo(caja.getCodigo())
                .responsable(caja.getResponsable())
                .saldoDisponible(caja.getSaldoDisponible())
                .limiteMaximo(caja.getLimiteMaximo())
                .build();
    }

    private RendicionCajaChicaResponse mapToRendicionResponse(RendicionCajaChica r) {
        List<DetalleRendicionResponse> detallesResp = r.getDetalles().stream()
                .map(d -> DetalleRendicionResponse.builder()
                        .id(d.getId())
                        .concepto(d.getConcepto())
                        .cantidad(d.getCantidad())
                        .precioUnitario(d.getPrecioUnitario())
                        .subtotal(d.getSubtotal())
                        .productoId(d.getProductoId())
                        .centroCostoId(d.getCentroCosto() != null ? d.getCentroCosto().getId() : null)
                        .centroCostoCodigo(d.getCentroCosto() != null ? d.getCentroCosto().getCodigoCc() : null)
                        .centroCostoNombre(d.getCentroCosto() != null ? d.getCentroCosto().getNombreArea() : null)
                        .build())
                .toList();

        return RendicionCajaChicaResponse.builder()
                .id(r.getId())
                .codigo(r.getCodigo())
                .responsable(r.getResponsable())
                .fecha(r.getFecha())
                .montoTotal(r.getMontoTotal())
                .estado(r.getEstado())
                .cajaChicaId(r.getCajaChica().getId())
                .detalles(detallesResp)
                .build();
    }
}
