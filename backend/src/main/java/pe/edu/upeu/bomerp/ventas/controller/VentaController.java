package pe.edu.upeu.bomerp.ventas.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.bomerp.ventas.dto.ReporteOmnicanalResponse;
import pe.edu.upeu.bomerp.ventas.dto.VentaReporte;
import pe.edu.upeu.bomerp.ventas.dto.VentaRequest;
import pe.edu.upeu.bomerp.ventas.dto.VentaResponse;
import pe.edu.upeu.bomerp.ventas.entity.EstadoVenta;
import pe.edu.upeu.bomerp.ventas.service.VentaService;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Ventas - Punto de Venta POS", description = "Transacciones comerciales, emisión de tickets y salida por lote (Zaggy Morales)")
@RestController
@RequestMapping("/api/v1/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;
    private final pe.edu.upeu.bomerp.ventas.service.AuditoriaService auditoriaService;

    @Operation(summary = "Consulta ventas con filtros, ordenamiento y paginación")
    @GetMapping
    public ResponseEntity<Page<VentaResponse>> buscar(
            @RequestParam(required = false) EstadoVenta estado,
            @RequestParam(required = false) pe.edu.upeu.bomerp.ventas.entity.MetodoPagoVenta metodoPago,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @PageableDefault(size = 20, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        String vendedorId = esVendedorRestringido(authentication) ? jwt.getSubject() : null;
        return ResponseEntity.ok(ventaService.buscar(estado, metodoPago, desde, hasta, vendedorId, pageable));
    }

    @Operation(summary = "Consultar una venta por ID con validación de propiedad RN8 (VENDEDOR solo ve sus ventas)")
    @GetMapping("/{id}")
    public ResponseEntity<VentaResponse> obtener(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        String vendedorId = esVendedorRestringido(authentication) ? jwt.getSubject() : null;
        return ResponseEntity.ok(ventaService.obtener(id, vendedorId));
    }

    @Operation(summary = "Registrar una venta atómica asociando automáticamente el vendedorId extraído del JWT (RN5)")
    @PostMapping
    public ResponseEntity<VentaResponse> crear(
            @Valid @RequestBody VentaRequest request,
            @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        String vendedorId = (jwt != null) ? jwt.getSubject() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.crear(request, vendedorId));
    }

    @Operation(summary = "Anula una venta registrada, restaurando el stock de sus productos y auditando al actor")
    @RequestMapping(value = "/{id}/anular", method = { RequestMethod.PATCH, RequestMethod.PUT })
    public ResponseEntity<VentaResponse> anular(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        String actor = (jwt != null) ? jwt.getSubject() : "ANONYMOUS";
        return ResponseEntity.ok(ventaService.anular(id, actor));
    }

    @Operation(summary = "Vendedor solicita la anulación de una venta para que el Administrador/Supervisor la apruebe")
    @RequestMapping(value = "/{id}/solicitar-anulacion", method = { RequestMethod.PATCH, RequestMethod.POST, RequestMethod.PUT })
    public ResponseEntity<VentaResponse> solicitarAnulacion(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo,
            @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        String actor = (jwt != null) ? jwt.getSubject() : "VENDEDOR";
        return ResponseEntity.ok(ventaService.solicitarAnulacion(id, motivo, actor,
                esVendedorRestringido(authentication) ? jwt.getSubject() : null));
    }

    @Operation(summary = "Administrador o Supervisor rechaza la solicitud de anulación y mantiene la venta registrada")
    @RequestMapping(value = "/{id}/rechazar-anulacion", method = { RequestMethod.PATCH, RequestMethod.POST, RequestMethod.PUT })
    public ResponseEntity<VentaResponse> rechazarAnulacion(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        String actor = (jwt != null) ? jwt.getSubject() : "ADMIN";
        return ResponseEntity.ok(ventaService.rechazarAnulacion(id, actor));
    }

    @Operation(summary = "Consultar el historial de auditoría de ventas")
    @GetMapping("/auditoria")
    public ResponseEntity<?> listarAuditoria() {
        return ResponseEntity.ok(auditoriaService.listarTodasPorEntidad("VENTA"));
    }

    @Operation(summary = "Generar reporte agregado de ventas (total recaudado, ticket promedio y resumen)")
    @GetMapping("/resumen")
    public ResponseEntity<VentaReporte> reporte(
            @RequestParam(required = false) EstadoVenta estado,
            @RequestParam(required = false) pe.edu.upeu.bomerp.ventas.entity.MetodoPagoVenta metodoPago,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        String vendedorId = esVendedorRestringido(authentication) ? jwt.getSubject() : null;
        return ResponseEntity.ok(ventaService.reporte(estado, metodoPago, desde, hasta, vendedorId));
    }

    @Operation(summary = "Generar reporte omnicanal consolidado y ranking de rentabilidad de productos")
    @GetMapping("/reporte-omnicanal")
    public ResponseEntity<ReporteOmnicanalResponse> reporteOmnicanal() {
        return ResponseEntity.ok(ventaService.obtenerReporteOmnicanal());
    }

    private boolean esVendedorRestringido(Authentication authentication) {
        // Usa las mismas autoridades normalizadas que SecurityConfig, incluidos roles de cliente.
        return authentication.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_SUPERVISOR"));
    }
}
