package cl.duocuc.rinconsur.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioDatos(
        @NotBlank
        @Size(max = 100)
        String nombre,
        @NotBlank
        @Email
        @Size(max = 254)
        String correo,
        @NotNull RolCuenta rol,
        @Size(min = 8, max = 72) String contrasena) {
}
