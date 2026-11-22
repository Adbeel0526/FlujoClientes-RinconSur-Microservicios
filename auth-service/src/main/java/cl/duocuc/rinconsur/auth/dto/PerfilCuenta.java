package cl.duocuc.rinconsur.auth.dto;

import jakarta.validation.constraints.NotNull;

public record PerfilCuenta(
        long id,
        @NotNull String nombre,
        @NotNull String apellido,
        @NotNull String correo,
        @NotNull String telefono,
        @NotNull RolCuenta rol,
        boolean activo,
        String foto) {
}
