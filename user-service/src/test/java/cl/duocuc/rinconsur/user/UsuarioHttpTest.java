package cl.duocuc.rinconsur.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import cl.duocuc.rinconsur.user.config.AccesoInternoInterceptor;
import cl.duocuc.rinconsur.user.controller.UsuarioController;
import cl.duocuc.rinconsur.user.data.entity.UsuarioEntity;
import cl.duocuc.rinconsur.user.data.repository.AuditoriaUsuarioRepository;
import cl.duocuc.rinconsur.user.data.repository.UsuarioRepository;
import cl.duocuc.rinconsur.user.exception.UsuarioExceptionHandler;
import cl.duocuc.rinconsur.user.model.Rol;
import cl.duocuc.rinconsur.user.service.UsuarioService;

class UsuarioHttpTest {
    private static final String RUTA = "/internal/users";
    private static final String TOKEN = "token-interno-exclusivo-para-pruebas";
    private static final String DATOS = """
            {"nombre":" Eva ","correo":"Eva@example.org","rol":"OBSERVADOR","activo":true}
            """;

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final AuditoriaUsuarioRepository auditoria = mock(AuditoriaUsuarioRepository.class);
    private final UsuarioEntity eva = new UsuarioEntity(7L, "Eva", "eva@example.org", Rol.OBSERVADOR,
            true, "", "", null, null);
    private final org.springframework.test.web.servlet.MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new UsuarioController(new UsuarioService(usuarios, auditoria)))
            .setControllerAdvice(new UsuarioExceptionHandler())
            .addInterceptors(new AccesoInternoInterceptor(TOKEN))
            .defaultRequest(get(RUTA).header("X-Service-Token", TOKEN))
            .build();

    @Test
    void rechazaSolicitudesConTokenInternoIncorrecto() throws Exception {
        var protegido = MockMvcBuilders.standaloneSetup(new UsuarioController(new UsuarioService(usuarios, auditoria)))
                .addInterceptors(new AccesoInternoInterceptor(TOKEN)).build();
        protegido.perform(get(RUTA).header("X-Service-Token", "incorrecto"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Acceso interno no autorizado"));
        verifyNoInteractions(usuarios, auditoria);
    }

    @Test
    void listaYBuscaUsuariosConPaginacionPredeterminada() throws Exception {
        var primeraPagina = PageRequest.of(0, 20, Sort.by("id"));
        when(usuarios.findAll(primeraPagina)).thenReturn(new PageImpl<>(List.of(eva), primeraPagina, 1));
        when(usuarios.findById(7L)).thenReturn(Optional.of(eva));
        when(usuarios.findByCorreo("eva@example.org")).thenReturn(eva);

        mvc.perform(get(RUTA)).andExpect(status().isOk()).andExpect(jsonPath("$.totalUsuarios").value(1))
                .andExpect(jsonPath("$.usuarios[0].id").value(7));
        mvc.perform(get(RUTA + "/7")).andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Eva"));
        mvc.perform(get(RUTA + "/por-correo").param("correo", "Eva@example.org"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7));
        mvc.perform(get(RUTA + "/99")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No se encontró el usuario"));
    }

    @Test
    void registraAlActorEnLasModificacionesYExponeLaActividad() throws Exception {
        var actor = new UsuarioEntity(3L, "Ana", "ana@example.org", Rol.ADMINISTRADOR, true, "", "", null, null);
        when(usuarios.findById(3L)).thenReturn(Optional.of(actor));
        when(usuarios.findById(7L)).thenReturn(Optional.of(eva));
        when(usuarios.saveAndFlush(any())).thenReturn(eva);
        var eventos = new java.util.ArrayList<cl.duocuc.rinconsur.user.data.entity.AuditoriaUsuarioEntity>();
        when(auditoria.save(any())).thenAnswer(invocacion -> {
            cl.duocuc.rinconsur.user.data.entity.AuditoriaUsuarioEntity evento = invocacion.getArgument(0);
            eventos.add(new cl.duocuc.rinconsur.user.data.entity.AuditoriaUsuarioEntity(
                    (long) eventos.size() + 1, evento.getActorId(), evento.getActor(), evento.getAccion(),
                    evento.getReferencia(), evento.getFecha()));
            return evento;
        });
        when(auditoria.findTop100ByOrderByFechaDescIdDesc()).thenAnswer(invocacion -> eventos.reversed());

        mvc.perform(post(RUTA).header("X-Actor-Id", "3").contentType("application/json").content(DATOS))
                .andExpect(status().isCreated()).andExpect(header().string("Location", RUTA + "/7"));
        mvc.perform(put(RUTA + "/7").header("X-Actor-Id", "3").contentType("application/json")
                .content(DATOS.replace(" Eva ", " Eva Rojas ")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Eva Rojas"));
        mvc.perform(patch(RUTA + "/7/estado").header("X-Actor-Id", "3").contentType("application/json")
                .content("{\"activo\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
        mvc.perform(get(RUTA + "/auditoria")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accion").value("USUARIO_DESACTIVADO"))
                .andExpect(jsonPath("$[0].actor").value("Ana"))
                .andExpect(jsonPath("$[0].referencia").value("7"))
                .andExpect(jsonPath("$[1].accion").value("USUARIO_ACTUALIZADO"))
                .andExpect(jsonPath("$[2].accion").value("USUARIO_CREADO"));
    }

    @Test
    void validaElPerfilYTraduceLosErroresDeEscritura() throws Exception {
        when(usuarios.findById(7L)).thenReturn(Optional.of(eva));
        mvc.perform(get(RUTA + "/7/perfil")).andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("eva@example.org"));
        mvc.perform(put(RUTA + "/7/perfil").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.correo").isString());
        mvc.perform(patch(RUTA + "/7/estado").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(RUTA + "/texto")).andExpect(status().isBadRequest());

        when(usuarios.existsByCorreo("eva@example.org")).thenReturn(true);
        mvc.perform(post(RUTA).contentType("application/json").content(DATOS))
                .andExpect(status().isConflict());
        when(usuarios.saveAndFlush(any())).thenThrow(new ObjectOptimisticLockingFailureException(UsuarioEntity.class, 7L));
        mvc.perform(put(RUTA + "/7/perfil").contentType("application/json")
                .content("{\"nombre\":\"Eva\",\"correo\":\"eva@example.org\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El usuario fue modificado por otra solicitud. Vuelve a consultarlo"));
    }
}
