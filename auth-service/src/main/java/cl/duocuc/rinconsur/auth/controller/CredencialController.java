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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/internal/credentials")
public class CredencialController {

    private final AuthService service;

    public CredencialController(AuthService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CredencialResponse> crearCredencial(@Valid @RequestBody CredencialRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.service.crearCredencial(datos));
    }
}
