package cl.duocuc.rinconsur.notification;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CorreoRecuperacion {

    private static final Logger LOG = LoggerFactory.getLogger(CorreoRecuperacion.class);
    private final ObjectProvider<JavaMailSender> remitente;
    private final String host;
    private final String desde;
    private final byte[] interno;

    public CorreoRecuperacion(
            ObjectProvider<JavaMailSender> remitente,
            @Value("${spring.mail.host}") String host,
            @Value("${correo.desde}") String desde,
            @Value("${correo.token-interno}") String token) {
        this.remitente = remitente;
        this.host = host;
        this.desde = desde;
        interno = token.getBytes(StandardCharsets.UTF_8);
    }

    public void enviar(String token, SolicitudCorreo datos) {
        if (interno.length < 32 || token == null
                || !MessageDigest.isEqual(interno, token.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        JavaMailSender correo = remitente.getIfAvailable();
        if (host.isBlank() || desde.isBlank() || correo == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(desde);
        mensaje.setTo(datos.correo());
        mensaje.setSubject("Código para recuperar tu cuenta de Rincón Sur");
        mensaje.setText("Ingresa este código en Rincón Sur: " + datos.codigo() + "\n\nCaduca en 15 minutos.");

        try {
            correo.send(mensaje);
        } catch (MailException error) {
            LOG.warn("No se pudo entregar un correo de recuperación");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, null, error);
        }
    }
}
