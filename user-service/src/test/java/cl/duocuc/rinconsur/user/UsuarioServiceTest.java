package cl.duocuc.rinconsur.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cl.duocuc.rinconsur.user.data.entity.UsuarioEntity;
import cl.duocuc.rinconsur.user.data.repository.*;
import cl.duocuc.rinconsur.user.dto.*;
import cl.duocuc.rinconsur.user.exception.CorreoRegistradoException;
import cl.duocuc.rinconsur.user.exception.UsuarioNoEncontradoException;
import cl.duocuc.rinconsur.user.model.Rol;
import cl.duocuc.rinconsur.user.service.UsuarioService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class UsuarioServiceTest {
    private final UsuarioRepository datos = mock(UsuarioRepository.class);
    private final AuditoriaUsuarioRepository auditoria = mock(AuditoriaUsuarioRepository.class);
    private final UsuarioService servicio = new UsuarioService(datos, auditoria);
    private final UsuarioEntity usuario = new UsuarioEntity(1L, "Ana", "ana@example.test", Rol.OBSERVADOR,
        true, "", "", null, 0L);

    @BeforeEach
    void preparar() {
        when(datos.findById(1L)).thenReturn(Optional.of(usuario));
        when(datos.saveAndFlush(any(UsuarioEntity.class))).thenAnswer(invocacion -> {
            UsuarioEntity guardado = invocacion.getArgument(0);
            if (guardado.getId() == null) guardado.setId(2L);
            return guardado;
        });
    }

    @Test
    void creaConCorreoNormalizadoYRegistraAuditoria() {
        var cuenta = servicio.crearUsuario(new UsuarioRequest(" Pedro ", " Pedro@Example.Test ", Rol.SUPERVISOR, null), 1L);
        assertEquals("pedro@example.test", cuenta.correo());
        assertEquals("Pedro", cuenta.nombre());
        assertTrue(cuenta.activo());
        verify(auditoria).save(argThat(e -> e.getAccion().equals("USUARIO_CREADO")));
    }

    @Test
    void bloqueaCorreosDuplicados() {
        when(datos.existsByCorreo("ana@example.test")).thenReturn(true);
        assertThrows(CorreoRegistradoException.class,
            () -> servicio.crearUsuario(new UsuarioRequest("Ana", "ANA@example.test", Rol.OBSERVADOR, true), null));
    }

    @Test
    void permiteEditarYDesactivar() {
        assertEquals(Rol.SUPERVISOR, servicio.actualizarUsuario(1,
            new UsuarioRequest("Ana", "ana@example.test", Rol.SUPERVISOR, null), 1L).rol());
        assertFalse(servicio.cambiarEstado(1, false, 1L).activo());
        assertFalse(servicio.cambiarEstado(1, false, 1L).activo());
        assertEquals("Ana", servicio.obtenerUsuario(1).nombre());
    }

    @Test
    void validaElFormatoDeLaFoto() {
        assertThrows(IllegalArgumentException.class, () -> servicio.actualizarPerfil(1,
            new PerfilRequest("Ana", "", "ana@example.test", "", "no-es-jpeg")));
        assertEquals("Pérez", servicio.actualizarPerfil(1,
            new PerfilRequest("Ana", "Pérez", "ana@example.test", "+56912345678", "/9j//9k=")).apellido());
        assertEquals("/9j//9k=", servicio.perfil(1).foto());
    }

    @Test
    void paginaYBuscaUsuarios() {
        when(datos.findByCorreo("ana@example.test")).thenReturn(usuario);
        when(datos.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(usuario)));
        assertEquals(1, servicio.obtenerUsuarios(0, 10).totalUsuarios());
        assertEquals(1, servicio.obtenerUsuarioPorCorreo("ANA@example.test").id());
        assertThrows(UsuarioNoEncontradoException.class, () -> servicio.obtenerUsuarioPorCorreo("nadie@example.test"));
    }
}
