package cl.duocuc.rinconsur.auth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import cl.duocuc.rinconsur.auth.client.CorreoClient;
import cl.duocuc.rinconsur.auth.client.UsuarioClient;
import cl.duocuc.rinconsur.auth.dto.*;
import cl.duocuc.rinconsur.auth.exception.*;
import reactor.core.publisher.Mono;

class UsuarioClientTest {
    private static final String USUARIO = """
            {"id":7,"nombre":"Eva","correo":"eva@example.org","rol":"OBSERVADOR","activo":true}
            """;
    private final ExchangeFunction transporte = mock(ExchangeFunction.class);
    private final WebClient.Builder builder = WebClient.builder().exchangeFunction(transporte);
    private final UsuarioClient cliente = new UsuarioClient(builder, "token-interno-de-prueba");

    @Test
    void normalizaElCorreoSinPerderElSignoMas() {
        responder(USUARIO);
        assertThat(cliente.obtenerPorCorreo(" Eva+Turno@Example.org ").id()).isEqualTo(7);
        verificar(HttpMethod.GET, "/internal/users/por-correo?correo=eva%2Bturno%40example.org", null);
    }

    @Test
    void listaCuentasPaginadas() {
        responder("""
                {"usuarios":[%s],"pagina":2,"totalPaginas":3,"totalUsuarios":41}
                """.formatted(USUARIO));
        var pagina = cliente.listar(2);
        assertThat(pagina.totalUsuarios()).isEqualTo(41);
        assertThat(pagina.usuarios()).extracting(UsuarioResponse::id).containsExactly(7L);
        verificar(HttpMethod.GET, "/internal/users?pagina=2&cantidad=20", null);
    }

    @Test
    void enviaElActorEnLasOperacionesDeAdministracion() {
        responder(USUARIO);
        var datos = new UsuarioInterno("Eva", "eva@example.org", RolCuenta.OBSERVADOR, true);

        assertThat(cliente.crear(datos, 9).id()).isEqualTo(7);
        verificar(HttpMethod.POST, "/internal/users", "9");
        clearInvocations(transporte);
        assertThat(cliente.editar(7, datos, 9).correo()).isEqualTo(datos.correo());
        verificar(HttpMethod.PUT, "/internal/users/7", "9");
        clearInvocations(transporte);
        assertThat(cliente.estado(7, true, 9).activo()).isTrue();
        verificar(HttpMethod.PATCH, "/internal/users/7/estado", "9");
    }

    @Test
    void consultaAuditoriaYAdministraElPerfilSinActorAdministrativo() {
        responder("[]");
        assertThat(cliente.actividad()).isEmpty();
        verificar(HttpMethod.GET, "/internal/users/auditoria", null);
        clearInvocations(transporte);
        responder("""
                {"id":7,"nombre":"Eva","apellido":"Rojas","correo":"eva@example.org",
                 "rol":"OBSERVADOR","telefono":"","activo":true}
                """);
        assertThat(cliente.perfil(7).apellido()).isEqualTo("Rojas");
        verificar(HttpMethod.GET, "/internal/users/7/perfil", null);
        clearInvocations(transporte);
        assertThat(cliente.editarPerfil(7, new PerfilDatos("Eva", "Rojas", "eva@example.org", "", null)).id())
                .isEqualTo(7);
        verificar(HttpMethod.PUT, "/internal/users/7/perfil", null);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 409, 500, 204})
    void traduceRespuestasSinUsuario(int status) {
        when(transporte.exchange(any())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.valueOf(status)).build()));
        var error = catchThrowable(() -> cliente.obtenerPorId(7));
        switch (status) {
            case 400, 409 -> assertThat(error).isInstanceOfSatisfying(ErrorCuenta.class,
                    cuenta -> assertThat(cuenta.getEstado().value()).isEqualTo(status));
            case 404 -> assertThat(error).isInstanceOf(UsuarioNoEncontradoException.class);
            default -> assertThat(error).isInstanceOf(UsuariosNoDisponibleException.class);
        }
    }

    @Test
    void manejaLaFallaDeTransporteAlConsultarUsuariosOEnviarCorreo() {
        when(transporte.exchange(any())).thenAnswer(invocacion -> {
            ClientRequest solicitud = invocacion.getArgument(0);
            return Mono.error(new WebClientRequestException(new java.io.IOException("Servicio no disponible"),
                    solicitud.method(), solicitud.url(), solicitud.headers()));
        });
        assertThatThrownBy(() -> cliente.obtenerPorId(7)).isInstanceOf(UsuariosNoDisponibleException.class);
        clearInvocations(transporte);

        assertThatCode(() -> new CorreoClient(builder, "token-interno-de-prueba")
                .enviar("eva@example.org", "ab".repeat(32))).doesNotThrowAnyException();
        verify(transporte).exchange(argThat(solicitud -> solicitud.method() == HttpMethod.POST
                && solicitud.url().toString().equals("http://notification-service/internal/notifications/password-reset")
                && "token-interno-de-prueba".equals(solicitud.headers().getFirst("X-Service-Token"))));
    }

    private void responder(String json) {
        when(transporte.exchange(any())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, "application/json").body(json).build()));
    }

    private void verificar(HttpMethod metodo, String ruta, String actor) {
        verify(transporte).exchange(argThat(solicitud -> solicitud.method() == metodo
                && solicitud.url().toString().equals("http://user-service" + ruta)
                && "token-interno-de-prueba".equals(solicitud.headers().getFirst("X-Service-Token"))
                && java.util.Objects.equals(actor, solicitud.headers().getFirst("X-Actor-Id"))));
    }
}
