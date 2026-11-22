package cl.duocuc.rinconsur.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cl.duocuc.rinconsur.auth.client.UsuarioClient;
import cl.duocuc.rinconsur.auth.data.entity.CredencialEntity;
import cl.duocuc.rinconsur.auth.data.repository.CredencialRepository;
import cl.duocuc.rinconsur.auth.dto.*;
import cl.duocuc.rinconsur.auth.exception.*;
import cl.duocuc.rinconsur.auth.service.AuthService;
import cl.duocuc.rinconsur.auth.service.CuentasService;
import cl.duocuc.rinconsur.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {
    private final CredencialRepository datos = mock(CredencialRepository.class);
    private final UsuarioClient usuarios = mock(UsuarioClient.class);
    private final PasswordEncoder codificadorContrasenas = mock(PasswordEncoder.class);
    private final JwtService jwt = mock(JwtService.class);
    private final UsuarioResponse admin = new UsuarioResponse(1, "Admin", "admin@example.test", "ADMINISTRADOR", true);
    private final String clave = "Prueba123!";
    private AuthService autenticacion;
    private CuentasService cuentas;

    @BeforeEach
    void preparar() {
        when(codificadorContrasenas.encode(anyString())).thenReturn("hash");
        autenticacion = new AuthService(datos, usuarios, codificadorContrasenas, jwt);
        cuentas = new CuentasService(autenticacion, usuarios);
        when(usuarios.obtenerPorId(1L)).thenReturn(admin);
        when(usuarios.obtenerPorCorreo(admin.correo())).thenReturn(admin);
    }

    @Test
    void autenticaSoloConHashCorrectoYVersionVigente() {
        when(datos.findByUsuarioId(1)).thenReturn(new CredencialEntity(10L, 1, "almacenado", 2L));
        when(codificadorContrasenas.matches(clave, "almacenado")).thenReturn(true);
        var esperado = new LoginResponse("token-prueba", 900, admin);
        when(jwt.crearToken(admin, 2L)).thenReturn(esperado);
        assertEquals(esperado, autenticacion.login(new LoginRequest(admin.correo(), clave)));
        assertThrows(CredencialesInvalidasException.class, () -> autenticacion.login(new LoginRequest(admin.correo(), "otra")));
    }

    @Test
    void rechazaCorreoDesconocidoYSesionDeUsuarioInactivo() {
        when(usuarios.obtenerPorCorreo("otro@example.test")).thenThrow(new UsuarioNoEncontradoException());
        assertThrows(CredencialesInvalidasException.class, () -> autenticacion.login(new LoginRequest("otro@example.test", clave)));
        verify(codificadorContrasenas).matches(clave, "hash");
        when(usuarios.obtenerPorId(2L)).thenReturn(new UsuarioResponse(2, "Usuario", "otro@example.test", "OBSERVADOR", false));
        assertThrows(CredencialesInvalidasException.class, () -> autenticacion.obtenerUsuarioActual("2"));
        assertThrows(CredencialesInvalidasException.class, () -> autenticacion.obtenerUsuarioActual("incorrecto"));
    }

    @Test
    void guardaCredencialValidaYTieneEnCuentaElLimiteUtf8() {
        assertEquals(1, autenticacion.crearCredencial(new CredencialRequest(1, clave)).usuarioId());
        verify(datos).saveAndFlush(argThat(c -> c.getUsuarioId() == 1 && c.getContrasenaHash().equals("hash")));
        assertThrows(ContrasenaInvalidaException.class, () -> autenticacion.validarContrasenaNueva("ñ".repeat(37)));
        assertThrows(ContrasenaInvalidaException.class, () -> autenticacion.validarContrasenaNueva("       "));
        when(datos.existsByUsuarioId(1)).thenReturn(true);
        assertThrows(CredencialExistenteException.class, () -> autenticacion.crearCredencial(new CredencialRequest(1, clave)));
    }

    @Test
    void impideQueElAdministradorDesactiveSuPropiaCuentaOQuiteSuRol() {
        assertEquals(HttpStatus.CONFLICT, assertThrows(ErrorCuenta.class, () -> cuentas.estado("1", 1, false)).getEstado());
        assertEquals(HttpStatus.CONFLICT, assertThrows(ErrorCuenta.class, () ->
            cuentas.editar("1", 1, new UsuarioDatos("Admin", admin.correo(), RolCuenta.OBSERVADOR, null))).getEstado());
    }

    @Test
    void activaNuevoUsuarioDespuesDeGuardarCredencial() {
        var inactivo = new UsuarioResponse(2, "Ana", "ana@example.test", "OBSERVADOR", false);
        var activo = new UsuarioResponse(2, "Ana", "ana@example.test", "OBSERVADOR", true);
        var interno = new UsuarioInterno("Ana", "ana@example.test", RolCuenta.OBSERVADOR, false);
        when(usuarios.crear(interno, 1)).thenReturn(inactivo);
        when(usuarios.estado(2, true, 1)).thenReturn(activo);
        assertTrue(cuentas.crear("1", new UsuarioDatos("Ana", "ana@example.test", RolCuenta.OBSERVADOR, clave)).activo());
        var orden = inOrder(usuarios, datos);
        orden.verify(usuarios).crear(interno, 1);
        orden.verify(datos).saveAndFlush(any(CredencialEntity.class));
        orden.verify(usuarios).estado(2, true, 1);
    }
}
