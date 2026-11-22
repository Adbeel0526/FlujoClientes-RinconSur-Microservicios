package cl.duocuc.rinconsur.operations;

import jakarta.validation.constraints.NotNull;

public record Estadistica(
    @NotNull String id,
    @NotNull String nombre,
    int sesiones,
    @NotNull Conteo conteo
) {}
