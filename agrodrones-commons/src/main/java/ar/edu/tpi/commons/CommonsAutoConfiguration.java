package ar.edu.tpi.commons;

import ar.edu.tpi.commons.error.ManejadorGlobalErrores;
import ar.edu.tpi.commons.seguridad.KeycloakRolesConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Registra automaticamente los componentes comunes en cada microservicio que
 * agrega la dependencia, sin que el servicio tenga que escanear este paquete.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ManejadorGlobalErrores manejadorGlobalErrores() {
        return new ManejadorGlobalErrores();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(Jwt.class)
    public KeycloakRolesConverter keycloakRolesConverter() {
        return new KeycloakRolesConverter();
    }
}
