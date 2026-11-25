package cl.duocuc.rinconsur.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duocuc.rinconsur.auth.dto.CambioContrasena;
import cl.duocuc.rinconsur.auth.dto.RestablecerContrasena;
import cl.duocuc.rinconsur.auth.dto.SolicitudRecuperacion;
import cl.duocuc.rinconsur.auth.service.ContrasenasService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@Tag(name = "Contraseñas", description = "Cambio y recuperación de contraseñas.")
@RestController
@RequestMapping("/api/v1/auth")
public class ContrasenasController {

    private final ContrasenasService contrasenas;

    public ContrasenasController(ContrasenasService contrasenas) {
        this.contrasenas = contrasenas;
    }

    @Operation(
            summary = "Solicitar código de recuperación",
            description = "Recibe un correo y solicita el envío de un código si la cuenta puede recuperarse.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Solicitud recibida sin confirmar si la cuenta existe.", content = @Content),
            @ApiResponse(responseCode = "400", description = "El correo o la solicitud son inválidos."),
            @ApiResponse(responseCode = "503", description = "El servicio de usuarios no está disponible.")
    })
    @PostMapping("/recuperacion")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void solicitar(@Valid @RequestBody SolicitudRecuperacion datos) {
        this.contrasenas.solicitar(datos.correo());
    }

    @Operation(
            summary = "Restablecer contraseña",
            description = "Establece una nueva contraseña mediante un código de recuperación vigente.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contraseña restablecida.", content = @Content),
            @ApiResponse(responseCode = "400", description = "Código inválido o vencido, o contraseña nueva inválida."),
            @ApiResponse(responseCode = "401", description = "La cuenta ya no está disponible para iniciar sesión."),
            @ApiResponse(responseCode = "503", description = "El servicio de usuarios no está disponible.")
    })
    @PutMapping("/recuperacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restablecer(@Valid @RequestBody RestablecerContrasena datos) {
        this.contrasenas.restablecer(datos);
    }

    @Operation(
            summary = "Cambiar mi contraseña",
            description = "Verifica la contraseña actual y guarda una nueva para la cuenta autenticada.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contraseña actualizada.", content = @Content),
            @ApiResponse(responseCode = "400", description = "Contraseña actual incorrecta o contraseña nueva inválida."),
            @ApiResponse(responseCode = "401", description = "Sesión ausente o inválida."),
            @ApiResponse(responseCode = "503", description = "El servicio de usuarios no está disponible.")
    })
    @PutMapping("/contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CambioContrasena datos) {
        this.contrasenas.actualizar(jwt.getSubject(), datos);
    }
}
