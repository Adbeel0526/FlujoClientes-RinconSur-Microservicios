package cl.duocuc.rinconsur.auth.config;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import cl.duocuc.rinconsur.auth.data.entity.CredencialEntity;
import cl.duocuc.rinconsur.auth.data.repository.CredencialRepository;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

@Configuration(proxyBeanMethods = false)
public class JwtConfig {

    @Bean
    RSAKey rsaKey(
            @Value("${jwt.clave-publica}") Resource publica,
            @Value("${jwt.clave-privada}") Resource privada) throws IOException, NoSuchAlgorithmException {
        RSAPublicKey clavePublica;
        RSAPrivateKey clavePrivada;
        try (InputStream entrada = publica.getInputStream()) {
            clavePublica = Objects.requireNonNull(RsaKeyConverters.x509().convert(entrada));
        }
        try (InputStream entrada = privada.getInputStream()) {
            clavePrivada = Objects.requireNonNull(RsaKeyConverters.pkcs8().convert(entrada));
        }
        if (clavePublica.getModulus().bitLength() < 2048) {
            throw new IllegalArgumentException("La clave RSA debe tener al menos 2048 bits");
        }
        if (!clavePublica.getModulus().equals(clavePrivada.getModulus())) {
            throw new IllegalArgumentException("Las claves RSA no pertenecen al mismo par");
        }
        String id = Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(MessageDigest.getInstance("SHA-256").digest(clavePublica.getEncoded()));
        return new RSAKey.Builder(clavePublica)
                .privateKey(clavePrivada)
                .keyID(id)
                .algorithm(JWSAlgorithm.RS256)
                .keyUse(KeyUse.SIGNATURE)
                .build();
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey clave) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(clave)));
    }

    @Bean
    JwtDecoder jwtDecoder(
            RSAKey clave,
            CredencialRepository credenciales,
            @Value("${jwt.emisor}") String emisor,
            @Value("${jwt.audiencia}") String audiencia)
            throws JOSEException {
        NimbusJwtDecoder decodificadorJwt = NimbusJwtDecoder.withPublicKey(clave.toRSAPublicKey()).build();
        JwtClaimValidator<List<String>> validarAudiencia = new JwtClaimValidator<>(JwtClaimNames.AUD,
                aud -> aud != null && aud.contains(audiencia));
        DelegatingOAuth2TokenValidator<Jwt> estandar = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(emisor),
                validarAudiencia);
        decodificadorJwt.setJwtValidator(token -> {
            OAuth2TokenValidatorResult resultado = estandar.validate(token);
            if (resultado.hasErrors()) {
                return resultado;
            }
            try {
                long id = Long.parseLong(token.getSubject());
                CredencialEntity credencial = credenciales.findByUsuarioId(id);
                if (token.getClaims().get("cv") instanceof Number version && credencial != null
                        && Objects.equals(credencial.getVersion(), version.longValue())) {
                    return OAuth2TokenValidatorResult.success();
                }
            } catch (NumberFormatException error) {
                return tokenInvalido();
            }
            return tokenInvalido();
        });
        return decodificadorJwt;
    }

    private OAuth2TokenValidatorResult tokenInvalido() {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
