package cl.duocuc.rinconsur.auth.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Claves públicas", description = "Publicación de las claves para verificar los tokens de acceso.")
@RestController
public class ClavesController {

    private final RSAKey rsaKey;

    public ClavesController(RSAKey rsaKey) {
        this.rsaKey = rsaKey;
    }

    @Operation(
            summary = "Consultar claves públicas",
            description = "Devuelve las claves públicas en formato JWKS para verificar la firma de los tokens.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conjunto de claves públicas disponible.")
    })
    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> obtenerClavePublica() {
        JWK clavePublica = this.rsaKey.toPublicJWK();
        JWKSet claves = new JWKSet(clavePublica);

        return claves.toJSONObject();
    }
}
