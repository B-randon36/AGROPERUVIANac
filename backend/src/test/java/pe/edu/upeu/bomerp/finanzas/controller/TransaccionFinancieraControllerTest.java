package pe.edu.upeu.bomerp.finanzas.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upeu.bomerp.exception.GlobalExceptionHandler;
import pe.edu.upeu.bomerp.exception.ResourceNotFoundException;
import pe.edu.upeu.bomerp.finanzas.dto.CategoriaFinancieraResumen;
import pe.edu.upeu.bomerp.finanzas.dto.FlujoCajaResumenResponse;
import pe.edu.upeu.bomerp.finanzas.dto.TransaccionFinancieraRequest;
import pe.edu.upeu.bomerp.finanzas.dto.TransaccionFinancieraResponse;
import pe.edu.upeu.bomerp.finanzas.entity.EstadoFinanciero;
import pe.edu.upeu.bomerp.finanzas.entity.TipoFinanciero;
import pe.edu.upeu.bomerp.finanzas.service.FinanzasService;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TransaccionFinancieraController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class
)
@Import(GlobalExceptionHandler.class)
class TransaccionFinancieraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FinanzasService finanzasService;

    @Test
    void asociacion_casoValido_crearConCategoriaValida_responde201ConDtoEmbebido() throws Exception {
        TransaccionFinancieraRequest request = TransaccionFinancieraRequest.builder()
                .codigo("FIN-2026-001")
                .concepto("Venta de 50 frascos de miel de abeja Agro Peruvian Foods")
                .monto(new BigDecimal("450.00"))
                .fechaTransaccion(LocalDate.now())
                .tipo(TipoFinanciero.INGRESO)
                .estado(EstadoFinanciero.PAGADO)
                .categoriaFinancieraId(1L)
                .build();

        when(finanzasService.crearTransaccion(any())).thenReturn(
                TransaccionFinancieraResponse.builder()
                        .id(1L)
                        .codigo("FIN-2026-001")
                        .concepto("Venta de 50 frascos de miel de abeja Agro Peruvian Foods")
                        .monto(new BigDecimal("450.00"))
                        .tipo(TipoFinanciero.INGRESO)
                        .estado(EstadoFinanciero.PAGADO)
                        .categoriaFinanciera(CategoriaFinancieraResumen.builder()
                                .id(1L)
                                .nombre("Ventas Directas")
                                .tipoDefault(TipoFinanciero.INGRESO)
                                .build())
                        .build()
        );

        mockMvc.perform(post("/api/v1/finanzas/transacciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.codigo").value("FIN-2026-001"))
                .andExpect(jsonPath("$.categoriaFinanciera.id").value(1))
                .andExpect(jsonPath("$.categoriaFinanciera.nombre").value("Ventas Directas"));
    }

    @Test
    void asociacion_casoInvalido_crearConCategoriaInexistente_responde404NotFound() throws Exception {
        TransaccionFinancieraRequest request = TransaccionFinancieraRequest.builder()
                .codigo("FIN-2026-999")
                .concepto("Compra de insumos con categoría inválida")
                .monto(new BigDecimal("200.00"))
                .tipo(TipoFinanciero.EGRESO)
                .categoriaFinancieraId(999L)
                .build();

        when(finanzasService.crearTransaccion(any()))
                .thenThrow(new ResourceNotFoundException("Categoría financiera no encontrada: 999"));

        mockMvc.perform(post("/api/v1/finanzas/transacciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Categoría financiera no encontrada: 999"));
    }

    @Test
    void navegacionControlada_casoValido_filtrarPorCategoriaId_responde200ConLista() throws Exception {
        when(finanzasService.listarTransacciones(null, null, 1L)).thenReturn(List.of(
                TransaccionFinancieraResponse.builder()
                        .id(100L)
                        .codigo("FIN-2026-010")
                        .monto(new BigDecimal("300.00"))
                        .categoriaFinanciera(CategoriaFinancieraResumen.builder().id(1L).nombre("Ventas Directas").build())
                        .build()
        ));

        mockMvc.perform(get("/api/v1/finanzas/transacciones").param("categoriaFinancieraId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].categoriaFinanciera.id").value(1));
    }

    @Test
    void navegacionControlada_casoInvalido_filtrarPorCategoriaInexistente_responde404NotFound() throws Exception {
        when(finanzasService.listarTransacciones(null, null, 999L))
                .thenThrow(new ResourceNotFoundException("Categoría financiera no encontrada: 999"));

        mockMvc.perform(get("/api/v1/finanzas/transacciones").param("categoriaFinancieraId", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void obtener_conIdInexistente_respondeNotFound() throws Exception {
        when(finanzasService.obtenerTransaccion(999L))
                .thenThrow(new ResourceNotFoundException("Transacción financiera no encontrada: 999"));

        mockMvc.perform(get("/api/v1/finanzas/transacciones/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void obtenerResumenFlujoCaja_respondeOk() throws Exception {
        when(finanzasService.obtenerResumenFlujoCaja()).thenReturn(
                FlujoCajaResumenResponse.builder()
                        .totalIngresos(new BigDecimal("5000.00"))
                        .totalEgresos(new BigDecimal("2000.00"))
                        .balanceNeto(new BigDecimal("3000.00"))
                        .cuentasPorCobrarPendientes(new BigDecimal("800.00"))
                        .cuentasPorPagarPendientes(new BigDecimal("400.00"))
                        .totalTransacciones(15)
                        .build()
        );

        mockMvc.perform(get("/api/v1/finanzas/resumen-flujo-caja"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIngresos").value(5000.00))
                .andExpect(jsonPath("$.balanceNeto").value(3000.00));
    }
}
