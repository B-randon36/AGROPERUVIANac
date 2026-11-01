package pe.edu.upeu.bomerp.finanzas.controller;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upeu.bomerp.exception.GlobalExceptionHandler;
import pe.edu.upeu.bomerp.exception.SolvenciaInsuficienteException;
import pe.edu.upeu.bomerp.finanzas.dto.DetalleRendicionRequest;
import pe.edu.upeu.bomerp.finanzas.dto.DetalleRendicionResponse;
import pe.edu.upeu.bomerp.finanzas.dto.RendicionCajaChicaRequest;
import pe.edu.upeu.bomerp.finanzas.dto.RendicionCajaChicaResponse;
import pe.edu.upeu.bomerp.finanzas.entity.EstadoRendicion;
import pe.edu.upeu.bomerp.finanzas.service.RendicionCajaChicaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = RendicionCajaChicaController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class
)
@Import(GlobalExceptionHandler.class)
class RendicionCajaChicaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RendicionCajaChicaService rendicionService;

    @MockitoBean
    private pe.edu.upeu.bomerp.finanzas.service.SesionCajaService sesionCajaService;

    @Test
    @DisplayName("POST /rendiciones responde 201 Created cuando la rendición cumple con RN-FIN-01")
    void registrarRendicion_exito() throws Exception {
        RendicionCajaChicaRequest request = RendicionCajaChicaRequest.builder()
                .codigo("REND-2026-TEST")
                .responsable("Carlos M.")
                .cajaChicaId(1L)
                .detalles(List.of(
                        DetalleRendicionRequest.builder()
                                .concepto("Gasto de oficina")
                                .cantidad(2)
                                .precioUnitario(new BigDecimal("50.00"))
                                .build()
                ))
                .build();

        RendicionCajaChicaResponse response = RendicionCajaChicaResponse.builder()
                .id(10L)
                .codigo("REND-2026-TEST")
                .responsable("Carlos M.")
                .fecha(LocalDate.now())
                .montoTotal(new BigDecimal("100.00"))
                .estado(EstadoRendicion.APROBADO)
                .cajaChicaId(1L)
                .detalles(List.of(
                        DetalleRendicionResponse.builder()
                                .id(1L)
                                .concepto("Gasto de oficina")
                                .cantidad(2)
                                .precioUnitario(new BigDecimal("50.00"))
                                .subtotal(new BigDecimal("100.00"))
                                .build()
                ))
                .build();

        when(rendicionService.registrarRendicion(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/finanzas/rendiciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.codigo").value("REND-2026-TEST"))
                .andExpect(jsonPath("$.montoTotal").value(100.00))
                .andExpect(jsonPath("$.detalles[0].subtotal").value(100.00));
    }

    @Test
    @DisplayName("POST /rendiciones responde 400 Bad Request cuando se viola la regla RN-FIN-01 (SolvenciaInsuficienteException)")
    void registrarRendicion_errorSolvencia() throws Exception {
        RendicionCajaChicaRequest request = RendicionCajaChicaRequest.builder()
                .codigo("REND-FAIL")
                .responsable("Carlos M.")
                .cajaChicaId(1L)
                .detalles(List.of(
                        DetalleRendicionRequest.builder()
                                .concepto("Gasto desmedido")
                                .cantidad(1)
                                .precioUnitario(new BigDecimal("9999.00"))
                                .build()
                ))
                .build();

        when(rendicionService.registrarRendicion(any())).thenThrow(
                new SolvenciaInsuficienteException("RN-FIN-01: El monto total de la rendición (S/ 9999.00) supera la solvencia/saldo disponible de Caja Chica (S/ 1000.00)")
        );

        mockMvc.perform(post("/api/v1/finanzas/rendiciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.reglaNegocio").value("RN-FIN-01"))
                .andExpect(jsonPath("$.message").value(containsString("supera la solvencia/saldo disponible")));
    }

    @Test
    @DisplayName("GET /rendiciones/paginado responde 200 OK con estructura de página")
    void listarRendicionesPaginado_exito() throws Exception {
        RendicionCajaChicaResponse item = RendicionCajaChicaResponse.builder()
                .id(1L)
                .codigo("REND-2026-PAG")
                .responsable("Carlos M.")
                .fecha(LocalDate.now())
                .montoTotal(new BigDecimal("100.00"))
                .estado(EstadoRendicion.APROBADO)
                .cajaChicaId(1L)
                .build();

        org.springframework.data.domain.Page<RendicionCajaChicaResponse> page =
                new org.springframework.data.domain.PageImpl<>(List.of(item));

        when(rendicionService.listarPaginado(any())).thenReturn(page);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/finanzas/rendiciones/paginado")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].codigo").value("REND-2026-PAG"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /rendiciones/filtros responde 200 OK con resultados según combinación de filtros")
    void buscarRendicionesConFiltros_exito() throws Exception {
        RendicionCajaChicaResponse item = RendicionCajaChicaResponse.builder()
                .id(1L)
                .codigo("REND-FILTROS")
                .responsable("Carlos M.")
                .fecha(LocalDate.now())
                .montoTotal(new BigDecimal("250.00"))
                .estado(EstadoRendicion.APROBADO)
                .cajaChicaId(1L)
                .build();

        org.springframework.data.domain.Page<RendicionCajaChicaResponse> page =
                new org.springframework.data.domain.PageImpl<>(List.of(item));

        when(rendicionService.buscarConFiltros(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/finanzas/rendiciones/filtros")
                        .param("cajaChicaId", "1")
                        .param("estado", "APROBADO")
                        .param("responsable", "Carlos")
                        .param("sort", "montoTotal,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codigo").value("REND-FILTROS"))
                .andExpect(jsonPath("$.content[0].montoTotal").value(250.00));
    }

    @Test
    @DisplayName("GET /rendiciones/resumen responde 200 OK con lista de DTOs proyectados")
    void obtenerResumen_exito() throws Exception {
        pe.edu.upeu.bomerp.finanzas.dto.RendicionResumenDto resumen = pe.edu.upeu.bomerp.finanzas.dto.RendicionResumenDto.builder()
                .id(1L)
                .codigo("REND-RESUMEN")
                .responsable("Carlos M.")
                .fecha(LocalDate.now())
                .montoTotal(new BigDecimal("300.00"))
                .estado(EstadoRendicion.APROBADO)
                .codigoCajaChica("CC-001")
                .cantidadDetalles(3)
                .build();

        when(rendicionService.obtenerResumen(any())).thenReturn(List.of(resumen));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/finanzas/rendiciones/resumen")
                        .param("cajaChicaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("REND-RESUMEN"))
                .andExpect(jsonPath("$[0].codigoCajaChica").value("CC-001"))
                .andExpect(jsonPath("$[0].cantidadDetalles").value(3));
    }

    @Test
    @DisplayName("GET /rendiciones/reporte-agregado responde 200 OK con totales y promedio (no null)")
    void obtenerReporteAgregado_exito() throws Exception {
        pe.edu.upeu.bomerp.finanzas.dto.ReporteRendicionesAgregadoDto reporte = pe.edu.upeu.bomerp.finanzas.dto.ReporteRendicionesAgregadoDto.builder()
                .totalMonto(new BigDecimal("500.00"))
                .promedioMonto(250.0)
                .montoMaximo(new BigDecimal("300.00"))
                .montoMinimo(new BigDecimal("200.00"))
                .cantidadRendiciones(2L)
                .build();

        when(rendicionService.obtenerReporteAgregado(any())).thenReturn(reporte);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/finanzas/rendiciones/reporte-agregado")
                        .param("cajaChicaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMonto").value(500.00))
                .andExpect(jsonPath("$.promedioMonto").value(250.0))
                .andExpect(jsonPath("$.cantidadRendiciones").value(2));
    }
}
