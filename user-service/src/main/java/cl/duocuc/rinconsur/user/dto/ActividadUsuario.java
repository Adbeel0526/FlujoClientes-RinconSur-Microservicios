package cl.duocuc.rinconsur.user.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;

public record ActividadUsuario(
        @NotNull String id,
        @NotNull String actor,
        @NotNull String accion,
        @NotNull String referencia,
        @NotNull OffsetDateTime fecha) {
}
