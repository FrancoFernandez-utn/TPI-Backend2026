package ar.edu.tpi.commons.seguridad;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuracion de seguridad comun a los microservicios funcionales.
 *
 * <p>Cada servicio es un Resource Server: valida el JWT de Keycloak en cada peticion aunque ya
 * lo haya validado el Gateway (defensa en profundidad dentro de la red interna). Las rutas
 * {@code /internal/**} exigen el rol SERVICIO; el resto exige un usuario autenticado y los
 * permisos por rol se declaran con {@code @PreAuthorize} en cada endpoint.
 *
 * <p>Un servicio puede reemplazarla declarando su propio {@link SecurityFilterChain}.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({SecurityFilterChain.class, HttpSecurity.class})
@EnableMethodSecurity
public class SeguridadServicioAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain cadenaSeguridadServicio(HttpSecurity http, KeycloakRolesConverter rolesConverter)
            throws Exception {
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
