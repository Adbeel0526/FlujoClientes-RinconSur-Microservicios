package cl.duocuc.rinconsur.user.dto;

import cl.duocuc.rinconsur.user.model.Rol;

import jakarta.validation.constraints.NotNull;

public record PerfilResponse(
        long id,
        @NotNull String nombre,
        @NotNull String apellido,
        @NotNull String correo,
        @NotNull String telefono,
        @NotNull Rol rol,
        boolean activo,
        String foto) {
}
