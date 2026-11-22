package cl.duocuc.rinconsur.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RestablecerContrasena(
        @Pattern(regexp = "[a-f0-9]{64}")
        @NotNull
        String codigo,
        @NotBlank
        @Size(min = 8, max = 72)
        String nueva) {
}
