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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class CuentasController {

    private final CuentasService cuentas;

    public CuentasController(CuentasService cuentas) {
        this.cuentas = cuentas;
    }

    @GetMapping("/admin/usuarios")
    public PaginaCuentas listar(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int pagina) {
        return this.cuentas.listar(jwt.getSubject(), pagina);
    }

    @GetMapping("/admin/usuarios/auditoria")
    public List<ActividadCuenta> actividad(@AuthenticationPrincipal Jwt jwt) {
        return this.cuentas.actividad(jwt.getSubject());
    }

    @PostMapping("/admin/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UsuarioDatos datos) {
        return this.cuentas.crear(jwt.getSubject(), datos);
    }

    @PutMapping("/admin/usuarios/{id}")
    public UsuarioResponse editar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long id,
            @Valid @RequestBody UsuarioDatos datos) {
        return this.cuentas.editar(jwt.getSubject(), id, datos);
    }

    @PutMapping("/admin/usuarios/{id}/estado")
    public UsuarioResponse estado(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long id,
            @Valid @RequestBody EstadoCuenta estado) {
        String identificadorUsuario = jwt.getSubject();
        Boolean activo = estado.activo();

        if (activo == null) {
            throw new IllegalArgumentException("Debes indicar si el usuario está activo");
        }

        return this.cuentas.estado(identificadorUsuario, id, activo);
    }

    @GetMapping("/perfil")
    public ResponseEntity<PerfilCuenta> perfil(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity
                .ok()
                .header("Cache-Control", "no-store")
                .body(this.cuentas.perfil(jwt.getSubject()));
    }

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
