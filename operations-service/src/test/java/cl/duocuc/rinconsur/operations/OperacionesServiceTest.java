package cl.duocuc.rinconsur.operations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class OperacionesServiceTest {
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-20T08:00:00Z");
    private final OperacionesRepository datos = mock(OperacionesRepository.class);
    private final Identidad identidad = mock(Identidad.class);
    private final AuditoriaOperaciones auditoria = mock(AuditoriaOperaciones.class);
    private final Clock reloj = Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC);
    private final OperacionesService servicio = new OperacionesService(datos, identidad, auditoria, "UTC", reloj);
    private final Usuario admin = new Usuario(1, "Admin", Rol.ADMINISTRADOR, true);
    private final Usuario supervisor = new Usuario(2, "Supervisor", Rol.SUPERVISOR, true);
    private final Usuario observador = new Usuario(3, "Observador", Rol.OBSERVADOR, true);
    private final LocalDate manana = LocalDate.now(reloj).plusDays(1);
    private final UUID acceso = UUID.randomUUID();
    private final UUID sesion = UUID.randomUUID();
    private final Configuracion jornada = new Configuracion(LocalTime.of(8, 0), LocalTime.of(22, 0), 30, 60, "UTC");

    @BeforeEach
    void preparar() {
        when(datos.configuracion(eq("UTC"), anyBoolean())).thenReturn(jornada);
        when(identidad.observador(observador.id())).thenReturn(observador);
        when(datos.accesoActivo(acceso)).thenReturn(true);
    }

    @Test
    void programaSesionDeTreintaMinutosYAudita() {
        var solicitud = programacion(8, 0, 8, 30);
        when(datos.sesion(any())).thenReturn(resumen(Estado.PROGRAMADA));
        assertEquals(Estado.PROGRAMADA, servicio.programar(supervisor, null, solicitud).estado());
        verify(datos).guardarSesion(any(), eq(acceso), eq(observador), any(), any(), any(), eq(true));
        verify(auditoria).guardar(eq(supervisor), eq(AccionAuditada.SESION_PROGRAMADA), any());
    }

    @Test
    void rechazaDuracionesFueraDelRango() {
        assertEquals("La sesión no puede durar menos de 30 minutos",
            assertThrows(ErrorOperacion.class, () -> servicio.programar(supervisor, null, programacion(8, 0, 8, 29))).getMessage());
        assertEquals("La sesión no puede durar más de 60 minutos",
            assertThrows(ErrorOperacion.class, () -> servicio.programar(supervisor, null, programacion(8, 0, 9, 1))).getMessage());
    }

    @Test
    void rechazaHorarioFueraDeJornadaYOrdenIncorrecto() {
        for (var solicitud : List.of(programacion(7, 59, 8, 30), programacion(21, 30, 22, 1),
                programacion(8, 30, 8, 0))) {
            assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ErrorOperacion.class, () -> servicio.programar(supervisor, null, solicitud)).getEstado());
        }
    }

    @Test
    void rechazaSuperposicionYAccesoInactivo() {
        when(datos.existeSuperposicion(anyLong(), any(), any(), any())).thenReturn(true);
        assertEquals(HttpStatus.CONFLICT,
            assertThrows(ErrorOperacion.class,
                () -> servicio.programar(supervisor, null, programacion(8, 0, 9, 0))).getEstado());
        when(datos.accesoActivo(acceso)).thenReturn(false);
        assertEquals(HttpStatus.BAD_REQUEST,
            assertThrows(ErrorOperacion.class,
                () -> servicio.programar(supervisor, null, programacion(8, 0, 9, 0))).getEstado());
    }

    @Test
    void rechazaReprogramarEnOtroAccesoCuandoElObservadorEstaOcupado() {
        UUID otroAcceso = UUID.randomUUID();
        Programacion solicitud = new Programacion(otroAcceso, observador.id(),
                manana.atTime(10, 0), manana.atTime(10, 40));
        when(datos.accesoActivo(otroAcceso)).thenReturn(true);
        when(datos.bloquearSesion(sesion)).thenReturn(bloqueada(Estado.PROGRAMADA));
        when(datos.existeSuperposicion(observador.id(), manana.atTime(9, 50).atOffset(ZoneOffset.UTC),
                manana.atTime(10, 50).atOffset(ZoneOffset.UTC), sesion)).thenReturn(true);

        ErrorOperacion error = assertThrows(ErrorOperacion.class,
                () -> servicio.programar(supervisor, sesion, solicitud));

        assertEquals(HttpStatus.CONFLICT, error.getEstado());
        verify(datos, never()).guardarSesion(any(), any(), any(), any(), any(), any(), anyBoolean());
        verifyNoInteractions(auditoria);
    }

    @Test
    void soloSupervisorProgramaYAdministradorPuedeEditar() {
        var solicitud = programacion(8, 0, 9, 0);
        assertEquals(HttpStatus.FORBIDDEN,
            assertThrows(ErrorOperacion.class, () -> servicio.programar(admin, null, solicitud)).getEstado());
        when(datos.bloquearSesion(sesion)).thenReturn(bloqueada(Estado.PROGRAMADA));
        servicio.programar(admin, sesion, solicitud);
        verify(auditoria).guardar(admin, AccionAuditada.SESION_REPROGRAMADA, sesion);
    }

    @Test
    void permiteTransicionesDeSesionPropiaYCancelacion() {
        when(datos.bloquearSesion(sesion)).thenReturn(
            bloqueada(Estado.PROGRAMADA), bloqueada(Estado.EN_CURSO), bloqueada(Estado.PROGRAMADA));
        servicio.transicion(observador, sesion, Estado.EN_CURSO);
        servicio.transicion(observador, sesion, Estado.FINALIZADA);
        servicio.transicion(supervisor, sesion, Estado.CANCELADA);
        verify(auditoria).guardar(supervisor, AccionAuditada.SESION_CANCELADA, sesion);
    }

    @Test
    void rechazaProgramarAntesDeDiezMinutosDeUnaOcupacionReciente() {
        OffsetDateTime inicio = AHORA.plusMinutes(9);
        Programacion solicitud = new Programacion(acceso, observador.id(),
                inicio.toLocalDateTime(), inicio.plusMinutes(40).toLocalDateTime());
        when(datos.existeOcupacionReciente(eq(observador.id()), any(), eq(inicio.minusMinutes(10)), eq(AHORA)))
                .thenReturn(true);

        ErrorOperacion error = assertThrows(ErrorOperacion.class,
                () -> servicio.programar(supervisor, null, solicitud));

        assertEquals(HttpStatus.CONFLICT, error.getEstado());
        verify(datos, never()).guardarSesion(any(), any(), any(), any(), any(), any(), anyBoolean());
        verifyNoInteractions(auditoria);
    }

    @Test
    void rechazaIniciarMientrasExisteElDescansoPendiente() {
        when(datos.bloquearSesion(sesion)).thenReturn(bloqueada(Estado.PROGRAMADA));
        when(datos.existeOcupacionReciente(observador.id(), sesion, AHORA.minusMinutes(10), AHORA))
                .thenReturn(true);

        ErrorOperacion error = assertThrows(ErrorOperacion.class,
                () -> servicio.transicion(observador, sesion, Estado.EN_CURSO));

        assertEquals(HttpStatus.CONFLICT, error.getEstado());
        verify(datos, never()).actualizarEstado(any(), any(), any());
        verifyNoInteractions(auditoria);
    }

    @Test
    void rechazaIniciarSiUnaReservaPreexistenteSeSuperpone() {
        OperacionesRepository.SesionBloqueada reserva = bloqueada(Estado.PROGRAMADA);
        when(datos.bloquearSesion(sesion)).thenReturn(reserva);
        when(datos.existeSuperposicion(observador.id(), reserva.inicio().minusMinutes(10),
                reserva.fin().plusMinutes(10), sesion)).thenReturn(true);

        ErrorOperacion error = assertThrows(ErrorOperacion.class,
                () -> servicio.transicion(observador, sesion, Estado.EN_CURSO));

        assertEquals(HttpStatus.CONFLICT, error.getEstado());
        verify(datos, never()).actualizarEstado(any(), any(), any());
        verifyNoInteractions(auditoria);
    }

    @Test
    void registroRepetidoNoSeCuentaDosVeces() {
        var registro = new RegistroRequest(UUID.randomUUID(), Categoria.TRANSITO);
        when(datos.bloquearSesion(sesion)).thenReturn(bloqueada(Estado.EN_CURSO));
        servicio.registrar(observador, sesion, registro);
        when(datos.registro(registro.id())).thenReturn(
            new OperacionesRepository.RegistroGuardado(sesion, Categoria.TRANSITO));
        servicio.registrar(observador, sesion, registro);
        verify(datos, times(1)).insertarRegistro(sesion, registro);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ErrorOperacion.class,
            () -> servicio.registrar(observador, sesion,
                new RegistroRequest(registro.id(), Categoria.VISITA))).getEstado());
    }

    @Test
    void filtraYResumeSegunRol() {
        when(identidad.usuariosActivos()).thenReturn(List.of(supervisor, observador, admin));
        when(datos.sesiones(null, false)).thenReturn(List.of(resumen(Estado.PROGRAMADA)));
        assertEquals(List.of(new Observador(3, "Observador")), servicio.observadores(supervisor));
        assertEquals(1, servicio.dashboard(supervisor).porAcceso().size());
        assertEquals(3, servicio.dashboard(admin).usuariosActivos());
        servicio.sesiones(observador, true);
        verify(datos).sesiones(3L, true);
    }

    @Test
    void administraAccesoConfiguracionYAuditoria() {
        assertEquals(jornada, servicio.configuracion());
        assertEquals(jornada, servicio.configurar(admin, new HorarioRequest(jornada.apertura(), jornada.cierre(), 30, 60)));
        when(datos.guardarAcceso(any(), eq("entrada"), eq(true))).thenReturn(1);
        assertEquals("Entrada", servicio.guardarAcceso(admin, null, new AccesoRequest(" Entrada ", null)).nombre());
        servicio.accesos(observador);
        servicio.actividad(admin);
        verify(datos).accesos(true);
        verify(auditoria).recientes();
    }


    private Programacion programacion(int inicio, int minutoInicio, int fin, int minutoFin) {
        return new Programacion(acceso, observador.id(), manana.atTime(inicio, minutoInicio), manana.atTime(fin, minutoFin));
    }

    private OperacionesRepository.SesionBloqueada bloqueada(Estado estado) {
        return new OperacionesRepository.SesionBloqueada(estado, observador.id(),
                manana.atTime(10, 0).atOffset(ZoneOffset.UTC), manana.atTime(10, 40).atOffset(ZoneOffset.UTC));
    }

    private Sesion resumen(Estado estado) {
        return new Sesion(sesion, acceso, "Entrada", observador.id(), observador.nombre(),
            AHORA, AHORA.plusMinutes(30), estado, AHORA, new Conteo());
    }
}
