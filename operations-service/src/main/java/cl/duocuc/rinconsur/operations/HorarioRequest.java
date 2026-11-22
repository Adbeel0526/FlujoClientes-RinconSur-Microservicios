package cl.duocuc.rinconsur.operations;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record HorarioRequest(
    @NotNull LocalTime apertura,
    @NotNull LocalTime cierre,
    @Min(1)
    @Max(1440) int minimo,
    @Min(1)
    @Max(1440) int maximo
) {}
