package cl.duocuc.rinconsur.operations;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

class IdentidadTest {
    private static final String EVA = """
            {"id":7,"nombre":"Eva","rol":"OBSERVADOR","activo":true}
            """;
    private static final String ANA_INACTIVA = """
            {"id":8,"nombre":"Ana","rol":"OBSERVADOR","activo":false}
            """;
    private final ExchangeFunction transporte = mock(ExchangeFunction.class);
    private final Identidad identidad = new Identidad(WebClient.builder().exchangeFunction(transporte), "token-interno");

    @Test
    void reenviaLaAutorizacionSoloAlServicioDeAutenticacion() {
        when(transporte.exchange(any())).thenReturn(respuesta(EVA));

        assertThat(identidad.autenticar("Bearer jwt-prueba")).isEqualTo(new Usuario(7, "Eva", Rol.OBSERVADOR, true));
        verify(transporte).exchange(argThat(solicitud -> solicitud.url().toString()
                .equals("http://auth-service/api/v1/auth/me")
                && "Bearer jwt-prueba".equals(solicitud.headers().getFirst(HttpHeaders.AUTHORIZATION))
                && solicitud.headers().getFirst("X-Service-Token") == null));
    }

    @Test
    void distingueUnaSesionInvalidaDeUnaFallaDeServicio() {
        comprobarError(HttpStatus.UNAUTHORIZED, () -> identidad.autenticar(" "));
        verifyNoInteractions(transporte);

        when(transporte.exchange(any())).thenReturn(respuesta(ANA_INACTIVA), estado(HttpStatus.UNAUTHORIZED),
                estado(HttpStatus.SERVICE_UNAVAILABLE), estado(HttpStatus.NO_CONTENT));
        comprobarError(HttpStatus.UNAUTHORIZED, () -> identidad.autenticar("Bearer inactivo"));
        comprobarError(HttpStatus.UNAUTHORIZED, () -> identidad.autenticar("Bearer invalido"));
        comprobarError(HttpStatus.SERVICE_UNAVAILABLE, () -> identidad.autenticar("Bearer valido"));
        comprobarError(HttpStatus.SERVICE_UNAVAILABLE, () -> identidad.autenticar("Bearer valido"));
    }

    @Test
    void exigeQueLaCuentaAsignadaTengaRolDeObservadorYEsteActiva() {
        when(transporte.exchange(any())).thenReturn(respuesta(EVA), respuesta(EVA.replace("OBSERVADOR", "SUPERVISOR")),
                respuesta(ANA_INACTIVA), estado(HttpStatus.NOT_FOUND), estado(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThat(identidad.observador(7).nombre()).isEqualTo("Eva");
        comprobarError(HttpStatus.BAD_REQUEST, () -> identidad.observador(7));
        comprobarError(HttpStatus.BAD_REQUEST, () -> identidad.observador(8));
        comprobarError(HttpStatus.BAD_REQUEST, () -> identidad.observador(99));
        comprobarError(HttpStatus.SERVICE_UNAVAILABLE, () -> identidad.observador(7));
        verify(transporte, times(3)).exchange(argThat(solicitud -> solicitud.url().toString()
                .equals("http://user-service/internal/users/7")
                && "token-interno".equals(solicitud.headers().getFirst("X-Service-Token"))));
    }

    @Test
    void recorreTodasLasPaginasYDescartaLasCuentasInactivas() {
        when(transporte.exchange(any())).thenReturn(
                respuesta("{\"usuarios\":[" + ANA_INACTIVA + "],\"totalPaginas\":2}"),
                respuesta("{\"usuarios\":[" + EVA + "],\"totalPaginas\":2}"));

        assertThat(identidad.usuariosActivos()).extracting(Usuario::id).containsExactly(7L);
        var orden = inOrder(transporte);
        orden.verify(transporte).exchange(argThat(solicitud -> solicitud.url().getQuery().equals("pagina=0&cantidad=100")));
        orden.verify(transporte).exchange(argThat(solicitud -> solicitud.url().getQuery().equals("pagina=1&cantidad=100")));
    }

    private static Mono<ClientResponse> respuesta(String json) {
        return Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, "application/json").body(json).build());
    }

    private static Mono<ClientResponse> estado(HttpStatus estado) {
        return Mono.just(ClientResponse.create(estado).build());
    }

    private static void comprobarError(HttpStatus estado, org.assertj.core.api.ThrowableAssert.ThrowingCallable accion) {
        assertThatThrownBy(accion).isInstanceOfSatisfying(ErrorOperacion.class,
                error -> assertThat(error.getEstado()).isEqualTo(estado));
    }
}
