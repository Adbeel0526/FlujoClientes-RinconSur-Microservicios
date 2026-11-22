package cl.duocuc.rinconsur.operations;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Actividad(
    @NotNull UUID id,
    long actorId,
    @NotNull String actor,
    @NotNull AccionAuditada accion,
    UUID referencia,
    @NotNull OffsetDateTime fecha
) {}
