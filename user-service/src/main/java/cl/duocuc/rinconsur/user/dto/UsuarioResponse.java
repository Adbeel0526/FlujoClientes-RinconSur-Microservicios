package cl.duocuc.rinconsur.user.dto;

import cl.duocuc.rinconsur.user.model.Rol;

import jakarta.validation.constraints.NotNull;

public record UsuarioResponse(
        long id,
        @NotNull String nombre,
        @NotNull String correo,
        @NotNull Rol rol,
        boolean activo) {
}
