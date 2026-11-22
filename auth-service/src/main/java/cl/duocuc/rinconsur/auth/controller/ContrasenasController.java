package cl.duocuc.rinconsur.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duocuc.rinconsur.auth.dto.CambioContrasena;
import cl.duocuc.rinconsur.auth.dto.RestablecerContrasena;
import cl.duocuc.rinconsur.auth.dto.SolicitudRecuperacion;
import cl.duocuc.rinconsur.auth.service.ContrasenasService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class ContrasenasController {

    private final ContrasenasService contrasenas;

    public ContrasenasController(ContrasenasService contrasenas) {
        this.contrasenas = contrasenas;
    }

    @PostMapping("/recuperacion")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void solicitar(@Valid @RequestBody SolicitudRecuperacion datos) {
        this.contrasenas.solicitar(datos.correo());
    }

    @PutMapping("/recuperacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restablecer(@Valid @RequestBody RestablecerContrasena datos) {
        this.contrasenas.restablecer(datos);
    }

    @PutMapping("/contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CambioContrasena datos) {
        this.contrasenas.actualizar(jwt.getSubject(), datos);
    }
}
