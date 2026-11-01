package pe.edu.upeu.bomerp.produccion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.bomerp.produccion.dto.CentroTrabajoRequest;
import pe.edu.upeu.bomerp.produccion.dto.CentroTrabajoResponse;
import pe.edu.upeu.bomerp.produccion.service.CentroTrabajoService;

import java.util.List;

@Tag(name = "Producción - Centros de Trabajo", description = "Gestión de líneas operativas de planta, capacidades nominales y costos por hora")
@RestController
@RequestMapping("/api/v1/produccion/centros-trabajo")
@RequiredArgsConstructor
public class CentroTrabajoController {

    private final CentroTrabajoService centroTrabajoService;

    @Operation(summary = "Listar todos los centros de trabajo de planta")
    @GetMapping
    public ResponseEntity<List<CentroTrabajoResponse>> listar() {
        return ResponseEntity.ok(centroTrabajoService.listar());
    }

    @Operation(summary = "Obtener centro de trabajo por ID")
    @GetMapping("/{id}")
    public ResponseEntity<CentroTrabajoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(centroTrabajoService.obtener(id));
    }

    @Operation(summary = "Registrar nuevo centro de trabajo")
    @PostMapping
    public ResponseEntity<CentroTrabajoResponse> crear(@Valid @RequestBody CentroTrabajoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(centroTrabajoService.crear(request));
    }

    @Operation(summary = "Actualizar centro de trabajo existente")
    @PutMapping("/{id}")
    public ResponseEntity<CentroTrabajoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CentroTrabajoRequest request) {
        return ResponseEntity.ok(centroTrabajoService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar centro de trabajo")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        centroTrabajoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

