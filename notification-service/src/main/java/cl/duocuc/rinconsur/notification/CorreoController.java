package cl.duocuc.rinconsur.notification;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
public class CorreoController {

    private final CorreoRecuperacion correo;

    public CorreoController(CorreoRecuperacion correo) {
        this.correo = correo;
    }

    @PostMapping("/internal/notifications/password-reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enviar(
            @RequestHeader(value = "X-Service-Token", required = false) String token,
            @Valid @RequestBody SolicitudCorreo datos) {
        this.correo.enviar(token, datos);
    }
}
