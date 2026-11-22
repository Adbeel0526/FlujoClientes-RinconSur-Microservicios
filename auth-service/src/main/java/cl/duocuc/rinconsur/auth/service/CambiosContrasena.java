package cl.duocuc.rinconsur.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duocuc.rinconsur.auth.data.entity.CredencialEntity;
import cl.duocuc.rinconsur.auth.data.entity.RecuperacionEntity;
import cl.duocuc.rinconsur.auth.data.repository.CredencialRepository;
import cl.duocuc.rinconsur.auth.data.repository.RecuperacionRepository;
import cl.duocuc.rinconsur.auth.dto.CambioContrasena;
import cl.duocuc.rinconsur.auth.dto.RestablecerContrasena;
import cl.duocuc.rinconsur.auth.exception.ErrorCuenta;

@Service
public class CambiosContrasena {

    private final AuthService autenticacion;
    private final CredencialRepository credenciales;
    private final RecuperacionRepository recuperaciones;
    private final PasswordEncoder codificadorContrasenas;
    private final SecureRandom aleatorio = new SecureRandom();
    private final HexFormat formato = HexFormat.of();

    public CambiosContrasena(
            AuthService autenticacion,
            CredencialRepository credenciales,
            RecuperacionRepository recuperaciones,
            PasswordEncoder codificadorContrasenas) {
        this.autenticacion = autenticacion;
        this.credenciales = credenciales;
        this.recuperaciones = recuperaciones;
        this.codificadorContrasenas = codificadorContrasenas;
    }

    @Transactional
    public String emitir(long usuarioId) {
        if (credenciales.bloquear(usuarioId) == null) {
            return null;
        }

        Instant ahora = Instant.now();
        RecuperacionEntity anterior = recuperaciones.findById(usuarioId).orElse(null);
        if (anterior != null && anterior.getEmitida().plusSeconds(60).isAfter(ahora)) {
            return null;
        }

        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String codigo = formato.formatHex(bytes);
        recuperaciones.save(new RecuperacionEntity(usuarioId, huella(codigo), ahora));
        return codigo;
    }

    @Transactional
    public void cambiar(long usuarioId, CambioContrasena datos) {
        CredencialEntity credencial = credenciales.bloquear(usuarioId);
        if (credencial == null) {
            throw invalido();
        }

        if (datos.actual().getBytes(StandardCharsets.UTF_8).length > 72
                || !codificadorContrasenas.matches(datos.actual(), credencial.getContrasenaHash())) {
            throw new ErrorCuenta(HttpStatus.BAD_REQUEST, "La contraseña actual es incorrecta");
        }

        credencial.setContrasenaHash(Objects.requireNonNull(codificadorContrasenas.encode(datos.nueva())));
        recuperaciones.deleteById(usuarioId);
    }

    @Transactional
    public void restablecer(RestablecerContrasena datos) {
        String huella = huella(datos.codigo());
        Long id = recuperaciones.usuarioPorHuella(huella);
        if (id == null) {
            throw invalido();
        }

        CredencialEntity credencial = credenciales.bloquear(id);
        if (credencial == null) {
            throw invalido();
        }

        RecuperacionEntity recuperacion = recuperaciones
                .findById(id)
                .orElseThrow(this::invalido);
        if (!recuperacion.getHuella().equals(huella)
                || !recuperacion.getEmitida().plusSeconds(900).isAfter(Instant.now())) {
            throw invalido();
        }

        autenticacion.obtenerUsuarioActual(id.toString());
        credencial.setContrasenaHash(Objects.requireNonNull(codificadorContrasenas.encode(datos.nueva())));
        recuperaciones.delete(recuperacion);
    }

    private String huella(String codigo) {
        try {
            return formato
                    .formatHex(MessageDigest
                            .getInstance("SHA-256")
                            .digest(codigo.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 no disponible", error);
        }
    }

    private ErrorCuenta invalido() {
        return new ErrorCuenta(HttpStatus.BAD_REQUEST, "El código es inválido o ya caducó");
    }
}
