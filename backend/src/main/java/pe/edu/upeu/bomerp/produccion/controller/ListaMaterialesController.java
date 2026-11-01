package pe.edu.upeu.bomerp.produccion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.bomerp.produccion.dto.ListaMaterialesRequest;
import pe.edu.upeu.bomerp.produccion.dto.ListaMaterialesResponse;
import pe.edu.upeu.bomerp.produccion.service.ListaMaterialesService;

import java.util.List;

@Tag(name = "Producción - Recetas Maestras BOM", description = "Gestión persistente de Listas de Materiales (Bill of Materials) por producto terminado")
@RestController
@RequestMapping("/api/v1/produccion/listas-materiales")
@RequiredArgsConstructor
public class ListaMaterialesController {

    private final ListaMaterialesService listaMaterialesService;

    @Operation(summary = "Listar todas las recetas maestras BOM")
    @GetMapping
    public ResponseEntity<List<ListaMaterialesResponse>> listar() {
        return ResponseEntity.ok(listaMaterialesService.listar());
    }

    @Operation(summary = "Obtener receta maestra BOM por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ListaMaterialesResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(listaMaterialesService.obtener(id));
    }

    @Operation(summary = "Obtener receta maestra BOM activa vinculada a un producto terminado")
    @GetMapping("/por-producto/{productoId}")
    public ResponseEntity<ListaMaterialesResponse> obtenerPorProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(listaMaterialesService.obtenerActivaPorProducto(productoId));
    }

    @Operation(summary = "Registrar nueva receta maestra BOM con insumos requeridos")
    @PostMapping
    public ResponseEntity<ListaMaterialesResponse> crear(@Valid @RequestBody ListaMaterialesRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(listaMaterialesService.crear(request));
    }

    @Operation(summary = "Actualizar receta maestra BOM existente")
    @PutMapping("/{id}")
    public ResponseEntity<ListaMaterialesResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ListaMaterialesRequest request) {
        return ResponseEntity.ok(listaMaterialesService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar receta maestra BOM")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        listaMaterialesService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

