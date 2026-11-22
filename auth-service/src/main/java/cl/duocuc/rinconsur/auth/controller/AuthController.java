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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest datos) {
        return ResponseEntity
                .ok()
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .body(service.login(datos));
    }

    @GetMapping("/me")
    public UsuarioResponse obtenerUsuarioActual(@AuthenticationPrincipal Jwt jwt) {
        return this.service.obtenerUsuarioActual(jwt.getSubject());
    }
}
