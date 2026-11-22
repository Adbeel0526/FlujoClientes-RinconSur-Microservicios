package cl.duocuc.rinconsur.auth.dto;

import jakarta.validation.constraints.NotNull;

public record EstadoCuenta(
        @NotNull Boolean activo) {
}
