package cl.duocuc.rinconsur.auth.dto;

import jakarta.validation.constraints.NotNull;

public record LoginResponse(
        @NotNull String accessToken,
        String tokenType,
        long expiresIn,
        @NotNull UsuarioResponse usuario) {

    public LoginResponse(String accessToken, long expiresIn, UsuarioResponse usuario) {
        this(accessToken, "Bearer", expiresIn, usuario);
    }
}
