package cl.duocuc.rinconsur.user.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import cl.duocuc.rinconsur.user.controller.UsuarioController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AccesoInternoInterceptor implements HandlerInterceptor {

    private final byte[] tokenEsperado;

    public AccesoInternoInterceptor(@Value("${seguridad.interna.token}") String token) {
        tokenEsperado = token.getBytes(StandardCharsets.UTF_8);

        if (tokenEsperado.length < 32) {
            throw new IllegalArgumentException("INTERNAL_SERVICE_TOKEN debe tener al menos 32 bytes");
        }
    }

    @Override
    public boolean preHandle(
            HttpServletRequest solicitud,
            HttpServletResponse respuesta,
            Object destino) throws IOException {
        if (!(destino instanceof HandlerMethod metodo)
                || !UsuarioController.class.isAssignableFrom(metodo.getBeanType())) {
            return true;
        }

        String recibido = solicitud.getHeader("X-Service-Token");
        if (recibido != null && MessageDigest.isEqual(tokenEsperado, recibido.getBytes(StandardCharsets.UTF_8))) {
            return true;
        }

        respuesta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        respuesta.setContentType("application/json");
        respuesta.setCharacterEncoding("UTF-8");
        respuesta.getWriter().write("{\"mensaje\":\"Acceso interno no autorizado\",\"errores\":{}}");
        return false;
    }
}
