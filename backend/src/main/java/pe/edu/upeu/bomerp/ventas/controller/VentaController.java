package pe.edu.upeu.bomerp.ventas.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.bomerp.ventas.dto.ReporteOmnicanalResponse;
import pe.edu.upeu.bomerp.ventas.dto.VentaReporte;
import pe.edu.upeu.bomerp.ventas.dto.VentaRequest;
import pe.edu.upeu.bomerp.ventas.dto.VentaResponse;
import pe.edu.upeu.bomerp.ventas.entity.EstadoVenta;
import pe.edu.upeu.bomerp.ventas.service.VentaService;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "Ventas - Punto de Venta POS", description = "Transacciones comerciales, emisión de tickets y salida por lote (Zaggy Morales)")
@RestController
@RequestMapping("/api/v1/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;

    @Operation(summary = "Consultar ventas con filtros opcionales (estado, rango de fechas) y aislamiento RN8 por vendedor")
    @GetMapping
    public ResponseEntity<List<VentaResponse>> buscar(
            @RequestParam(required = false) EstadoVenta estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(defaultValue = "fecha") String ordenarPor,
            @RequestParam(defaultValue = "DESC") String direccion,
            @AuthenticationPrincipal Jwt jwt) {
        String vendedorId = esVendedorRestringido(jwt) ? jwt.getSubject() : null;
        return ResponseEntity.ok(ventaService.buscar(estado, desde, hasta, ordenarPor, direccion, vendedorId));
    }

    @Operation(summary = "Consultar una venta por ID con validación de propiedad RN8 (VENDEDOR solo ve sus ventas)")
    @GetMapping("/{id}")
    public ResponseEntity<VentaResponse> obtener(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        String vendedorId = esVendedorRestringido(jwt) ? jwt.getSubject() : null;
        return ResponseEntity.ok(ventaService.obtener(id, vendedorId));
    }

    @Operation(summary = "Registrar una venta atómica asociando automáticamente el vendedorId extraído del JWT (RN5)")
    @PostMapping
    public ResponseEntity<VentaResponse> crear(
            @Valid @RequestBody VentaRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String vendedorId = (jwt != null) ? jwt.getSubject() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.crear(request, vendedorId));
    }

    @Operation(summary = "Anula una venta registrada, restaurando el stock de sus productos")
    @RequestMapping(value = "/{id}/anular", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<VentaResponse> anular(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.anular(id));
    }

    @Operation(summary = "Generar reporte agregado de ventas (total recaudado, ticket promedio y resumen)")
    @GetMapping("/resumen")
    public ResponseEntity<VentaReporte> reporte(
            @RequestParam(required = false) EstadoVenta estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return ResponseEntity.ok(ventaService.reporte(estado, desde, hasta));
    }

    @Operation(summary = "Generar reporte omnicanal consolidado y ranking de rentabilidad de productos")
    @GetMapping("/reporte-omnicanal")
    public ResponseEntity<ReporteOmnicanalResponse> reporteOmnicanal() {
        return ResponseEntity.ok(ventaService.obtenerReporteOmnicanal());
    }

    @Operation(summary = "Eliminar todas las ventas del reporte transaccional")
    @DeleteMapping
    public ResponseEntity<Void> eliminarTodas() {
        ventaService.eliminarTodas();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar una venta individual por ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        ventaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private boolean esVendedorRestringido(Jwt jwt) {
        if (jwt == null) return false;
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        Object roles = realmAccess == null ? null : realmAccess.get("roles");
        if (!(roles instanceof Collection<?> lista)) {
            return false;
        }
        Set<String> normalizados = lista.stream()
                .filter(Objects::nonNull)
                .map(r -> r.toString().trim().toUpperCase())
                .collect(Collectors.toSet());

        if (normalizados.contains("ADMIN") || normalizados.contains("ROLE_ADMIN")
                || normalizados.contains("SUPERVISOR") || normalizados.contains("ROLE_SUPERVISOR")) {
            return false;
        }
        return normalizados.contains("VENDEDOR") || normalizados.contains("ROLE_VENDEDOR");
    }
}
