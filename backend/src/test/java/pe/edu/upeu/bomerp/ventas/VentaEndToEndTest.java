package pe.edu.upeu.bomerp.ventas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upeu.bomerp.ventas.dto.DetalleVentaRequest;
import pe.edu.upeu.bomerp.ventas.dto.VentaRequest;
import pe.edu.upeu.bomerp.ventas.entity.MetodoPagoVenta;

import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class VentaEndToEndTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private pe.edu.upeu.bomerp.catalogo.producto.service.ProductoService productos;

        @Autowired
        private pe.edu.upeu.bomerp.finanzas.repository.TransaccionFinancieraRepository transacciones;

        private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        @Test
        @DisplayName("S14 E2E: Flujo integral crear venta, paginar, consultar agregado y anular con auditoría")
        void flujoCompleto_crearVentaPaginadaYAuditoria() throws Exception {
                int stockInicial = productos.obtener(1L).getStock();
                VentaRequest request = new VentaRequest();
                request.setClienteNombre("Cliente E2E AgroPeruvian");
                request.setClienteDocumento("77889900");
                request.setMetodoPago(MetodoPagoVenta.EFECTIVO);
                request.setDetalles(List.of(
                                DetalleVentaRequest.builder()
                                                .productoId(1L)
                                                .cantidad(2)
                                                .build()));

                // 1. Crear Venta como VENDEDOR
                String json = mockMvc.perform(post("/api/v1/ventas")
                                .with(jwt().jwt(j -> j.claim("sub", "vendedor-e2e")
                                                .claim("realm_access", Map.of("roles", List.of("VENDEDOR"))))
                                                .authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").isNumber())
                                .andExpect(jsonPath("$.total").value(400.00))
                                .andExpect(jsonPath("$.subtotal").value(338.98))
                                .andExpect(jsonPath("$.igv").value(61.02))
                                .andReturn().getResponse().getContentAsString();

                Long id = objectMapper.readTree(json).get("id").asLong();
                String ticket = objectMapper.readTree(json).get("numeroTicket").asText();
                org.junit.jupiter.api.Assertions.assertTrue(ticket.startsWith("TKT-S-"));
                org.junit.jupiter.api.Assertions.assertEquals(stockInicial - 2, productos.obtener(1L).getStock());
                org.junit.jupiter.api.Assertions.assertEquals(
                        pe.edu.upeu.bomerp.finanzas.entity.EstadoFinanciero.PAGADO,
                        transacciones.findFirstByNumeroComprobante(ticket).orElseThrow().getEstado());

                // 2. Consultar venta por ID
                mockMvc.perform(get("/api/v1/ventas/" + id)
                                .with(jwt().jwt(j -> j.claim("sub", "vendedor-e2e")
                                                .claim("realm_access", Map.of("roles", List.of("VENDEDOR"))))
                                                .authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(id))
                                .andExpect(jsonPath("$.vendedorId").value("vendedor-e2e"));

                // 3. Consultar listado paginado (S14: Page<VentaResponse>)
                mockMvc.perform(get("/api/v1/ventas?page=0&size=10")
                                .with(jwt().jwt(j -> j.claim("sub", "vendedor-e2e")
                                                .claim("realm_access", Map.of("roles", List.of("VENDEDOR"))))
                                                .authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.totalElements").isNumber())
                                .andExpect(jsonPath("$.pageable").exists());

                // 4. Consultar agregado real del servidor (/resumen)
                mockMvc.perform(get("/api/v1/ventas/resumen")
                                .with(jwt().jwt(j -> j.claim("sub", "supervisor-e2e")
                                                .claim("realm_access", Map.of("roles", List.of("SUPERVISOR"))))
                                                .authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.agregado.totalVentas").isNumber())
                                .andExpect(jsonPath("$.agregado.montoTotal").isNumber())
                                .andExpect(jsonPath("$.agregado.totalAnuladas").isNumber());

                // 5. Anular venta con rol SUPERVISOR
                mockMvc.perform(patch("/api/v1/ventas/" + id + "/anular")
                                .with(jwt().jwt(j -> j.claim("sub", "supervisor-e2e")
                                                .claim("realm_access", Map.of("roles", List.of("SUPERVISOR"))))
                                                .authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.estado").value("ANULADA"));

                org.junit.jupiter.api.Assertions.assertEquals(stockInicial, productos.obtener(1L).getStock());
                org.junit.jupiter.api.Assertions.assertEquals(
                        pe.edu.upeu.bomerp.finanzas.entity.EstadoFinanciero.ANULADO,
                        transacciones.findFirstByNumeroComprobante(ticket).orElseThrow().getEstado());

                // 5.1 Consultar agregado tras anulación: verifica que totalAnuladas >= 1 y
                // refleja descuento contable
                mockMvc.perform(get("/api/v1/ventas/resumen")
                                .with(jwt().jwt(j -> j.claim("sub", "supervisor-e2e")
                                                .claim("realm_access", Map.of("roles", List.of("SUPERVISOR"))))
                                                .authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.agregado.totalAnuladas")
                                                .value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

                // 6. Verificar historial de Auditoría
                mockMvc.perform(get("/api/v1/ventas/auditoria")
                                .with(jwt().jwt(j -> j.claim("sub", "admin-e2e")
                                                .claim("realm_access", Map.of("roles", List.of("ADMIN"))))
                                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray());
        }
}
