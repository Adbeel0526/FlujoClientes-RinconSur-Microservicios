package cl.duocuc.rinconsur.operations;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.UUID;

public record Programacion(
    @NotNull UUID accesoId,
    @Positive long observadorId,
    @NotNull LocalDateTime inicio,
    @NotNull LocalDateTime fin
) {}
