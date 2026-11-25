package cl.duocuc.rinconsur.user.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duocuc.rinconsur.user.dto.ActividadUsuario;
import cl.duocuc.rinconsur.user.dto.EstadoUsuarioRequest;
import cl.duocuc.rinconsur.user.dto.PaginaUsuariosResponse;
import cl.duocuc.rinconsur.user.dto.PerfilRequest;
import cl.duocuc.rinconsur.user.dto.PerfilResponse;
import cl.duocuc.rinconsur.user.dto.UsuarioRequest;
import cl.duocuc.rinconsur.user.dto.UsuarioResponse;
import cl.duocuc.rinconsur.user.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Tag(name = "Usuarios internos", description = "Gestión de usuarios, perfiles y actividad entre servicios del backend.")
@RestController
@Validated
@RequestMapping("/internal/users")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @Operation(
            summary = "Listar usuarios",
            description = "Devuelve una página de usuarios ordenada por identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de usuarios disponible."),
            @ApiResponse(responseCode = "400", description = "La página o la cantidad son inválidas."),
            @ApiResponse(responseCode = "401", description = "Token interno ausente o inválido.")
    })
    @GetMapping
    public PaginaUsuariosResponse obtenerUsuarios(
            @Parameter(description = "Número de página, empezando en cero.")
            @RequestParam(defaultValue = "0") @Min(0L) int pagina,
            @Parameter(description = "Cantidad de usuarios por página, entre 1 y 100.")
            @RequestParam(defaultValue = "20") @Min(1L) @Max(100L) int cantidad) {
        return this.service.obtenerUsuarios(pagina, cantidad);
    }

    @Operation(
            summary = "Buscar usuario por ID",
            description = "Devuelve los datos básicos del usuario indicado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado."),
            @ApiResponse(responseCode = "400", description = "El identificador es inválido."),
            @ApiResponse(responseCode = "401", description = "Token interno ausente o inválido."),
            @ApiResponse(responseCode = "404", description = "No se encontró el usuario.")
    })
    @GetMapping("/{id}")
    public UsuarioResponse obtenerUsuario(
            @Parameter(description = "Identificador del usuario.")
            @PathVariable @Positive long id) {
        return this.service.obtenerUsuario(id);
    }

    @Operation(
            summary = "Buscar usuario por correo",
            description = "Busca un usuario a partir de su dirección de correo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado."),
            @ApiResponse(responseCode = "400", description = "La dirección de correo es inválida."),
            @ApiResponse(responseCode = "401", description = "Token interno ausente o inválido."),
            @ApiResponse(responseCode = "404", description = "No se encontró el usuario.")
    })
    @GetMapping("/por-correo")
    public UsuarioResponse obtenerUsuarioPorCorreo(
            @Parameter(description = "Correo electrónico del usuario que se busca.")
            @RequestParam @NotBlank @Email String correo) {
        return this.service.obtenerUsuarioPorCorreo(correo);
    }

    @Operation(
            summary = "Consultar actividad de usuarios",
            description = "Devuelve los cambios recientes registrados sobre los usuarios.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actividad disponible."),
            @ApiResponse(responseCode = "401", description = "Token interno ausente o inválido.")
    })
    @GetMapping("/auditoria")
    public List<ActividadUsuario> actividadUsuarios() {
        return this.service.actividadUsuarios();
    }

    @Operation(
            summary = "Crear usuario",
            description = "Registra un usuario con su nombre, correo, rol y estado inicial.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado."),
            @ApiResponse(responseCode = "400", description = "Los datos de la solicitud son inválidos."),
            @ApiResponse(responseCode = "401", description = "Token interno ausente o inválido."),
            @ApiResponse(responseCode = "404", description = "No se encontró al usuario que realiza el cambio."),
            @ApiResponse(responseCode = "409", description = "El correo ya está en uso o los datos cambiaron durante la operación.")
    })
    @PostMapping
    public ResponseEntity<UsuarioResponse> crearUsuario(
            @Valid @RequestBody UsuarioRequest datos,
            @RequestHeader(value = "X-Actor-Id", required = false) @Positive Long actor) {
        UsuarioResponse usuario = this.service.crearUsuario(datos, actor);
        return ResponseEntity
                .created(URI.create("/internal/users/" + usuario.id()))
                .body(usuario);
    }

    @Operation(
            summary = "Actualizar usuario",
            description = "Edita los datos básicos y el rol del usuario indicado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario actualizado."),
            @ApiResponse(responseCode = "400", description = "Los datos de la solicitud son inválidos."),
            @ApiResponse(responseCode = "401", description = "Token interno ausente o inválido."),
            @ApiResponse(responseCode = "404", description = "No se encontró el usuario indicado o quien realiza el cambio."),
            @ApiResponse(responseCode = "409", description = "El correo ya está en uso o los datos cambiaron durante la operación.")
    })
    @PutMapping("/{id}")
    public UsuarioResponse actualizarUsuario(
            @Parameter(description = "Identificador del usuario.")
            @PathVariable @Positive long id,
            @Valid @RequestBody UsuarioRequest datos,
            @RequestHeader(value = "X-Actor-Id", required = false) @Positive Long actor) {
        return this.service.actualizarUsuario(id, datos, actor);
    }

    @Operation(
            summary = "Consultar perfil de usuario",
            description = "Devuelve los datos personales y la foto del usuario indicado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil disponible."),
            @ApiResponse(responseCode = "400", description = "El identificador es inválido."),
            @ApiResponse(responseCode = "401", description = "Token interno ausente o inválido."),
            @ApiResponse(responseCode = "404", description = "No se encontró el usuario.")
    })
    @GetMapping("/{id}/perfil")
    public PerfilResponse perfil(
            @Parameter(description = "Identificador del usuario.")
            @PathVariable @Positive long id) {
        return this.service.perfil(id);
    }

    @Operation(
            summary = "Actualizar perfil de usuario",
            description = "Guarda los datos personales y la foto del usuario indicado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil actualizado."),
            @ApiResponse(responseCode = "400", description = "Los datos de la solicitud son inválidos."),
            @ApiResponse(responseCode = "401", description = "Token interno ausente o inválido."),
            @ApiResponse(responseCode = "404", description = "No se encontró el usuario."),
            @ApiResponse(responseCode = "409", description = "El correo ya está en uso o los datos cambiaron durante la operación.")
    })
    @PutMapping("/{id}/perfil")
    public PerfilResponse actualizarPerfil(
            @Parameter(description = "Identificador del usuario.")
            @PathVariable @Positive long id,
            @Valid @RequestBody PerfilRequest datos) {
        return this.service.actualizarPerfil(id, datos);
    }

    @Operation(
            summary = "Cambiar estado de usuario",
            description = "Activa o desactiva al usuario indicado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado del usuario actualizado."),
            @ApiResponse(responseCode = "400", description = "Los datos de la solicitud son inválidos."),
            @ApiResponse(responseCode = "401", description = "Token interno ausente o inválido."),
            @ApiResponse(responseCode = "404", description = "No se encontró el usuario indicado o quien realiza el cambio."),
            @ApiResponse(responseCode = "409", description = "Otro cambio entró en conflicto con la actualización.")
    })
    @PatchMapping("/{id}/estado")
    public UsuarioResponse cambiarEstado(
            @Parameter(description = "Identificador del usuario.")
            @PathVariable @Positive long id,
            @Valid @RequestBody EstadoUsuarioRequest datos,
            @RequestHeader(value = "X-Actor-Id", required = false) @Positive Long actor) {
        Boolean activo = datos.activo();
        if (activo == null) {
            throw new IllegalArgumentException("Debes indicar si el usuario está activo");
        }

        return service.cambiarEstado(id, activo, actor);
    }
}
