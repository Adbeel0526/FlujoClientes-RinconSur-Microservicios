package cl.duocuc.rinconsur.notification;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@Tag(name = "Notificaciones por correo", description = "Envío interno de códigos de recuperación de contraseña.")
@RestController
public class CorreoController {

    private final CorreoRecuperacion correo;

    public CorreoController(CorreoRecuperacion correo) {
        this.correo = correo;
    }

    @Operation(
            summary = "Enviar código de recuperación",
            description = "Envía por correo el código de recuperación al destinatario indicado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Correo enviado.", content = @Content),
            @ApiResponse(responseCode = "400", description = "El correo o el código son inválidos."),
            @ApiResponse(responseCode = "401", description = "Token interno ausente o inválido."),
            @ApiResponse(responseCode = "503", description = "El servicio de correo no está disponible o el envío falló.")
    })
    @PostMapping("/internal/notifications/password-reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enviar(
            @RequestHeader(value = "X-Service-Token", required = false) String token,
            @Valid @RequestBody SolicitudCorreo datos) {
        this.correo.enviar(token, datos);
    }
}
