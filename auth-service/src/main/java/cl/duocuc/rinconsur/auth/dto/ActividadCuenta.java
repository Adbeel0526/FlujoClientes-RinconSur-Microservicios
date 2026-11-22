package cl.duocuc.rinconsur.auth.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;

public record ActividadCuenta(
        @NotNull String id,
        @NotNull String actor,
        @NotNull String accion,
        @NotNull String referencia,
        @NotNull OffsetDateTime fecha) {
}
