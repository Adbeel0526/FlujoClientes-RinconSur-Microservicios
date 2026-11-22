package cl.duocuc.rinconsur.auth.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import cl.duocuc.rinconsur.auth.client.UsuarioClient;
import cl.duocuc.rinconsur.auth.dto.ActividadCuenta;
import cl.duocuc.rinconsur.auth.dto.PaginaCuentas;
import cl.duocuc.rinconsur.auth.dto.PerfilCuenta;
import cl.duocuc.rinconsur.auth.dto.PerfilDatos;
import cl.duocuc.rinconsur.auth.dto.RolCuenta;
import cl.duocuc.rinconsur.auth.dto.UsuarioDatos;
import cl.duocuc.rinconsur.auth.dto.UsuarioInterno;
import cl.duocuc.rinconsur.auth.dto.UsuarioResponse;
import cl.duocuc.rinconsur.auth.exception.ErrorCuenta;

@Service
public class CuentasService {

    private final AuthService autenticacion;
    private final UsuarioClient usuarios;

    public CuentasService(AuthService autenticacion, UsuarioClient usuarios) {
        this.autenticacion = autenticacion;
        this.usuarios = usuarios;
    }

    public PaginaCuentas listar(String identificadorUsuario, int pagina) {
        this.administrador(identificadorUsuario);
        if (pagina < 0) {
            throw new ErrorCuenta(HttpStatus.BAD_REQUEST, "La página debe ser positiva o cero");
        }

        return this.usuarios.listar(pagina);
    }

    public UsuarioResponse crear(String identificadorUsuario, UsuarioDatos datos) {
        long actor = this.administrador(identificadorUsuario);
        String contrasena = datos.contrasena();

        if (contrasena == null) {
            throw new ErrorCuenta(HttpStatus.BAD_REQUEST, "Indica una contraseña inicial");
        }

        this.autenticacion.validarContrasenaNueva(contrasena);
        UsuarioInterno solicitud = new UsuarioInterno(datos.nombre(), datos.correo(), datos.rol(), false);
        UsuarioResponse creado = this.usuarios.crear(solicitud, actor);

        this.autenticacion.guardarCredencial(creado.id(), contrasena);
        return this.usuarios.estado(creado.id(), true, actor);
    }

    public UsuarioResponse editar(String identificadorUsuario, long id, UsuarioDatos datos) {
        long actor = this.administrador(identificadorUsuario);
        if (actor == id && datos.rol() != RolCuenta.ADMINISTRADOR) {
            throw new ErrorCuenta(HttpStatus.CONFLICT, "No puedes quitar tu propio rol de administrador");
        }

        UsuarioInterno solicitud = new UsuarioInterno(datos.nombre(), datos.correo(), datos.rol(), null);
        return this.usuarios.editar(id, solicitud, actor);
    }

    public UsuarioResponse estado(String identificadorUsuario, long id, boolean activo) {
        long actor = this.administrador(identificadorUsuario);
        if (actor == id && !activo) {
            throw new ErrorCuenta(HttpStatus.CONFLICT, "No puedes desactivar tu cuenta");
        }

        return this.usuarios.estado(id, activo, actor);
    }

    public List<ActividadCuenta> actividad(String identificadorUsuario) {
        this.administrador(identificadorUsuario);
        return this.usuarios.actividad();
    }

    public PerfilCuenta perfil(String identificadorUsuario) {
        return this.usuarios.perfil(this.autenticacion.obtenerUsuarioActual(identificadorUsuario).id());
    }

    public PerfilCuenta editarPerfil(String identificadorUsuario, PerfilDatos datos) {
        return this.usuarios.editarPerfil(this.autenticacion.obtenerUsuarioActual(identificadorUsuario).id(), datos);
    }

    private long administrador(String identificadorUsuario) {
        UsuarioResponse actor = this.autenticacion.obtenerUsuarioActual(identificadorUsuario);
        if (!"ADMINISTRADOR".equals(actor.rol())) {
            throw new ErrorCuenta(HttpStatus.FORBIDDEN, "Esta operación requiere el rol de administrador");
        }

        return actor.id();
    }
}
