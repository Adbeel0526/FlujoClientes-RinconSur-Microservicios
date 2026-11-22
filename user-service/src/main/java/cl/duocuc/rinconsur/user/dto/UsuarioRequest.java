package cl.duocuc.rinconsur.user.dto;

import cl.duocuc.rinconsur.user.model.Rol;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank(message = "Debes ingresar un nombre") @Size(max = 100, message = "El nombre no puede superar los 100 caracteres") String nombre,
        @NotBlank(message = "Debes ingresar un correo") @Email(message = "Debes ingresar un correo válido") @Size(max = 254, message = "El correo no puede superar los 254 caracteres") String correo,
        @NotNull Rol rol,
        Boolean activo) {
}
