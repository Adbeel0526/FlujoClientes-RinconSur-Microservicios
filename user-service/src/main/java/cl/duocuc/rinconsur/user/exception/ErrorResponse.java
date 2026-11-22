package cl.duocuc.rinconsur.user.exception;

import java.util.Map;

import jakarta.validation.constraints.NotNull;

public record ErrorResponse(
        @NotNull String mensaje,
        Map<String, String> errores) {
}
