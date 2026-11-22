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

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@RestController
@Validated
@RequestMapping("/internal/users")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public PaginaUsuariosResponse obtenerUsuarios(
            @RequestParam(defaultValue = "0") @Min(0L) int pagina,
            @RequestParam(defaultValue = "20") @Min(1L) @Max(100L) int cantidad) {
        return this.service.obtenerUsuarios(pagina, cantidad);
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtenerUsuario(@PathVariable @Positive long id) {
        return this.service.obtenerUsuario(id);
    }

    @GetMapping("/por-correo")
    public UsuarioResponse obtenerUsuarioPorCorreo(@RequestParam @NotBlank @Email String correo) {
        return this.service.obtenerUsuarioPorCorreo(correo);
    }

    @GetMapping("/auditoria")
    public List<ActividadUsuario> actividadUsuarios() {
        return this.service.actividadUsuarios();
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crearUsuario(
            @Valid @RequestBody UsuarioRequest datos,
            @RequestHeader(value = "X-Actor-Id", required = false) @Positive Long actor) {
        UsuarioResponse usuario = this.service.crearUsuario(datos, actor);
        return ResponseEntity
                .created(URI.create("/internal/users/" + usuario.id()))
                .body(usuario);
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizarUsuario(
            @PathVariable @Positive long id,
            @Valid @RequestBody UsuarioRequest datos,
            @RequestHeader(value = "X-Actor-Id", required = false) @Positive Long actor) {
        return this.service.actualizarUsuario(id, datos, actor);
    }

    @GetMapping("/{id}/perfil")
    public PerfilResponse perfil(@PathVariable @Positive long id) {
        return this.service.perfil(id);
    }

    @PutMapping("/{id}/perfil")
    public PerfilResponse actualizarPerfil(
            @PathVariable @Positive long id,
            @Valid @RequestBody PerfilRequest datos) {
        return this.service.actualizarPerfil(id, datos);
    }

    @PatchMapping("/{id}/estado")
    public UsuarioResponse cambiarEstado(
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
