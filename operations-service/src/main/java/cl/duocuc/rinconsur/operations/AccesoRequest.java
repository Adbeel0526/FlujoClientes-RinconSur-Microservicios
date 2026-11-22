package cl.duocuc.rinconsur.operations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccesoRequest(
    @NotBlank
    @Size(max = 80) String nombre,
    Boolean activo
) {}
