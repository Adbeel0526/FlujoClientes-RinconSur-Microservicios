package cl.duocuc.rinconsur.auth.dto;

import jakarta.validation.constraints.NotNull;

public record UsuarioResponse(
        long id,
        @NotNull String nombre,
        @NotNull String correo,
        @NotNull String rol,
        boolean activo) {
}
