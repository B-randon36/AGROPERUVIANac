package pe.edu.upeu.bomerp.ventas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upeu.bomerp.config.CorsConfig;
import pe.edu.upeu.bomerp.config.SecurityConfig;
import pe.edu.upeu.bomerp.ventas.controller.VentaController;
import pe.edu.upeu.bomerp.ventas.service.AuditoriaService;
import pe.edu.upeu.bomerp.ventas.service.VentaService;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VentaController.class)
@Import({SecurityConfig.class, CorsConfig.class, VentaSecurityTest.SecurityInfrastructure.class})
class VentaSecurityTest {
    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebSecurity
    static class SecurityInfrastructure { }
    @Autowired MockMvc mvc;
    @MockitoBean VentaService ventas;
    @MockitoBean AuditoriaService auditoria;
    @MockitoBean JwtDecoder decoder;

    @Test
    void auditoriaSinSesion_requiereAutenticacion() throws Exception {
        mvc.perform(get("/api/v1/ventas/auditoria")).andExpect(status().isUnauthorized());
        verifyNoInteractions(auditoria);
    }

    @Test
    void vendedor_noAccedeAAuditoriaNiReporteGlobal() throws Exception {
        for (String ruta : List.of("auditoria", "reporte-omnicanal")) {
            mvc.perform(get("/api/v1/ventas/" + ruta)
                    .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR"))))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(ventas, auditoria);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "SUPERVISOR"})
    void gestores_consultanAuditoriaYReporteGlobal(String rol) throws Exception {
        mvc.perform(get("/api/v1/ventas/auditoria")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/ventas/reporte-omnicanal")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol))))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "SUPERVISOR", "VENDEDOR"})
    void nadiePuedeBorrarVentas(String rol) throws Exception {
        for (String ruta : List.of("/api/v1/ventas", "/api/v1/ventas/1")) {
            mvc.perform(delete(ruta).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol))))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(ventas);
    }

    @ParameterizedTest
    @ValueSource(strings = {"realm_access", "resource_access"})
    void vendedor_conCualquieraDeLosOrigenesDeRol_siempreSeFiltra(String origen) throws Exception {
        Object roles = origen.equals("realm_access") ? Map.of("roles", List.of("vendedor"))
                : Map.of("frontend", Map.of("roles", List.of("vendedor")));
        Jwt token = Jwt.withTokenValue("token-prueba").header("alg", "RS256")
                .subject("vendedor-1").claim(origen, roles).build();
        when(decoder.decode("token-prueba")).thenReturn(token);
        mvc.perform(get("/api/v1/ventas/resumen").header("Authorization", "Bearer token-prueba")
                .param("vendedorId", "otro-vendedor"))
                .andExpect(status().isOk());
        verify(ventas).reporte(null, null, null, null, "vendedor-1");
        mvc.perform(get("/api/v1/ventas/7").header("Authorization", "Bearer token-prueba"))
                .andExpect(status().isOk());
        verify(ventas).obtener(7L, "vendedor-1");
        mvc.perform(patch("/api/v1/ventas/7/solicitar-anulacion").header("Authorization", "Bearer token-prueba"))
                .andExpect(status().isOk());
        verify(ventas).solicitarAnulacion(7L, null, "vendedor-1", "vendedor-1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "SUPERVISOR"})
    void gestoresVenTotalesGlobales(String rol) throws Exception {
        mvc.perform(get("/api/v1/ventas/resumen")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol))))
                .andExpect(status().isOk());
        verify(ventas).reporte(null, null, null, null, null);
    }

    @Test
    void vendedorNoPuedeAnularDirectamenteNiRechazar() throws Exception {
        for (String accion : List.of("anular", "rechazar-anulacion")) {
            mvc.perform(patch("/api/v1/ventas/7/" + accion)
                    .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR"))))
                    .andExpect(status().isForbidden());
            mvc.perform(put("/api/v1/ventas/7/" + accion)
                    .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR"))))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/v1/ventas/7/rechazar-anulacion")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(ventas);
    }
}
