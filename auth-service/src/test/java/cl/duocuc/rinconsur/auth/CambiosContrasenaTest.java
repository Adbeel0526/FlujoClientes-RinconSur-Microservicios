package cl.duocuc.rinconsur.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cl.duocuc.rinconsur.auth.data.entity.CredencialEntity;
import cl.duocuc.rinconsur.auth.data.entity.RecuperacionEntity;
import cl.duocuc.rinconsur.auth.data.repository.CredencialRepository;
import cl.duocuc.rinconsur.auth.data.repository.RecuperacionRepository;
import cl.duocuc.rinconsur.auth.dto.CambioContrasena;
import cl.duocuc.rinconsur.auth.dto.RestablecerContrasena;
import cl.duocuc.rinconsur.auth.exception.ErrorCuenta;
import cl.duocuc.rinconsur.auth.service.AuthService;
import cl.duocuc.rinconsur.auth.service.CambiosContrasena;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class CambiosContrasenaTest {
    private final AuthService autenticacion = mock(AuthService.class);
    private final CredencialRepository credenciales = mock(CredencialRepository.class);
    private final RecuperacionRepository recuperaciones = mock(RecuperacionRepository.class);
    private final PasswordEncoder codificadorContrasenas = mock(PasswordEncoder.class);
    private final CambiosContrasena cambios = new CambiosContrasena(autenticacion, credenciales, recuperaciones, codificadorContrasenas);
    private final CredencialEntity credencial = new CredencialEntity(10L, 1, "hash-viejo", 0L);

    @BeforeEach
    void preparar() {
        when(credenciales.bloquear(1)).thenReturn(credencial);
        when(codificadorContrasenas.encode("ClaveNueva123!")).thenReturn("hash-nuevo");
    }

    @Test
    void validaContrasenaActualAntesDeCambiar() {
        assertThrows(ErrorCuenta.class, () -> cambios.cambiar(1, new CambioContrasena("incorrecta", "ClaveNueva123!")));
        assertEquals("hash-viejo", credencial.getContrasenaHash());
        when(codificadorContrasenas.matches("correcta", "hash-viejo")).thenReturn(true);
        cambios.cambiar(1, new CambioContrasena("correcta", "ClaveNueva123!"));
        assertEquals("hash-nuevo", credencial.getContrasenaHash());
        verify(recuperaciones).deleteById(1L);
    }

    @Test
    void recuperacionGuardaHuellaYCaducaDespuesDeQuinceMinutos() {
        String codigo = cambios.emitir(1);
        assertTrue(codigo.matches("[a-f0-9]{64}"));
        var captura = ArgumentCaptor.forClass(RecuperacionEntity.class);
        verify(recuperaciones).save(captura.capture());
        RecuperacionEntity solicitud = captura.getValue();
        assertNotEquals(codigo, solicitud.getHuella());
        when(recuperaciones.findById(1L)).thenReturn(Optional.of(solicitud));
        assertNull(cambios.emitir(1));

        when(recuperaciones.usuarioPorHuella(solicitud.getHuella())).thenReturn(1L);
        solicitud.setEmitida(Instant.now().minusSeconds(901));
        assertThrows(ErrorCuenta.class, () -> cambios.restablecer(new RestablecerContrasena(codigo, "ClaveNueva123!")));
        solicitud.setEmitida(Instant.now());
        cambios.restablecer(new RestablecerContrasena(codigo, "ClaveNueva123!"));
        assertEquals("hash-nuevo", credencial.getContrasenaHash());
        verify(recuperaciones).delete(solicitud);
    }
}
