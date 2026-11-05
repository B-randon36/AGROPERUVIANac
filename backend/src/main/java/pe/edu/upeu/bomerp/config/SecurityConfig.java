package pe.edu.upeu.bomerp.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<String> rolesCrudos = new LinkedHashSet<>();

            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> listaRealm) {
                listaRealm.stream().filter(Objects::nonNull).map(Object::toString).forEach(rolesCrudos::add);
            }

            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            if (resourceAccess != null) {
                for (Object clienteObj : resourceAccess.values()) {
                    if (clienteObj instanceof Map<?, ?> clienteMap && clienteMap.get("roles") instanceof Collection<?> listaCliente) {
                        listaCliente.stream().filter(Objects::nonNull).map(Object::toString).forEach(rolesCrudos::add);
                    }
                }
            }

            return rolesCrudos.stream()
                    .map(rol -> {
                        String normalizado = rol.trim().toUpperCase();
                        if (normalizado.startsWith("ROLE_")) {
                            return (GrantedAuthority) new SimpleGrantedAuthority(normalizado);
                        }
                        return (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + normalizado);
                    })
                    .toList();
        });
        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                     JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. Endpoints públicos
                        .requestMatchers("/actuator/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/h2-console/**", "/api/v1/personas/**", "/api/v1/auth/login", "/api/v1/auth/usuarios-demo").permitAll()

                        // 2. Catálogo (S08 / S10b): Consulta abierta a cualquier autenticado; Modificación para ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/v1/productos/**", "/api/v1/categorias/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/productos/**", "/api/v1/categorias/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/productos/**", "/api/v1/categorias/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/productos/**", "/api/v1/categorias/**").hasRole("ADMIN")

                        // 3. Ventas y Delivery: Habilitado completamente para ADMIN normal, además de VENDEDOR y SUPERVISOR
                        .requestMatchers("/api/v1/ventas/delivery/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/ventas").hasAnyRole("ADMIN", "VENDEDOR", "SUPERVISOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/ventas/resumen").hasAnyRole("ADMIN", "SUPERVISOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/ventas/*/anular").hasAnyRole("ADMIN", "SUPERVISOR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/ventas/*/anular").hasAnyRole("ADMIN", "SUPERVISOR")
                        .requestMatchers("/api/v1/ventas/**").hasAnyRole("ADMIN", "VENDEDOR", "SUPERVISOR")

                        // 4. Finanzas (Brandon): Habilitado para ADMIN normal, ADMIN_FINANZAS, SUPERVISOR y CAJERO
                        .requestMatchers(HttpMethod.GET, "/api/v1/finanzas/**", "/api/v1/cajas-chicas/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/finanzas/**", "/api/v1/cajas-chicas/**").hasAnyRole("ADMIN", "ADMIN_FINANZAS", "SUPERVISOR", "CAJERO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/finanzas/**", "/api/v1/cajas-chicas/**").hasAnyRole("ADMIN", "ADMIN_FINANZAS", "SUPERVISOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/finanzas/**", "/api/v1/cajas-chicas/**").hasAnyRole("ADMIN", "ADMIN_FINANZAS", "SUPERVISOR", "CAJERO")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/finanzas/**", "/api/v1/cajas-chicas/**").hasAnyRole("ADMIN", "ADMIN_FINANZAS")

                        // 5. Inventario: Lotes, FEFO y Kardex
                        .requestMatchers("/api/v1/inventario/**").hasAnyRole("ADMIN", "SUPERVISOR", "VENDEDOR", "CAJERO")

                        // 6. Producción, Compras y CRM Terceros
                        .requestMatchers("/api/v1/produccion/**").hasAnyRole("ADMIN", "SUPERVISOR")
                        .requestMatchers("/api/v1/compras/**").hasAnyRole("ADMIN", "SUPERVISOR")
                        .requestMatchers("/api/v1/terceros/**").authenticated()

                        // 7. Todo lo demás requiere autenticación
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }
}
