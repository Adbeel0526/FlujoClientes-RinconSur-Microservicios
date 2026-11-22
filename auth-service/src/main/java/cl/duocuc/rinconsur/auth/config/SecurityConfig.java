package cl.duocuc.rinconsur.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint noAutorizado = (solicitud, respuesta, error) -> {
            respuesta.setStatus(HttpStatus.UNAUTHORIZED.value());
            respuesta.setContentType("application/json");
            respuesta.setCharacterEncoding("UTF-8");
            respuesta.getWriter().write("{\"mensaje\":\"Debes iniciar sesión\",\"errores\":{}}");
        };
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sesiones -> sesiones.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reglas -> reglas
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/.well-known/jwks.json",
                                "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login")
                        .permitAll()
                        .requestMatchers("/api/v1/auth/recuperacion", "/internal/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me")
                        .authenticated()
                        .requestMatchers("/api/v1/auth/contrasena", "/api/v1/auth/perfil",
                                "/api/v1/auth/admin/usuarios", "/api/v1/auth/admin/usuarios/**")
                        .authenticated()
                        .anyRequest()
                        .denyAll())
                .exceptionHandling(errores -> errores.authenticationEntryPoint(noAutorizado))
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()).authenticationEntryPoint(noAutorizado))
                .build();
    }
}
