package cl.duocuc.rinconsur.auth.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration(proxyBeanMethods = false)
public class UsuarioClientConfig {

    @Bean
    @LoadBalanced
    WebClient.Builder clienteInterno() {
        return WebClient
                .builder()
                .clientConnector(new JdkClientHttpConnector(
                        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()));
    }
}
