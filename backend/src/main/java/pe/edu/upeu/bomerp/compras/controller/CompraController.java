package pe.edu.upeu.bomerp.compras.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.bomerp.compras.dto.*;
import pe.edu.upeu.bomerp.compras.entity.EstadoCompra;
import pe.edu.upeu.bomerp.compras.service.CompraService;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Compras - Insumos y Proveedores", description = "Adquisición de materias primas, insumos y cuentas por pagar (Elishan Huaylla)")
@RestController
@RequestMapping("/api/v1/compras")
@RequiredArgsConstructor
public class CompraController {

    private final CompraService compraService;

    @Operation(summary = "Catálogo maestro de insumos y materiales")
    @GetMapping("/insumos")
    public ResponseEntity<List<InsumoDto>> listarInsumos() {
        return ResponseEntity.ok(compraService.listarInsumos());
    }

    @Operation(summary = "Registrar un nuevo proveedor")
    @PostMapping("/proveedores")
    public ResponseEntity<ProveedorResponse> crearProveedor(@Valid @RequestBody ProveedorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compraService.crearProveedor(request));
    }

    @Operation(summary = "Listar todos los proveedores registrados")
    @GetMapping("/proveedores")
    public ResponseEntity<List<ProveedorResponse>> listarProveedores() {
        return ResponseEntity.ok(compraService.listarProveedores());
    }

    @Operation(summary = "Registrar una compra con su colección de insumos adquiridos")
    @PostMapping
    public ResponseEntity<CompraResponse> registrarCompra(@Valid @RequestBody CompraRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compraService.registrarCompra(request));
    }

    @Operation(summary = "Amortizar pago o liquidar saldo pendiente de una compra")
    @PutMapping("/{id}/amortizar")
    public ResponseEntity<CompraResponse> amortizarPago(@PathVariable Long id, @Valid @RequestBody AmortizarPagoRequest request) {
        return ResponseEntity.ok(compraService.amortizarPago(id, request));
    }

    @Operation(summary = "Consultar el historial de pagos de amortización de una compra")
    @GetMapping("/{id}/pagos")
    public ResponseEntity<List<PagoCompraResponse>> listarPagos(@PathVariable Long id) {
        return ResponseEntity.ok(compraService.listarPagosPorCompra(id));
    }

    @Operation(summary = "Anular una compra y liquidar saldo pendiente")
    @PostMapping("/{id}/anular")
    public ResponseEntity<CompraResponse> anularCompra(@PathVariable Long id) {
        return ResponseEntity.ok(compraService.anularCompra(id));
    }

    @Operation(summary = "Generar reporte agregado de compras y cuentas por pagar")
    @GetMapping("/resumen")
    public ResponseEntity<CompraReporte> reporte(
            @RequestParam(required = false) EstadoCompra estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(compraService.reporte(estado, desde, hasta));
    }

    @Operation(summary = "Consultar compra por ID con el detalle de insumos adquiridos")
    @GetMapping("/{id}")
    public ResponseEntity<CompraResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(compraService.obtener(id));
    }

    @Operation(summary = "Listar compras registradas con filtros opcionales de estado y fecha")
    @GetMapping
    public ResponseEntity<List<CompraResponse>> listarCompras(
            @RequestParam(required = false) EstadoCompra estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        if (estado != null || desde != null || hasta != null) {
            return ResponseEntity.ok(compraService.listarCompras(estado, desde, hasta));
        }
        return ResponseEntity.ok(compraService.listarCompras());
    }
}
