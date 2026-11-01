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
import pe.edu.upeu.bomerp.finanzas.dto.CategoriaFinancieraRequest;
import pe.edu.upeu.bomerp.finanzas.dto.CategoriaFinancieraResponse;
import pe.edu.upeu.bomerp.finanzas.dto.TransaccionFinancieraResponse;
import pe.edu.upeu.bomerp.finanzas.entity.TipoFinanciero;
import pe.edu.upeu.bomerp.finanzas.service.FinanzasService;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CategoriaFinancieraController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class
)
@Import(GlobalExceptionHandler.class)
class CategoriaFinancieraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FinanzasService finanzasService;

    @Test
    void crear_conDatosValidos_respondeCreated() throws Exception {
        CategoriaFinancieraRequest request = CategoriaFinancieraRequest.builder()
                .nombre("Ventas Directas")
                .descripcion("Ingresos por venta de productos de Agro Peruvian Foods")
                .tipoDefault(TipoFinanciero.INGRESO)
                .build();

        when(finanzasService.crearCategoria(any())).thenReturn(
                CategoriaFinancieraResponse.builder()
                        .id(1L)
                        .nombre("Ventas Directas")
                        .descripcion("Ingresos por venta de productos de Agro Peruvian Foods")
                        .tipoDefault(TipoFinanciero.INGRESO)
                        .build()
        );

        mockMvc.perform(post("/api/v1/finanzas/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Ventas Directas"));
    }

    @Test
    void obtener_conIdInexistente_respondeNotFound() throws Exception {
        when(finanzasService.obtenerCategoria(999L)).thenThrow(new ResourceNotFoundException("Categoría financiera no encontrada: 999"));

        mockMvc.perform(get("/api/v1/finanzas/categorias/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listarTransaccionesPorCategoria_respondeOkConNavegacionControlada() throws Exception {
        when(finanzasService.listarTransaccionesPorCategoria(1L)).thenReturn(List.of(
                TransaccionFinancieraResponse.builder()
                        .id(100L)
                        .codigo("TRX-001")
                        .monto(new BigDecimal("150.00"))
                        .tipo(TipoFinanciero.INGRESO)
                        .build()
        ));

        mockMvc.perform(get("/api/v1/finanzas/categorias/1/transacciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].codigo").value("TRX-001"));
    }
}
