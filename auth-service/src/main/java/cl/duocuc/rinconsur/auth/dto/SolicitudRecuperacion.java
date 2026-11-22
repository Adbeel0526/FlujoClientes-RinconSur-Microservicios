package cl.duocuc.rinconsur.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudRecuperacion(
        @NotBlank
        @Email
        @Size(max = 254)
        String correo) {
}
