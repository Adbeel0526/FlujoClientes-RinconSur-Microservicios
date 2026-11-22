package cl.duocuc.rinconsur.operations;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record Acceso(
    @NotNull UUID id,
    @NotNull String nombre,
    boolean activo
) {}
