package cl.duocuc.rinconsur.auth.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

@RestController
public class ClavesController {

    private final RSAKey rsaKey;

    public ClavesController(RSAKey rsaKey) {
        this.rsaKey = rsaKey;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> obtenerClavePublica() {
        JWK clavePublica = this.rsaKey.toPublicJWK();
        JWKSet claves = new JWKSet(clavePublica);

        return claves.toJSONObject();
    }
}
