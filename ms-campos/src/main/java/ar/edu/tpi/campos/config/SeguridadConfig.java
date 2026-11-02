package ar.edu.tpi.campos.config;

import ar.edu.tpi.commons.seguridad.KeycloakRolesConverter;
import ar.edu.tpi.commons.seguridad.RespuestasSeguridad;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * El servicio es un Resource Server: valida el JWT de Keycloak en cada peticion,
 * aunque ya lo haya validado el Gateway (defensa en profundidad dentro de la red interna).
 * Los permisos finos por rol se declaran con @PreAuthorize en cada endpoint.
 */
@Configuration
@EnableMethodSecurity
public class SeguridadConfig {

    @Bean
    SecurityFilterChain cadenaSeguridad(HttpSecurity http, KeycloakRolesConverter rolesConverter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()
                        .requestMatchers("/internal/**").hasRole("SERVICIO")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(rolesConverter))
                        .authenticationEntryPoint(RespuestasSeguridad.noAutenticado())
                        .accessDeniedHandler(RespuestasSeguridad.accesoDenegado()))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(RespuestasSeguridad.noAutenticado())
                        .accessDeniedHandler(RespuestasSeguridad.accesoDenegado()));
        return http.build();
    }
}
