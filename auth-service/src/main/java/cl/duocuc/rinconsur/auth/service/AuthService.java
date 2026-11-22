package cl.duocuc.rinconsur.auth.service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duocuc.rinconsur.auth.client.UsuarioClient;
import cl.duocuc.rinconsur.auth.data.entity.CredencialEntity;
import cl.duocuc.rinconsur.auth.data.repository.CredencialRepository;
import cl.duocuc.rinconsur.auth.dto.CredencialRequest;
import cl.duocuc.rinconsur.auth.dto.CredencialResponse;
import cl.duocuc.rinconsur.auth.dto.LoginRequest;
import cl.duocuc.rinconsur.auth.dto.LoginResponse;
import cl.duocuc.rinconsur.auth.dto.UsuarioResponse;
import cl.duocuc.rinconsur.auth.exception.ContrasenaInvalidaException;
import cl.duocuc.rinconsur.auth.exception.CredencialExistenteException;
import cl.duocuc.rinconsur.auth.exception.CredencialesInvalidasException;
import cl.duocuc.rinconsur.auth.exception.UsuarioInactivoException;
import cl.duocuc.rinconsur.auth.exception.UsuarioNoEncontradoException;

@Service
public class AuthService {

    private final CredencialRepository repositorio;
    private final UsuarioClient clienteUsuarios;
    private final PasswordEncoder codificadorContrasenas;
    private final JwtService servicioJwt;
    private final String hashSimulado;

    public AuthService(
            CredencialRepository repositorio,
            UsuarioClient clienteUsuarios,
            PasswordEncoder codificadorContrasenas,
            JwtService servicioJwt) {
        this.repositorio = repositorio;
        this.clienteUsuarios = clienteUsuarios;
        this.codificadorContrasenas = codificadorContrasenas;
        this.servicioJwt = servicioJwt;
        this.hashSimulado = generarHash(UUID.randomUUID().toString());
    }

    @Transactional
    public CredencialResponse crearCredencial(CredencialRequest datos) {
        this.validarContrasena(datos.contrasena());

        UsuarioResponse usuario = this.clienteUsuarios.obtenerPorId(datos.usuarioId());
        if (!usuario.activo()) {
            throw new UsuarioInactivoException();
        }

        if (this.repositorio.existsByUsuarioId(usuario.id())) {
            throw new CredencialExistenteException();
        }

        this.guardarCredencial(usuario.id(), datos.contrasena());
        return new CredencialResponse(usuario.id());
    }

    public void guardarCredencial(long usuarioId, String contrasena) {
        this.validarContrasenaNueva(contrasena);
        CredencialEntity credencial = new CredencialEntity(null, usuarioId, generarHash(contrasena), null);
        this.repositorio.saveAndFlush(credencial);
    }

    public void validarContrasenaNueva(String contrasena) {
        this.validarContrasena(contrasena);
        if (contrasena.isBlank() || contrasena.length() < 8) {
            throw new ContrasenaInvalidaException();
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest datos) {
        validarContrasena(datos.contrasena());
        UsuarioResponse usuario;
        try {
            usuario = clienteUsuarios.obtenerPorCorreo(datos.correo());
        } catch (UsuarioNoEncontradoException error) {
            codificadorContrasenas.matches(datos.contrasena(), hashSimulado);
            throw new CredencialesInvalidasException();
        }

        CredencialEntity credencial = repositorio.findByUsuarioId(usuario.id());
        String hash = credencial == null ? hashSimulado : credencial.getContrasenaHash();

        boolean coincide = codificadorContrasenas.matches(datos.contrasena(), hash);
        if (!coincide || credencial == null || !usuario.activo()) {
            throw new CredencialesInvalidasException();
        }

        return servicioJwt.crearToken(usuario, credencial.getVersion());
    }

    public UsuarioResponse obtenerUsuarioActual(String identificadorUsuario) {
        try {
            long id = Long.parseLong(identificadorUsuario);
            if (id <= 0) {
                throw new CredencialesInvalidasException();
            }

            UsuarioResponse usuario = clienteUsuarios.obtenerPorId(id);
            if (!usuario.activo()) {
                throw new CredencialesInvalidasException();
            }
            return usuario;
        } catch (NumberFormatException | UsuarioNoEncontradoException error) {
            throw new CredencialesInvalidasException();
        }
    }

    private String generarHash(String contrasena) {
        String hash = codificadorContrasenas.encode(contrasena);
        if (hash == null) {
            throw new IllegalStateException("No se pudo generar el hash de la contraseña");
        }
        return hash;
    }

    private void validarContrasena(String contrasena) {
        if (contrasena.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ContrasenaInvalidaException();
        }
    }
}
