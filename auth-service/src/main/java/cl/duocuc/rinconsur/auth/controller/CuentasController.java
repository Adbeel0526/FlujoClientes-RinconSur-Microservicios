package cl.duocuc.rinconsur.auth.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duocuc.rinconsur.auth.dto.ActividadCuenta;
import cl.duocuc.rinconsur.auth.dto.EstadoCuenta;
import cl.duocuc.rinconsur.auth.dto.PaginaCuentas;
import cl.duocuc.rinconsur.auth.dto.PerfilCuenta;
import cl.duocuc.rinconsur.auth.dto.PerfilDatos;
import cl.duocuc.rinconsur.auth.dto.UsuarioDatos;
import cl.duocuc.rinconsur.auth.dto.UsuarioResponse;
import cl.duocuc.rinconsur.auth.service.CuentasService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@Tag(name = "Cuentas y perfil", description = "Administración de cuentas y consulta o edición del perfil propio.")
@RestController
@RequestMapping("/api/v1/auth")
public class CuentasController {

    private final CuentasService cuentas;

    public CuentasController(CuentasService cuentas) {
        this.cuentas = cuentas;
    }

    @Operation(
            summary = "Listar cuentas",
            description = "Muestra una página de cuentas para su administración.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de cuentas disponible."),
            @ApiResponse(responseCode = "400", description = "El número de página es inválido."),
            @ApiResponse(responseCode = "401", description = "Sesión ausente o inválida."),
            @ApiResponse(responseCode = "403", description = "Se requiere el rol de administrador."),
            @ApiResponse(responseCode = "503", description = "El servicio de usuarios no está disponible.")
    })
    @GetMapping("/admin/usuarios")
    public PaginaCuentas listar(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Número de página, empezando en cero.")
            @RequestParam(defaultValue = "0") int pagina) {
        return this.cuentas.listar(jwt.getSubject(), pagina);
    }

    @Operation(
            summary = "Consultar actividad de cuentas",
            description = "Muestra los cambios recientes en las cuentas de usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actividad disponible."),
            @ApiResponse(responseCode = "401", description = "Sesión ausente o inválida."),
            @ApiResponse(responseCode = "403", description = "Se requiere el rol de administrador."),
            @ApiResponse(responseCode = "503", description = "El servicio de usuarios no está disponible.")
    })
    @GetMapping("/admin/usuarios/auditoria")
    public List<ActividadCuenta> actividad(@AuthenticationPrincipal Jwt jwt) {
        return this.cuentas.actividad(jwt.getSubject());
    }

    @Operation(
            summary = "Crear cuenta",
            description = "Crea una cuenta, registra su contraseña inicial y la activa.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuenta creada y activada."),
            @ApiResponse(responseCode = "400", description = "Los datos de la solicitud son inválidos."),
            @ApiResponse(responseCode = "401", description = "Sesión ausente o inválida."),
            @ApiResponse(responseCode = "403", description = "Se requiere el rol de administrador."),
            @ApiResponse(responseCode = "409", description = "El correo o la credencial ya están registrados, o la cuenta cambió."),
            @ApiResponse(responseCode = "503", description = "El servicio de usuarios no está disponible.")
    })
    @PostMapping("/admin/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UsuarioDatos datos) {
        return this.cuentas.crear(jwt.getSubject(), datos);
    }

    @Operation(
            summary = "Editar cuenta",
            description = "Actualiza el nombre, el correo y el rol de una cuenta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta actualizada."),
            @ApiResponse(responseCode = "400", description = "Los datos de la solicitud son inválidos."),
            @ApiResponse(responseCode = "401", description = "Sesión ausente o inválida."),
            @ApiResponse(responseCode = "403", description = "Se requiere el rol de administrador."),
            @ApiResponse(responseCode = "404", description = "No se encontró el usuario."),
            @ApiResponse(responseCode = "409", description = "El correo ya está en uso, la cuenta cambió o se intentó quitar el rol propio de administrador."),
            @ApiResponse(responseCode = "503", description = "El servicio de usuarios no está disponible.")
    })
    @PutMapping("/admin/usuarios/{id}")
    public UsuarioResponse editar(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Identificador de la cuenta que se quiere modificar.")
            @PathVariable long id,
            @Valid @RequestBody UsuarioDatos datos) {
        return this.cuentas.editar(jwt.getSubject(), id, datos);
    }

    @Operation(
            summary = "Cambiar estado de cuenta",
            description = "Activa o desactiva la cuenta indicada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado de la cuenta actualizado."),
            @ApiResponse(responseCode = "400", description = "Los datos de la solicitud son inválidos."),
            @ApiResponse(responseCode = "401", description = "Sesión ausente o inválida."),
            @ApiResponse(responseCode = "403", description = "Se requiere el rol de administrador."),
            @ApiResponse(responseCode = "404", description = "No se encontró el usuario."),
            @ApiResponse(responseCode = "409", description = "La cuenta cambió o se intentó desactivar la cuenta propia."),
            @ApiResponse(responseCode = "503", description = "El servicio de usuarios no está disponible.")
    })
    @PutMapping("/admin/usuarios/{id}/estado")
    public UsuarioResponse estado(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Identificador de la cuenta que se quiere modificar.")
            @PathVariable long id,
            @Valid @RequestBody EstadoCuenta estado) {
        String identificadorUsuario = jwt.getSubject();
        Boolean activo = estado.activo();

        if (activo == null) {
            throw new IllegalArgumentException("Debes indicar si el usuario está activo");
        }

        return this.cuentas.estado(identificadorUsuario, id, activo);
    }

    @Operation(
            summary = "Consultar mi perfil",
            description = "Devuelve el perfil de la cuenta autenticada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil disponible."),
            @ApiResponse(responseCode = "401", description = "Sesión ausente o inválida."),
            @ApiResponse(responseCode = "404", description = "No se encontró el perfil."),
            @ApiResponse(responseCode = "503", description = "El servicio de usuarios no está disponible.")
    })
    @GetMapping("/perfil")
    public ResponseEntity<PerfilCuenta> perfil(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity
                .ok()
                .header("Cache-Control", "no-store")
                .body(this.cuentas.perfil(jwt.getSubject()));
    }

    @Operation(
            summary = "Editar mi perfil",
            description = "Actualiza los datos personales y la foto de la cuenta autenticada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil actualizado."),
            @ApiResponse(responseCode = "400", description = "Los datos de la solicitud son inválidos."),
            @ApiResponse(responseCode = "401", description = "Sesión ausente o inválida."),
            @ApiResponse(responseCode = "404", description = "No se encontró el perfil."),
            @ApiResponse(responseCode = "409", description = "El correo ya está en uso o los datos cambiaron durante la operación."),
            @ApiResponse(responseCode = "503", description = "El servicio de usuarios no está disponible.")
    })
    @PutMapping("/perfil")
    public ResponseEntity<PerfilCuenta> editarPerfil(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PerfilDatos datos) {
        return ResponseEntity
                .ok()
                .header("Cache-Control", "no-store")
                .body(this.cuentas.editarPerfil(jwt.getSubject(), datos));
    }
}
