package cl.duocuc.rinconsur.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duocuc.rinconsur.auth.dto.CredencialRequest;
import cl.duocuc.rinconsur.auth.dto.CredencialResponse;
import cl.duocuc.rinconsur.auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@Tag(name = "Credenciales internas", description = "Registro de credenciales desde otros servicios del backend.")
@RestController
@RequestMapping("/internal/credentials")
public class CredencialController {

    private final AuthService service;

    public CredencialController(AuthService service) {
        this.service = service;
    }

    @Operation(
            summary = "Registrar credencial",
            description = "Guarda la contraseña inicial de un usuario activo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Credencial registrada."),
            @ApiResponse(responseCode = "400", description = "Los datos de la solicitud son inválidos."),
            @ApiResponse(responseCode = "401", description = "Token interno ausente o inválido."),
            @ApiResponse(responseCode = "404", description = "No se encontró el usuario."),
            @ApiResponse(responseCode = "409", description = "La cuenta está inactiva o ya tiene una credencial."),
            @ApiResponse(responseCode = "503", description = "El servicio de usuarios no está disponible.")
    })
    @PostMapping
    public ResponseEntity<CredencialResponse> crearCredencial(@Valid @RequestBody CredencialRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.service.crearCredencial(datos));
    }
}
