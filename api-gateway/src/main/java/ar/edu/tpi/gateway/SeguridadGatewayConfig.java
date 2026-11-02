package ar.edu.tpi.gateway;

import ar.edu.tpi.commons.error.ErrorResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Security Filter del Gateway.
 *
 * <ul>
 *   <li>Toda ruta /api/** exige un JWT valido emitido por Keycloak (firma, vencimiento y emisor).</li>
 *   <li>/internal/** se rechaza siempre: esas rutas solo se usan dentro de la red interna, entre servicios.</li>
 *   <li>Solo son publicos la documentacion Swagger y el health.</li>
 * </ul>
 * Los permisos finos por rol y la propiedad de los recursos los decide cada microservicio, que vuelve a
 * validar el token (defensa en profundidad).
 */
@Configuration
@EnableWebFluxSecurity
public class SeguridadGatewayConfig {

    @Bean
    SecurityWebFilterChain cadenaGateway(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers("/internal/**").denyAll()
                        .pathMatchers("/actuator/health/**", "/swagger-ui.html", "/swagger-ui/**", "/webjars/**",
                                "/v3/api-docs/**", "/docs/**").permitAll()
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().denyAll())
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> { })
                        .authenticationEntryPoint((exchange, e) -> escribir(exchange, HttpStatus.UNAUTHORIZED,
                                "NO_AUTENTICADO", "Se requiere un token de acceso valido")))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((exchange, ex) -> escribir(exchange, HttpStatus.UNAUTHORIZED,
                                "NO_AUTENTICADO", "Se requiere un token de acceso valido"))
                        .accessDeniedHandler((exchange, ex) -> escribir(exchange, HttpStatus.FORBIDDEN,
                                "ACCESO_DENEGADO", "Ruta no disponible desde el exterior")))
                .build();
    }

    private static Mono<Void> escribir(ServerWebExchange exchange, HttpStatus status, String codigo, String mensaje) {
        ServerHttpResponse respuesta = exchange.getResponse();
        respuesta.setStatusCode(status);
        respuesta.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        if (status == HttpStatus.UNAUTHORIZED) {
            respuesta.getHeaders().set("WWW-Authenticate", "Bearer");
        }
        String json = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), codigo, mensaje,
                exchange.getRequest().getPath().value(), List.of()).toJson();
        DataBuffer cuerpo = respuesta.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return respuesta.writeWith(Mono.just(cuerpo));
    }
}
