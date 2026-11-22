package cl.duocuc.rinconsur.user.dto;

import jakarta.validation.constraints.NotNull;

public record EstadoUsuarioRequest(
        @NotNull(message = "Debes indicar si el usuario está activo") Boolean activo) {
}
