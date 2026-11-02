package ar.edu.tpi.commons.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Documento OpenAPI base con esquema de seguridad Bearer (JWT).
 * Solo se activa si el microservicio incluye springdoc.
 */
@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
public class OpenApiBearerConfig {

    static final String ESQUEMA = "bearer-jwt";

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI openApiAgrodrones(
            @Value("${agrodrones.openapi.titulo:${spring.application.name:agrodrones}}") String titulo,
            @Value("${agrodrones.openapi.descripcion:}") String descripcion,
            @Value("${agrodrones.openapi.version:1.0.0}") String version) {
        return new OpenAPI()
                .info(new Info().title(titulo).description(descripcion).version(version))
                .components(new Components().addSecuritySchemes(ESQUEMA, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token de acceso emitido por Keycloak (realm agrodrones)")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
    }
}
