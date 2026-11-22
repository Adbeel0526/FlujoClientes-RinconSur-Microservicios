package cl.duocuc.rinconsur.operations;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Sesion(
    @NotNull UUID id,
    @NotNull UUID accesoId,
    @NotNull String acceso,
    long observadorId,
    @NotNull String observador,
    @NotNull OffsetDateTime inicio,
    @NotNull OffsetDateTime fin,
    @NotNull Estado estado,
    @NotNull OffsetDateTime actualizada,
    @NotNull Conteo conteo
) {}
