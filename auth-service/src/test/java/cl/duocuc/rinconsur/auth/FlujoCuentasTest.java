package cl.duocuc.rinconsur.auth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import cl.duocuc.rinconsur.auth.client.UsuarioClient;
import cl.duocuc.rinconsur.auth.controller.CuentasController;
import cl.duocuc.rinconsur.auth.dto.*;
import cl.duocuc.rinconsur.auth.exception.ErrorCuenta;
import cl.duocuc.rinconsur.auth.service.AuthService;
import cl.duocuc.rinconsur.auth.service.CuentasService;

class FlujoCuentasTest {
    private static final UsuarioResponse ADMIN = new UsuarioResponse(1, "Ana", "ana@example.org", "ADMINISTRADOR", true);
    private static final UsuarioResponse NUEVA = new UsuarioResponse(2, "Eva", "eva@example.org", "OBSERVADOR", true);
    private static final UsuarioDatos DATOS = new UsuarioDatos("Eva", "eva@example.org", RolCuenta.OBSERVADOR, "ClaveSegura1");
    private final AuthService auth = mock(AuthService.class);
    private final UsuarioClient usuarios = mock(UsuarioClient.class);
    private final CuentasController controller = new CuentasController(new CuentasService(auth, usuarios));
    private final Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject("1").build();

    @Test
    void guardaLaCredencialAntesDeActivarLaCuenta() {
        when(auth.obtenerUsuarioActual("1")).thenReturn(ADMIN);
        var inactiva = new UsuarioResponse(2, DATOS.nombre(), DATOS.correo(), "OBSERVADOR", false);
        var solicitud = new UsuarioInterno(DATOS.nombre(), DATOS.correo(), DATOS.rol(), false);
        when(usuarios.crear(solicitud, 1)).thenReturn(inactiva);
        when(usuarios.estado(2, true, 1)).thenReturn(NUEVA);

        assertThat(controller.crear(jwt, DATOS)).isEqualTo(NUEVA);

        var orden = inOrder(auth, usuarios);
        orden.verify(auth).validarContrasenaNueva(DATOS.contrasena());
        orden.verify(usuarios).crear(solicitud, 1);
        orden.verify(auth).guardarCredencial(2, DATOS.contrasena());
        orden.verify(usuarios).estado(2, true, 1);
    }

    @Test
    void impideAdministrarSinPermisosYProtegeLaCuentaPropia() {
        when(auth.obtenerUsuarioActual("1")).thenReturn(NUEVA, ADMIN);
        assertThatExceptionOfType(ErrorCuenta.class).isThrownBy(() -> controller.listar(jwt, 0))
                .satisfies(error -> assertThat(error.getEstado()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatExceptionOfType(ErrorCuenta.class).isThrownBy(() -> controller.editar(jwt, 1, DATOS))
                .satisfies(error -> assertThat(error.getEstado()).isEqualTo(HttpStatus.CONFLICT));
        assertThatExceptionOfType(ErrorCuenta.class).isThrownBy(() -> controller.estado(jwt, 1, new EstadoCuenta(false)))
                .satisfies(error -> assertThat(error.getEstado()).isEqualTo(HttpStatus.CONFLICT));
        assertThatIllegalArgumentException().isThrownBy(() -> controller.estado(jwt, 2, new EstadoCuenta(null)));
        verifyNoInteractions(usuarios);
    }

    @Test
    void administraOtrasCuentasConLaIdentidadDelActor() {
        when(auth.obtenerUsuarioActual("1")).thenReturn(ADMIN);
        var pagina = new PaginaCuentas(List.of(NUEVA), 1, 2, 21);
        when(usuarios.listar(1)).thenReturn(pagina);
        when(usuarios.actividad()).thenReturn(List.of());
        when(usuarios.editar(2, new UsuarioInterno(DATOS.nombre(), DATOS.correo(), DATOS.rol(), null), 1))
                .thenReturn(NUEVA);
        var inactiva = new UsuarioResponse(2, DATOS.nombre(), DATOS.correo(), "OBSERVADOR", false);
        when(usuarios.estado(2, false, 1)).thenReturn(inactiva);

        assertThat(controller.listar(jwt, 1)).isEqualTo(pagina);
        assertThat(controller.actividad(jwt)).isEmpty();
        assertThat(controller.editar(jwt, 2, DATOS)).isEqualTo(NUEVA);
        assertThat(controller.estado(jwt, 2, new EstadoCuenta(false))).isEqualTo(inactiva);
        verify(usuarios).actividad();
    }

    @Test
    void elPerfilSoloUsaLaIdentidadAutenticadaYNoSeAlmacenaEnCache() {
        when(auth.obtenerUsuarioActual("1")).thenReturn(NUEVA);
        var datos = new PerfilDatos("Eva", null, DATOS.correo(), null, null);
        var perfil = new PerfilCuenta(2, "Eva", "", DATOS.correo(), "", RolCuenta.OBSERVADOR, true, null);
        when(usuarios.perfil(2)).thenReturn(perfil);
        when(usuarios.editarPerfil(2, datos)).thenReturn(perfil);

        var consulta = controller.perfil(jwt);
        var edicion = controller.editarPerfil(jwt, datos);
        assertThat(consulta.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(edicion.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(consulta.getBody()).isEqualTo(perfil);
        assertThat(edicion.getBody()).isEqualTo(perfil);
    }
}
