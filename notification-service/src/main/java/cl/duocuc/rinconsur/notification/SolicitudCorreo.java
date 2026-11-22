package cl.duocuc.rinconsur.notification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudCorreo(
        @NotBlank @Email @Size(max = 254) String correo,

        @Pattern(regexp = "[a-f0-9]{64}") @NotNull String codigo) {
}
