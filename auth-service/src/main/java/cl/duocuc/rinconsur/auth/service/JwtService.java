package cl.duocuc.rinconsur.auth.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import cl.duocuc.rinconsur.auth.dto.LoginResponse;
import cl.duocuc.rinconsur.auth.dto.UsuarioResponse;

import com.nimbusds.jose.jwk.RSAKey;

@Service
public class JwtService {

    private final JwtEncoder codificadorJwt;
    private final RSAKey claveRsa;
    private final String emisor;
    private final String audiencia;
    private final long duracionSegundos;

    public JwtService(
            JwtEncoder codificadorJwt,
            RSAKey claveRsa,
            @Value("${jwt.emisor}") String emisor,
            @Value("${jwt.audiencia}") String audiencia,
            @Value("${jwt.duracion-segundos}") long duracionSegundos) {
        this.codificadorJwt = codificadorJwt;
        this.claveRsa = claveRsa;
        this.emisor = emisor;
        this.audiencia = audiencia;
        this.duracionSegundos = duracionSegundos;
        if (duracionSegundos < 60 || duracionSegundos > 3600) {
            throw new IllegalArgumentException("La duración del JWT debe estar entre 60 y 3600 segundos");
        }
    }

    public LoginResponse crearToken(UsuarioResponse usuario, long version) {
        Instant ahora = Instant.now();
        JwtClaimsSet atributos = JwtClaimsSet
                .builder()
                .issuer(this.emisor)
                .subject(String.valueOf(usuario.id()))
                .audience(List.of(this.audiencia))
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(this.duracionSegundos))
                .id(UUID.randomUUID().toString())
                .claim("cv", version)
                .claim("roles", List.of(usuario.rol()))
                .build();

        JwsHeader encabezado = JwsHeader
                .with(SignatureAlgorithm.RS256)
                .keyId(this.claveRsa.getKeyID())
                .build();

        String token = this.codificadorJwt.encode(JwtEncoderParameters.from(encabezado, atributos)).getTokenValue();
        return new LoginResponse(token, this.duracionSegundos, usuario);
    }
}
