package cl.duocuc.rinconsur.auth.client;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import cl.duocuc.rinconsur.auth.dto.ActividadCuenta;
import cl.duocuc.rinconsur.auth.dto.EstadoCuenta;
import cl.duocuc.rinconsur.auth.dto.PaginaCuentas;
import cl.duocuc.rinconsur.auth.dto.PerfilCuenta;
import cl.duocuc.rinconsur.auth.dto.PerfilDatos;
import cl.duocuc.rinconsur.auth.dto.UsuarioInterno;
import cl.duocuc.rinconsur.auth.dto.UsuarioResponse;
import cl.duocuc.rinconsur.auth.exception.ErrorCuenta;
import cl.duocuc.rinconsur.auth.exception.UsuarioNoEncontradoException;
import cl.duocuc.rinconsur.auth.exception.UsuariosNoDisponibleException;

import reactor.core.publisher.Mono;

@Component
public class UsuarioClient {

    private final WebClient usuarios;

    public UsuarioClient(
            @LoadBalanced WebClient.Builder constructorCliente,
            @Value("${seguridad.interna.token}") String token) {
        usuarios = constructorCliente
                .clone()
                .baseUrl("http://user-service")
                .defaultHeader("X-Service-Token", token)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(512 * 1024))
                .build();
    }

    public UsuarioResponse obtenerPorCorreo(String correo) {
        return consultar(usuarios
                .get()
                .uri(constructorCliente -> constructorCliente
                        .path("/internal/users/por-correo")
                        .queryParam("correo", "{correo}")
                        .build(correo.strip().toLowerCase(Locale.ROOT))),
                UsuarioResponse.class);
    }

    public UsuarioResponse obtenerPorId(long id) {
        return consultar(usuarios.get().uri("/internal/users/{id}", id), UsuarioResponse.class);
    }

    public PaginaCuentas listar(int pagina) {
        return consultar(usuarios.get().uri("/internal/users?pagina={pagina}&cantidad=20", pagina),
                PaginaCuentas.class);
    }

    public UsuarioResponse crear(UsuarioInterno datos, long actor) {
        return consultar(usuarios
                .post()
                .uri("/internal/users")
                .header("X-Actor-Id", Long.toString(actor))
                .bodyValue(datos), UsuarioResponse.class);
    }

    public UsuarioResponse editar(long id, UsuarioInterno datos, long actor) {
        return consultar(usuarios
                .put()
                .uri("/internal/users/{id}", id)
                .header("X-Actor-Id", Long.toString(actor))
                .bodyValue(datos), UsuarioResponse.class);
    }

    public UsuarioResponse estado(long id, boolean activo, long actor) {
        return consultar(usuarios
                .patch()
                .uri("/internal/users/{id}/estado", id)
                .header("X-Actor-Id", Long.toString(actor))
                .bodyValue(new EstadoCuenta(activo)), UsuarioResponse.class);
    }

    public List<ActividadCuenta> actividad() {
        return consultar(usuarios
                .get()
                .uri("/internal/users/auditoria")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<ActividadCuenta>>() {
                }));
    }

    public PerfilCuenta perfil(long id) {
        return consultar(usuarios.get().uri("/internal/users/{id}/perfil", id), PerfilCuenta.class);
    }

    public PerfilCuenta editarPerfil(long id, PerfilDatos datos) {
        return consultar(usuarios.put().uri("/internal/users/{id}/perfil", id).bodyValue(datos),
                PerfilCuenta.class);
    }

    private <T> T consultar(WebClient.RequestHeadersSpec<?> solicitud, Class<T> tipo) {
        return consultar(solicitud.retrieve().bodyToMono(tipo));
    }

    private <T> T consultar(Mono<T> respuesta) {
        try {
            return respuesta
                    .timeout(Duration.ofSeconds(5), Mono.error(new UsuariosNoDisponibleException()))
                    .blockOptional()
                    .orElseThrow(UsuariosNoDisponibleException::new);
        } catch (WebClientResponseException error) {
            throw switch (error.getStatusCode().value()) {
                case 400 -> new ErrorCuenta(HttpStatus.BAD_REQUEST, "Revisa los datos ingresados");
                case 404 -> new UsuarioNoEncontradoException();
                case 409 ->
                    new ErrorCuenta(HttpStatus.CONFLICT, "El correo ya está registrado o la cuenta cambió");
                default -> new UsuariosNoDisponibleException();
            };
        } catch (WebClientException error) {
            throw new UsuariosNoDisponibleException();
        }
    }
}
