package cl.duocuc.rinconsur.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CorreoControllerTest {
    private static final String RUTA = "/internal/notifications/password-reset";
    private static final String TOKEN = "token-interno-exclusivo-para-pruebas";
    private static final String CODIGO = "a1".repeat(32);
    private static final String SOLICITUD = """
            {"correo":"eva@example.org","codigo":"%s"}
            """.formatted(CODIGO);
    private final JavaMailSender remitente = mock(JavaMailSender.class);

    @Test
    void aceptaUnaSolicitudInternaYEnviaElCodigoSinCuerpoHttp() throws Exception {
        mvc().perform(post(RUTA).header("X-Service-Token", TOKEN)
                .contentType("application/json").content(SOLICITUD))
                .andExpect(status().isNoContent());

        var mensaje = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(remitente).send(mensaje.capture());
        assertThat(mensaje.getValue().getTo()).containsExactly("eva@example.org");
        assertThat(mensaje.getValue().getText()).contains(CODIGO);
    }

    @Test
    void rechazaElCodigoInvalidoYElTokenAusenteSinEnviarCorreo() throws Exception {
        var mvc = mvc();
        mvc.perform(post(RUTA).header("X-Service-Token", TOKEN)
                .contentType("application/json").content(SOLICITUD.replace(CODIGO, "abc")))
                .andExpect(status().isBadRequest());
        mvc.perform(post(RUTA).contentType("application/json").content(SOLICITUD))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(remitente);
    }

    private MockMvc mvc() {
        var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("remitente", remitente);
        var correo = new CorreoRecuperacion(beans.getBeanProvider(JavaMailSender.class),
                "smtp.example.org", "origen@example.org", TOKEN);
        return MockMvcBuilders.standaloneSetup(new CorreoController(correo)).build();
    }
}
