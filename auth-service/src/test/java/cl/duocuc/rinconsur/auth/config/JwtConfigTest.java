package cl.duocuc.rinconsur.auth.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Base64;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import cl.duocuc.rinconsur.auth.controller.ClavesController;
import cl.duocuc.rinconsur.auth.data.entity.CredencialEntity;
import cl.duocuc.rinconsur.auth.data.repository.CredencialRepository;
import cl.duocuc.rinconsur.auth.dto.UsuarioResponse;
import cl.duocuc.rinconsur.auth.service.JwtService;

class JwtConfigTest {
    private static final String EMISOR = "rincon-sur-test";
    private static final String AUDIENCIA = "rincon-app-test";
    private static KeyPair par;
    private final JwtConfig config = new JwtConfig();

    @BeforeAll
    static void generarClaves() throws Exception {
        par = generarPar(2048);
    }

    @Test
    void firmaYValidaUnJwtHastaQueCambiaLaVersionDeCredencial() throws Exception {
        var clave = config.rsaKey(pem("PUBLIC KEY", par.getPublic().getEncoded()),
                pem("PRIVATE KEY", par.getPrivate().getEncoded()));
        var credenciales = mock(CredencialRepository.class);
        var credencial = mock(CredencialEntity.class);
        when(credenciales.findByUsuarioId(7L)).thenReturn(credencial);
        when(credencial.getVersion()).thenReturn(3L, 4L);
        var servicio = new JwtService(config.jwtEncoder(clave), clave, EMISOR, AUDIENCIA, 600);
        var usuario = new UsuarioResponse(7, "Eva", "eva@example.org", "OBSERVADOR", true);
        var respuesta = servicio.crearToken(usuario, 3);
        var decoder = config.jwtDecoder(clave, credenciales, EMISOR, AUDIENCIA);

        var jwt = decoder.decode(respuesta.accessToken());
        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getAudience()).containsExactly(AUDIENCIA);
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("OBSERVADOR");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).hasSeconds(600);
        assertThat(jwt.getHeaders()).containsEntry("kid", clave.getKeyID());
        assertThat(new ClavesController(clave).obtenerClavePublica())
                .containsEntry("keys", java.util.List.of(clave.toPublicJWK().toJSONObject()));
        assertThatThrownBy(() -> decoder.decode(respuesta.accessToken())).isInstanceOf(JwtValidationException.class);
        var otroDestino = config.jwtDecoder(clave, credenciales, EMISOR, "otra-audiencia");
        assertThatThrownBy(() -> otroDestino.decode(respuesta.accessToken())).isInstanceOf(JwtValidationException.class);
        verify(credenciales, times(2)).findByUsuarioId(7L);
    }

    @Test
    void rechazaClavesDebilesYClavesDeDistintosPares() throws Exception {
        var debil = generarPar(1024);
        var otra = generarPar(2048);
        assertThatIllegalArgumentException().isThrownBy(() -> config.rsaKey(
                pem("PUBLIC KEY", debil.getPublic().getEncoded()), pem("PRIVATE KEY", debil.getPrivate().getEncoded())))
                .withMessageContaining("2048 bits");
        assertThatIllegalArgumentException().isThrownBy(() -> config.rsaKey(
                pem("PUBLIC KEY", par.getPublic().getEncoded()), pem("PRIVATE KEY", otra.getPrivate().getEncoded())))
                .withMessageContaining("mismo par");
    }

    private static KeyPair generarPar(int bits) throws Exception {
        var generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(bits);
        return generador.generateKeyPair();
    }

    private static ByteArrayResource pem(String tipo, byte[] clave) {
        return new ByteArrayResource(("-----BEGIN " + tipo + "-----\n"
                + Base64.getMimeEncoder().encodeToString(clave) + "\n-----END " + tipo + "-----\n")
                .getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }
}
