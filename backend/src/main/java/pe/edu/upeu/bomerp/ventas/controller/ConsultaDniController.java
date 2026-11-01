package pe.edu.upeu.bomerp.ventas.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.bomerp.ventas.dto.DniConsultaResponse;
import pe.edu.upeu.bomerp.ventas.dto.RucConsultaResponse;
import pe.edu.upeu.bomerp.ventas.service.ConsultaDniService;

@Tag(name = "Consultas DNI y RUC / RENIEC y SUNAT", description = "Búsqueda y autocompletado de datos de personas naturales y jurídicas mediante APIsPERU")
@RestController
@RequestMapping("/api/v1/personas")
@RequiredArgsConstructor
public class ConsultaDniController {

    private final ConsultaDniService consultaDniService;

    @Operation(summary = "Consultar datos de persona por DNI (8 dígitos, RENIEC)")
    @GetMapping("/dni/{dni}")
    public ResponseEntity<DniConsultaResponse> consultarPorDni(@PathVariable String dni) {
        return ResponseEntity.ok(consultaDniService.buscarPorDni(dni));
    }

    @Operation(summary = "Consultar datos de empresa o proveedor por RUC (11 dígitos, SUNAT)")
    @GetMapping("/ruc/{ruc}")
    public ResponseEntity<RucConsultaResponse> consultarPorRuc(@PathVariable String ruc) {
        return ResponseEntity.ok(consultaDniService.buscarPorRuc(ruc));
    }
}
