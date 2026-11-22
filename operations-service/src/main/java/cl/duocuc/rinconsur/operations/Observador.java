package cl.duocuc.rinconsur.operations;

import jakarta.validation.constraints.NotNull;

public record Observador(
    long id,
    @NotNull String nombre
) {}
