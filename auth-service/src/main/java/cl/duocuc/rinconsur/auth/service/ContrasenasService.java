package cl.duocuc.rinconsur.auth.service;

import org.springframework.stereotype.Service;

import cl.duocuc.rinconsur.auth.client.CorreoClient;
import cl.duocuc.rinconsur.auth.client.UsuarioClient;
import cl.duocuc.rinconsur.auth.dto.CambioContrasena;
import cl.duocuc.rinconsur.auth.dto.RestablecerContrasena;
import cl.duocuc.rinconsur.auth.dto.UsuarioResponse;
import cl.duocuc.rinconsur.auth.exception.UsuarioNoEncontradoException;

@Service
public class ContrasenasService {

    private final AuthService autenticacion;
    private final UsuarioClient usuarios;
    private final CorreoClient correo;
    private final CambiosContrasena cambios;

    public ContrasenasService(
            AuthService autenticacion,
            UsuarioClient usuarios,
            CorreoClient correo,
            CambiosContrasena cambios) {
        this.autenticacion = autenticacion;
        this.usuarios = usuarios;
        this.correo = correo;
        this.cambios = cambios;
    }

    public void solicitar(String direccion) {
        UsuarioResponse usuario;
        try {
            usuario = this.usuarios.obtenerPorCorreo(direccion);
        } catch (UsuarioNoEncontradoException error) {
            return;
        }

        if (!usuario.activo()) {
            return;
        }

        String codigo = cambios.emitir(usuario.id());
        if (codigo != null) {
            correo.enviar(usuario.correo(), codigo);
        }
    }

    public void actualizar(String identificadorUsuario, CambioContrasena datos) {
        UsuarioResponse usuario = this.autenticacion.obtenerUsuarioActual(identificadorUsuario);
        this.autenticacion.validarContrasenaNueva(datos.nueva());
        this.cambios.cambiar(usuario.id(), datos);
    }

    public void restablecer(RestablecerContrasena datos) {
        this.autenticacion.validarContrasenaNueva(datos.nueva());
        this.cambios.restablecer(datos);
    }
}
