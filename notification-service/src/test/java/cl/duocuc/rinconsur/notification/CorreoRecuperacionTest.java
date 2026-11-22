package cl.duocuc.rinconsur.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

class CorreoRecuperacionTest {
    private final String token = "token-local-de-prueba-de-32-caracteres";
    private final SolicitudCorreo solicitud = new SolicitudCorreo("observador@example.test", "a1".repeat(32));
    private final JavaMailSender remitente = mock(JavaMailSender.class);

    @Test
    void enviaElCodigoAlDestinatario() {
        servicio("smtp.example.test").enviar(token, solicitud);
        var captura = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(remitente).send(captura.capture());
        assertThat(captura.getValue().getTo()).containsExactly(solicitud.correo());
        assertThat(captura.getValue().getText()).contains(solicitud.codigo()).doesNotContain(token);
    }

    @Test
    void rechazaTokenAusenteOIncorrecto() {
        var correo = servicio("smtp.example.test");
        for (String acceso : new String[] {null, "incorrecto"}) {
            assertThat(assertThrows(ResponseStatusException.class, () -> correo.enviar(acceso, solicitud))
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        verifyNoInteractions(remitente);
    }

    @Test
    void informaFaltaDeSmtpOFalloDeEntrega() {
        assertThat(assertThrows(ResponseStatusException.class, () -> servicio("").enviar(token, solicitud))
            .getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        doThrow(new MailSendException("Fallo de prueba")).when(remitente).send(any(SimpleMailMessage.class));
        assertThat(assertThrows(ResponseStatusException.class,
            () -> servicio("smtp.example.test").enviar(token, solicitud)).getStatusCode())
            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    private CorreoRecuperacion servicio(String host) {
        var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("remitente", remitente);
        return new CorreoRecuperacion(beans.getBeanProvider(JavaMailSender.class), host, "origen@example.test", token);
    }
}
