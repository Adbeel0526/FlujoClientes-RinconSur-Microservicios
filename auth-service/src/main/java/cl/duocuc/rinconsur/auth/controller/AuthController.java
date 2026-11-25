package cl.duocuc.rinconsur.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duocuc.rinconsur.auth.dto.LoginRequest;
import cl.duocuc.rinconsur.auth.dto.LoginResponse;
import cl.duocuc.rinconsur.auth.dto.UsuarioResponse;
import cl.duocuc.rinconsur.auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@Tag(name = "Autenticación", description = "Inicio de sesión y consulta del usuario autenticado.")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @Operation(
            summary = "Iniciar sesión",
            description = "Verifica el correo y la contraseña y devuelve un token de acceso.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión iniciada."),
            @ApiResponse(responseCode = "400", description = "Los datos de la solicitud son inválidos."),
            @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos, o cuenta inactiva."),
            @ApiResponse(responseCode = "503", description = "El servicio de usuarios no está disponible.")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest datos) {
        return ResponseEntity
                .ok()
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .body(service.login(datos));
    }

    @Operation(
            summary = "Consultar mi sesión",
            description = "Devuelve los datos de la cuenta asociada al token de acceso.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos del usuario autenticado."),
            @ApiResponse(responseCode = "401", description = "Sesión ausente o inválida."),
            @ApiResponse(responseCode = "503", description = "El servicio de usuarios no está disponible.")
    })
    @GetMapping("/me")
    public UsuarioResponse obtenerUsuarioActual(@AuthenticationPrincipal Jwt jwt) {
        return this.service.obtenerUsuarioActual(jwt.getSubject());
    }
}
