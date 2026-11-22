package cl.duocuc.rinconsur.operations;

import jakarta.validation.constraints.NotNull;

public record Usuario(
    long id,
    @NotNull String nombre,
    @NotNull Rol rol,
    boolean activo
) {}
