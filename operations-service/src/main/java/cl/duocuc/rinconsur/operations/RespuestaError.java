package cl.duocuc.rinconsur.operations;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record RespuestaError(
    int status,
    @NotNull String message,
    Instant timestamp
) {

    public RespuestaError(int status, String message) {
        this(status, message, Instant.now());
    }
}
