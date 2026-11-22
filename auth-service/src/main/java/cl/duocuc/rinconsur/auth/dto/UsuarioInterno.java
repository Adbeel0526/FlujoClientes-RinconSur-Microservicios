package cl.duocuc.rinconsur.auth.dto;

import jakarta.validation.constraints.NotNull;

public record UsuarioInterno(
        @NotNull String nombre,
        @NotNull String correo,
        @NotNull RolCuenta rol,
        Boolean activo) {
}
