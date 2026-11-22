package cl.duocuc.rinconsur.operations;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OperacionesControllerTest {
    private final Identidad identidad = mock(Identidad.class);
    private final OperacionesService servicio = mock(OperacionesService.class);
    private final Usuario observador = new Usuario(3, "Ana", Rol.OBSERVADOR, true);
    private final String ruta = "/api/v1/operations";
    private final String token = "Bearer prueba";
    private final org.springframework.test.web.servlet.MockMvc mvc = MockMvcBuilders
        .standaloneSetup(new OperacionesController(servicio, identidad)).setControllerAdvice(new Errores()).build();

    @Test
    void devuelveConfiguracionYRespetaContrato() throws Exception {
        when(identidad.autenticar(token)).thenReturn(observador);
        when(servicio.configuracion()).thenReturn(new Configuracion(LocalTime.of(8, 0), LocalTime.of(22, 0), 30, 60, "UTC"));
        mvc.perform(get(ruta + "/configuracion").header("Authorization", token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.minimo").value(30))
            .andExpect(jsonPath("$.zona").value("UTC"));
        when(servicio.sesiones(observador, true)).thenReturn(List.of());
        mvc.perform(get(ruta + "/sesiones?historial=true").header("Authorization", token))
            .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void rechazaSolicitudInvalidaYUsuarioSinSesion() throws Exception {
        when(identidad.autenticar(null)).thenThrow(new ErrorOperacion(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión"));
        mvc.perform(get(ruta + "/sesiones")).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Debes iniciar sesión"));
        mvc.perform(post(ruta + "/sesiones").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }
}
