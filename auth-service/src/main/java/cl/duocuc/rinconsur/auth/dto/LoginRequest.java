package cl.duocuc.rinconsur.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Debes ingresar un correo")
        @Email(message = "Debes ingresar un correo válido")
        @Size(max = 254, message = "El correo no puede superar los 254 caracteres")
        String correo,
        @NotBlank(message = "Debes ingresar una contraseña")
        @Size(max = 72, message = "La contraseña no puede superar los 72 caracteres")
        String contrasena) {
}
