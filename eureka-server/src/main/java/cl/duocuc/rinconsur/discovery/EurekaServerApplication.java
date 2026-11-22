package cl.duocuc.rinconsur.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class EurekaServerApplication {

    public static void main(String[] argumentos) {
        SpringApplication.run(EurekaServerApplication.class, argumentos);
    }
}
