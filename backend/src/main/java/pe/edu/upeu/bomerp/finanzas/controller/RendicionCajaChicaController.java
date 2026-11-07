package pe.edu.upeu.bomerp.finanzas.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.bomerp.finanzas.dto.CajaChicaResponse;
import pe.edu.upeu.bomerp.finanzas.dto.RendicionCajaChicaRequest;
import pe.edu.upeu.bomerp.finanzas.dto.RendicionCajaChicaResponse;
import pe.edu.upeu.bomerp.finanzas.dto.RendicionResumenDto;
import pe.edu.upeu.bomerp.finanzas.dto.ReporteRendicionesAgregadoDto;
import pe.edu.upeu.bomerp.finanzas.entity.EstadoRendicion;
import pe.edu.upeu.bomerp.finanzas.service.RendicionCajaChicaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Caja Chica y Rendiciones", description = "Gestión de cajas chicas operativas, asignación de centros de costo, rendiciones con comprobantes y agregaciones analíticas")
@CrossOrigin(originPatterns = "*")
@RestController
@RequestMapping({"/api/v1/finanzas", "/api/v1"})
public class RendicionCajaChicaController {

    private final RendicionCajaChicaService rendicionService;
    private final pe.edu.upeu.bomerp.finanzas.service.SesionCajaService sesionCajaService;

    public RendicionCajaChicaController(
            RendicionCajaChicaService rendicionService,
            pe.edu.upeu.bomerp.finanzas.service.SesionCajaService sesionCajaService) {
        this.rendicionService = rendicionService;
        this.sesionCajaService = sesionCajaService;
    }

    @Operation(summary = "Listar catálogo de billetes y monedas activas para arqueos")
    @GetMapping("/denominaciones")
    public ResponseEntity<List<pe.edu.upeu.bomerp.finanzas.dto.DenominacionMonedaResponse>> listarDenominaciones() {
        return ResponseEntity.ok(sesionCajaService.listarDenominaciones());
    }

    @Operation(summary = "Listar centros de costo disponibles para asignación presupuestaria")
    @GetMapping("/centros-costo")
    public ResponseEntity<List<pe.edu.upeu.bomerp.finanzas.dto.CentroCostoResponse>> listarCentrosCosto() {
        return ResponseEntity.ok(rendicionService.listarCentrosCosto());
    }

    @Operation(summary = "Crear nuevo centro de costo maestro")
    @PostMapping("/centros-costo")
    public ResponseEntity<pe.edu.upeu.bomerp.finanzas.dto.CentroCostoResponse> crearCentroCosto(
            @Valid @RequestBody pe.edu.upeu.bomerp.finanzas.dto.CentroCostoRequest request) {
        var response = rendicionService.crearCentroCosto(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Asignar presupuesto por centro de costo a una caja chica")
    @PostMapping("/cajas-chicas/{id}/presupuestos")
    public ResponseEntity<pe.edu.upeu.bomerp.finanzas.dto.AsignacionCajaCentroCostoResponse> asignarPresupuesto(
            @PathVariable Long id,
            @Valid @RequestBody pe.edu.upeu.bomerp.finanzas.dto.AsignarPresupuestoRequest request) {
        var response = rendicionService.asignarPresupuestoCentroCosto(id, request.getCentroCostoId(), request.getMontoPresupuesto());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Consultar presupuestos asignados por centro de costo a una caja chica")
    @GetMapping("/cajas-chicas/{id}/presupuestos")
    public ResponseEntity<List<pe.edu.upeu.bomerp.finanzas.dto.AsignacionCajaCentroCostoResponse>> listarPresupuestosCaja(@PathVariable Long id) {
        return ResponseEntity.ok(rendicionService.listarPresupuestosPorCaja(id));
    }

    @Operation(summary = "Listar todas las cajas chicas activas")
    @GetMapping("/cajas-chicas")
    public ResponseEntity<List<CajaChicaResponse>> listarCajasChicas() {
        return ResponseEntity.ok(rendicionService.listarCajasChicas());
    }

    @Operation(summary = "Crear nueva caja chica operativa")
    @PostMapping("/cajas-chicas")
    public ResponseEntity<CajaChicaResponse> crearCajaChica(
            @RequestParam String codigo,
            @RequestParam String responsable,
            @RequestParam BigDecimal limiteMaximo,
            @RequestParam BigDecimal saldoInicial) {
        CajaChicaResponse response = rendicionService.crearCajaChica(codigo, responsable, limiteMaximo, saldoInicial);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Consultar caja chica por su ID")
    @GetMapping("/cajas-chicas/{id}")
    public ResponseEntity<CajaChicaResponse> obtenerCajaChica(@PathVariable Long id) {
        return ResponseEntity.ok(rendicionService.obtenerCajaChica(id));
    }

    @Operation(summary = "Registrar nueva rendición de gastos con comprobantes de pago")
    @PostMapping("/rendiciones")
    public ResponseEntity<RendicionCajaChicaResponse> registrarRendicion(@Valid @RequestBody RendicionCajaChicaRequest request) {
        RendicionCajaChicaResponse response = rendicionService.registrarRendicion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Obtener detalle de rendición por ID")
    @GetMapping("/rendiciones/{id}")
    public ResponseEntity<RendicionCajaChicaResponse> obtenerRendicion(@PathVariable Long id) {
        return ResponseEntity.ok(rendicionService.obtenerRendicion(id));
    }

    /**
     * TRANSICIÓN DE ESTADO CON REGLA DE NEGOCIO REAL
     * Anula una rendición activa y reintegra el importe debitado a la Caja Chica.
     * Si ya se encuentra en estado ANULADO, el backend rechaza la transacción con HTTP 409 Conflict.
     */
    @Operation(summary = "Anular rendición activa y reintegrar saldo a la caja chica")
    @PostMapping("/rendiciones/{id}/anular")
    public ResponseEntity<RendicionCajaChicaResponse> anularRendicion(@PathVariable Long id) {
        return ResponseEntity.ok(rendicionService.anularRendicion(id));
    }

    @GetMapping("/rendiciones/caja/{cajaChicaId}")
    public ResponseEntity<List<RendicionCajaChicaResponse>> listarPorCaja(@PathVariable Long cajaChicaId) {
        return ResponseEntity.ok(rendicionService.listarRendicionesPorCaja(cajaChicaId));
    }

    @GetMapping("/rendiciones/paginado")
    public ResponseEntity<Page<RendicionCajaChicaResponse>> listarRendicionesPaginado(
            @PageableDefault(page = 0, size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(rendicionService.listarPaginado(pageable));
    }

    /**
     * 1. ENDPOINT DE FILTROS COMBINADOS Y ORDENAMIENTO
     * Permite múltiples combinaciones de parámetros de búsqueda y ordenamiento dinámico.
     * Soporta tanto ?ordenarPor=montoTotal&direccion=ASC como ?sort=montoTotal,asc.
     * Rutas compatibles: GET /api/v1/finanzas/rendiciones o GET /api/v1/finanzas/rendiciones/filtros
     */
    @GetMapping({"/rendiciones", "/rendiciones/filtros"})
    public ResponseEntity<Page<RendicionCajaChicaResponse>> buscarRendicionesConFiltros(
            @RequestParam(required = false) Long cajaChicaId,
            @RequestParam(required = false) EstadoRendicion estado,
            @RequestParam(required = false) String responsable,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) BigDecimal montoMin,
            @RequestParam(required = false) BigDecimal montoMax,
            @RequestParam(required = false) String ordenarPor,
            @RequestParam(required = false) String direccion,
            @PageableDefault(page = 0, size = 10, sort = "id") Pageable pageable) {

        Pageable effectivePageable = pageable;
        if (ordenarPor != null && !ordenarPor.isBlank()) {
            Sort.Direction dir = "DESC".equalsIgnoreCase(direccion) ? Sort.Direction.DESC : Sort.Direction.ASC;
            effectivePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(dir, ordenarPor));
        }

        return ResponseEntity.ok(rendicionService.buscarConFiltros(
                cajaChicaId, estado, responsable, fechaInicio, fechaFin, montoMin, montoMax, effectivePageable
        ));
    }

    /**
     * 2. ENDPOINT DE PROYECCIÓN DE RESUMEN
     * Devuelve DTOs planos optimizados construidos mediante JPQL constructor expression.
     * Ejemplo: GET /api/v1/finanzas/rendiciones/resumen?cajaChicaId=1
     */
    @GetMapping("/rendiciones/resumen")
    public ResponseEntity<List<RendicionResumenDto>> obtenerResumen(
            @RequestParam(required = false) Long cajaChicaId) {
        return ResponseEntity.ok(rendicionService.obtenerResumen(cajaChicaId));
    }

    /**
     * 3. ENDPOINT DE REPORTE AGREGADO
     * Devuelve agregaciones matemáticas (SUM, AVG, MAX, MIN, COUNT).
     * Garantiza valores numéricos en 0 / 0.00 en vez de null si no hay registros.
     * Ejemplo: GET /api/v1/finanzas/rendiciones/reporte-agregado?cajaChicaId=1
     */
    @GetMapping("/rendiciones/reporte-agregado")
    public ResponseEntity<ReporteRendicionesAgregadoDto> obtenerReporteAgregado(
            @RequestParam(required = false) Long cajaChicaId) {
        return ResponseEntity.ok(rendicionService.obtenerReporteAgregado(cajaChicaId));
    }
}

