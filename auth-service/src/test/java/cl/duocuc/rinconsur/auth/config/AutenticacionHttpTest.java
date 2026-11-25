package cl.duocuc.rinconsur.auth.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import cl.duocuc.rinconsur.auth.controller.AuthController;
import cl.duocuc.rinconsur.auth.controller.CredencialController;
import cl.duocuc.rinconsur.auth.dto.*;
import cl.duocuc.rinconsur.auth.exception.*;
import cl.duocuc.rinconsur.auth.service.AuthService;

@SpringJUnitWebConfig(AutenticacionHttpTest.MvcConfig.class)
class AutenticacionHttpTest {
    private static final String TOKEN_INTERNO = "token-interno-exclusivo-para-pruebas";
    private static final String LOGIN = "/api/v1/auth/login";
    private static final String CREDENCIALES = "/internal/credentials";
    private static final String DATOS_LOGIN = """
            {"correo":"eva@example.org","contrasena":"ClaveSegura1"}
            """;
    @Autowired private WebApplicationContext contexto;
    @Autowired private FilterChainProxy seguridad;
    @Autowired private AuthService auth;
    @Autowired private JwtDecoder decoder;
    private MockMvc mvc;

    @BeforeEach
    void prepararMvc() {
        reset(auth, decoder);
        mvc = MockMvcBuilders.webAppContextSetup(contexto).addFilters(seguridad).build();
    }

    @Test
    void permiteElLoginAnonimoYEvitaAlmacenarElTokenEnCache() throws Exception {
        var usuario = new UsuarioResponse(7, "Eva", "eva@example.org", "OBSERVADOR", true);
        when(auth.login(any())).thenReturn(new LoginResponse("jwt-de-prueba", 600, usuario));

        mvc.perform(post(LOGIN).contentType("application/json").content(DATOS_LOGIN))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(jsonPath("$.accessToken").value("jwt-de-prueba"));
    }

    @Test
    void exigeAutenticacionYResuelveElUsuarioDesdeElJwt() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Debes iniciar sesión"));
        when(decoder.decode("token-prueba")).thenReturn(
                Jwt.withTokenValue("token-prueba").header("alg", "RS256").subject("7").build());
        when(auth.obtenerUsuarioActual("7"))
                .thenReturn(new UsuarioResponse(7, "Eva", "eva@example.org", "OBSERVADOR", true));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer token-prueba"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void validaElTokenInternoAntesDeCrearCredenciales() throws Exception {
        String datos = """
                {"usuarioId":7,"contrasena":"ClaveSegura1"}
                """;
        mvc.perform(post(CREDENCIALES).contentType("application/json").content(datos))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Acceso interno no autorizado"));
        verifyNoInteractions(auth);

        when(auth.crearCredencial(new CredencialRequest(7, "ClaveSegura1")))
                .thenReturn(new CredencialResponse(7));
        mvc.perform(post(CREDENCIALES).header("X-Service-Token", TOKEN_INTERNO)
                .contentType("application/json").content(datos))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.usuarioId").value(7));
    }

    @Test
    void explicaLosCamposInvalidosYElJsonMalformado() throws Exception {
        mvc.perform(post(LOGIN).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.correo").isString())
                .andExpect(jsonPath("$.errores.contrasena").isString());
        mvc.perform(post(LOGIN).contentType("application/json").content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores").isEmpty());
        verifyNoInteractions(auth);
    }

    @Test
    void distingueCredencialesInvalidasDeUnServicioNoDisponible() throws Exception {
        when(auth.login(any())).thenThrow(new CredencialesInvalidasException(), new UsuariosNoDisponibleException());
        mvc.perform(post(LOGIN).contentType("application/json").content(DATOS_LOGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
        mvc.perform(post(LOGIN).contentType("application/json").content(DATOS_LOGIN))
                .andExpect(status().isServiceUnavailable());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, WebConfig.class, AuthController.class, CredencialController.class,
            AuthExceptionHandler.class})
    static class MvcConfig {
        @Bean AuthService authService() { return mock(AuthService.class); }

        @Bean JwtDecoder jwtDecoder() { return mock(JwtDecoder.class); }

        @Bean AccesoInternoInterceptor accesoInternoInterceptor() {
            return new AccesoInternoInterceptor(TOKEN_INTERNO);
        }
    }
}
