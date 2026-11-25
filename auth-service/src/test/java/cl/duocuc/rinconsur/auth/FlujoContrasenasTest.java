package cl.duocuc.rinconsur.auth;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import cl.duocuc.rinconsur.auth.client.CorreoClient;
import cl.duocuc.rinconsur.auth.client.UsuarioClient;
import cl.duocuc.rinconsur.auth.controller.ContrasenasController;
import cl.duocuc.rinconsur.auth.dto.*;
import cl.duocuc.rinconsur.auth.exception.UsuarioNoEncontradoException;
import cl.duocuc.rinconsur.auth.service.*;

class FlujoContrasenasTest {
    private static final String DIRECCION = "eva@example.org";
    private static final String CODIGO = "ab".repeat(32);
    private final AuthService auth = mock(AuthService.class);
    private final UsuarioClient usuarios = mock(UsuarioClient.class);
    private final CorreoClient correo = mock(CorreoClient.class);
    private final CambiosContrasena cambios = mock(CambiosContrasena.class);
    private final ContrasenasController controller =
            new ContrasenasController(new ContrasenasService(auth, usuarios, correo, cambios));

    @Test
    void enviaElCodigoSoloCuandoSeEmiteParaUnaCuentaActiva() {
        when(usuarios.obtenerPorCorreo(DIRECCION)).thenReturn(usuario(true));
        when(cambios.emitir(7)).thenReturn(CODIGO).thenReturn(null);

        controller.solicitar(new SolicitudRecuperacion(DIRECCION));
        controller.solicitar(new SolicitudRecuperacion(DIRECCION));

        verify(correo).enviar(DIRECCION, CODIGO);
        verifyNoMoreInteractions(correo);
    }

    @Test
    void omiteElEnvioParaCuentasInactivasOInexistentes() {
        when(usuarios.obtenerPorCorreo(DIRECCION)).thenReturn(usuario(false))
                .thenThrow(new UsuarioNoEncontradoException());

        controller.solicitar(new SolicitudRecuperacion(DIRECCION));
        controller.solicitar(new SolicitudRecuperacion(DIRECCION));

        verifyNoInteractions(cambios, correo);
    }

    @Test
    void validaLaContrasenaAntesDeCambiarlaORecuperarla() {
        var jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject("7").build();
        when(auth.obtenerUsuarioActual("7")).thenReturn(usuario(true));
        var cambio = new CambioContrasena("Anterior123", "Nueva123");
        var recuperacion = new RestablecerContrasena(CODIGO, "Recuperada123");

        controller.cambiar(jwt, cambio);
        controller.restablecer(recuperacion);

        var orden = inOrder(auth, cambios);
        orden.verify(auth).validarContrasenaNueva(cambio.nueva());
        orden.verify(cambios).cambiar(7, cambio);
        orden.verify(auth).validarContrasenaNueva(recuperacion.nueva());
        orden.verify(cambios).restablecer(recuperacion);
    }

    private UsuarioResponse usuario(boolean activo) {
        return new UsuarioResponse(7, "Eva", DIRECCION, "OBSERVADOR", activo);
    }
}
