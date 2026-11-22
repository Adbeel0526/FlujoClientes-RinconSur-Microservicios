package cl.duocuc.rinconsur.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AccesoInternoInterceptor accesoInternoInterceptor;

    public WebConfig(AccesoInternoInterceptor accesoInternoInterceptor) {
        this.accesoInternoInterceptor = accesoInternoInterceptor;
    }

    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(this.accesoInternoInterceptor);
    }
}
