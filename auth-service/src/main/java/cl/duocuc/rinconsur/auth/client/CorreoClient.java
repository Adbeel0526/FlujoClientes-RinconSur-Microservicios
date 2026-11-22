package cl.duocuc.rinconsur.auth.client;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import cl.duocuc.rinconsur.auth.dto.CorreoRecuperacionRequest;

import org.slf4j.LoggerFactory;

@Component
public class CorreoClient {

    private final WebClient cliente;

    public CorreoClient(
            @LoadBalanced WebClient.Builder constructorCliente,
            @Value("${seguridad.interna.token}") String token) {
        cliente = constructorCliente
                .clone()
                .baseUrl("http://notification-service")
                .defaultHeader("X-Service-Token", token)
                .build();
    }

    public void enviar(String correo, String codigo) {
        try {
            cliente
                    .post()
                    .uri("/internal/notifications/password-reset")
                    .bodyValue(new CorreoRecuperacionRequest(correo, codigo))
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(12))
                    .block();
        } catch (WebClientException | IllegalStateException error) {
            LoggerFactory.getLogger(CorreoClient.class).warn("No se pudo solicitar el correo de recuperación");
        }
    }
}
