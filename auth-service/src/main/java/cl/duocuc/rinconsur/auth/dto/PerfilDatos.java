package cl.duocuc.rinconsur.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PerfilDatos(
        @NotBlank
        @Size(max = 100)
        String nombre,
        @Size(max = 100) String apellido,
        @NotBlank
        @Email
        @Size(max = 254)
        String correo,
        @Size(max = 30)
        @Pattern(regexp = "[0-9+() .-]*")
        String telefono,
        @Size(max = 350000) String foto) {

    public PerfilDatos {
        apellido = apellido == null ? "" : apellido;
        telefono = telefono == null ? "" : telefono;
    }
}
