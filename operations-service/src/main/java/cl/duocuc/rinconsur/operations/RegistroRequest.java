package cl.duocuc.rinconsur.operations;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RegistroRequest(
    @NotNull UUID id,
    @NotNull Categoria categoria
) {}
