package cl.duocuc.rinconsur.auth.dto;

import jakarta.validation.constraints.NotNull;

public record CorreoRecuperacionRequest(
        @NotNull String correo,
        @NotNull String codigo) {
}
