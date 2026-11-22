package cl.duocuc.rinconsur.operations;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

@Component
public class Identidad {

    private final WebClient clienteAutenticacion;
    private final WebClient clienteUsuarios;

    public Identidad(
            @LoadBalanced WebClient.Builder constructorCliente,
            @Value("${identidad.token}") String token
    ) {
        clienteAutenticacion = constructorCliente.clone()
                .baseUrl("http://auth-service")
                .build();
        clienteUsuarios = constructorCliente.clone()
                .baseUrl("http://user-service")
                .defaultHeader("X-Service-Token", token)
                .build();
    }

    public Usuario autenticar(String autorizacion) {
        if (autorizacion == null || autorizacion.isBlank()) {
            throw sinSesion();
        }

        try {
            WebClient.RequestHeadersSpec<?> solicitud = clienteAutenticacion.get()
                    .uri("/api/v1/auth/me")
                    .header(HttpHeaders.AUTHORIZATION, autorizacion);

            Usuario usuario = consultar(solicitud, Usuario.class);
            if (!usuario.activo()) {
                throw sinSesion();
            }
            return usuario;
        } catch (WebClientResponseException.Unauthorized error) {
            throw sinSesion();
        } catch (WebClientException error) {
            throw indisponible();
        }
    }

    public Usuario observador(long id) {
        try {
            WebClient.RequestHeadersSpec<?> solicitud = clienteUsuarios.get().uri("/internal/users/{id}", id);
            Usuario usuario = consultar(solicitud, Usuario.class);
            if (!usuario.activo() || usuario.rol() != Rol.OBSERVADOR) {
                throw observadorInvalido();
            }
            return usuario;
        } catch (WebClientResponseException.NotFound error) {
            throw observadorInvalido();
        } catch (WebClientException error) {
            throw indisponible();
        }
    }

    public List<Usuario> usuariosActivos() {
        List<Usuario> activos = new ArrayList<>();
        try {
            int pagina = 0;
            PaginaUsuarios resultado;
            do {
                WebClient.RequestHeadersSpec<?> solicitud = clienteUsuarios.get()
                        .uri("/internal/users?pagina={pagina}&cantidad=100", pagina++);

                resultado = consultar(solicitud, PaginaUsuarios.class);
                resultado.usuarios()
                        .stream()
                        .filter(Usuario::activo)
                        .forEach(activos::add);
            } while (pagina < resultado.totalPaginas());
            return activos;
        } catch (WebClientException error) {
            throw indisponible();
        }
    }

    private <T> T consultar(WebClient.RequestHeadersSpec<?> solicitud, Class<T> tipo) {
        T resultado = solicitud.retrieve()
                .bodyToMono(tipo)
                .timeout(Duration.ofSeconds(5), Mono.error(indisponible()))
                .block();

        if (resultado == null) {
            throw indisponible();
        }
        return resultado;
    }

    private ErrorOperacion sinSesion() {
        return new ErrorOperacion(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión");
    }

    private ErrorOperacion observadorInvalido() {
        return new ErrorOperacion(HttpStatus.BAD_REQUEST, "Selecciona un observador activo");
    }

    private ErrorOperacion indisponible() {
        return new ErrorOperacion(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de identidad no está disponible");
    }
}

@Configuration(proxyBeanMethods = false)
class ClientesConfig {

    @Bean
    @LoadBalanced
    WebClient.Builder clienteInterno() {
        HttpClient cliente = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        return WebClient.builder().clientConnector(new JdkClientHttpConnector(cliente));
    }
}
