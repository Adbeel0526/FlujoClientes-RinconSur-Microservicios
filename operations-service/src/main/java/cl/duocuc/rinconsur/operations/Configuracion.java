package cl.duocuc.rinconsur.operations;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record Configuracion(
    @NotNull LocalTime apertura,
    @NotNull LocalTime cierre,
    int minimo,
    int maximo,
    @NotNull String zona
) {}
