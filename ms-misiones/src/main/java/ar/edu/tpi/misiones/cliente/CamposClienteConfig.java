package ar.edu.tpi.misiones.cliente;

import java.net.http.HttpClient;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

/**
 * Llamada interna ms-misiones a ms-campos con client credentials.
 *
 * <p>El token no es el del usuario: ms-misiones se autentica como si mismo ante Keycloak y recibe el rol
 * SERVICIO, que es el unico que ms-campos acepta en /internal/**. El token se cachea y se renueva solo.
 */
@Configuration
public class CamposClienteConfig {

    /** Identidad fija con la que se guarda el token de servicio (no depende del usuario de la peticion). */
    private static final Authentication IDENTIDAD_SERVICIO =
            UsernamePasswordAuthenticationToken.authenticated("ms-misiones", null, List.of());

    @Bean
    OAuth2AuthorizedClientManager gestorTokensServicio(ClientRegistrationRepository registraciones,
                                                       OAuth2AuthorizedClientService clientes) {
        var gestor = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registraciones, clientes);
        gestor.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
        return gestor;
    }

    @Bean
    CamposCliente camposCliente(RestClient.Builder builder, OAuth2AuthorizedClientManager gestor,
                                CamposProperties props) {
        var interceptor = new OAuth2ClientHttpRequestInterceptor(gestor);
        interceptor.setClientRegistrationIdResolver(request -> props.registracion());
        interceptor.setPrincipalResolver(request -> IDENTIDAD_SERVICIO);

        HttpClient http = HttpClient.newBuilder().connectTimeout(props.timeoutConexion()).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(props.timeoutLectura());

        RestClient cliente = builder.clone()
                .baseUrl(props.url())
                .requestFactory(fabrica)
                .requestInterceptor(interceptor)
                .build();
        return new CamposCliente(cliente, props.reintentarEnSegundos());
    }
}
