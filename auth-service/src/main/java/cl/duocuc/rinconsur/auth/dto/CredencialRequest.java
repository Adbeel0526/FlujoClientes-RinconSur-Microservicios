package cl.duocuc.rinconsur.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CredencialRequest(
        @Positive(message = "Debes indicar un usuario válido") long usuarioId,
        @NotBlank(message = "Debes ingresar una contraseña")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String contrasena) {
}
