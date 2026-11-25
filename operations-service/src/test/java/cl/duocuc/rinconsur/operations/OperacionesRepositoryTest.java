package cl.duocuc.rinconsur.operations;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class OperacionesRepositoryTest {
    private static final Usuario EVA = new Usuario(7, "Eva", Rol.OBSERVADOR, true);
    private static final OffsetDateTime INICIO = OffsetDateTime.parse("2026-05-10T09:00:00-04:00");
    private static final OffsetDateTime FIN = INICIO.plusHours(1);
    private SingleConnectionDataSource base;
    private OperacionesRepository repositorio;

    @BeforeEach
    void prepararBase() {
        base = new SingleConnectionDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "", true);
        new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V1__operaciones.sql"),
                new ClassPathResource("db/migration/V2__auditoria_operaciones.sql"),
                new ClassPathResource("db/migration/V3__limites_jornada.sql"))
                .execute(base);
        repositorio = new OperacionesRepository(JdbcClient.create(base));
    }

    @AfterEach
    void cerrarBase() {
        if (base != null) {
            base.destroy();
        }
    }

    @Test
    void persisteLaConfiguracionYFiltraAccesosActivosEnOrden() {
        var horario = new HorarioRequest(LocalTime.of(7, 0), LocalTime.of(19, 0), 25, 90);
        repositorio.actualizarHorario(horario);
        assertThat(repositorio.configuracion("America/Santiago", true))
                .isEqualTo(new Configuracion(horario.apertura(), horario.cierre(), 25, 90, "America/Santiago"));

        var sur = new Acceso(UUID.randomUUID(), "Sur", true);
        var norte = new Acceso(UUID.randomUUID(), "Norte", true);
        assertThat(repositorio.guardarAcceso(sur, "sur", true)).isEqualTo(1);
        assertThat(repositorio.guardarAcceso(norte, "norte", true)).isEqualTo(1);
        assertThat(repositorio.accesos(true)).containsExactly(norte, sur);

        var cerrado = new Acceso(norte.id(), norte.nombre(), false);
        assertThat(repositorio.guardarAcceso(cerrado, "norte", false)).isEqualTo(1);
        assertThat(repositorio.accesoActivo(norte.id())).isFalse();
        assertThat(repositorio.accesos(true)).containsExactly(sur);
        assertThat(repositorio.accesos(false)).containsExactly(cerrado, sur);
    }

    @Test
    void detectaSolapamientosYExcluyeLaSesionEditadaOLasCanceladas() {
        var sesion = crearSesion(EVA);
        UUID otra = UUID.randomUUID();
        assertThat(repositorio.existeSuperposicion(EVA.id(), INICIO.plusMinutes(15), FIN, otra)).isTrue();
        assertThat(repositorio.existeSuperposicion(EVA.id(), INICIO, FIN, sesion.id())).isFalse();
        assertThat(repositorio.existeSuperposicion(EVA.id(), FIN, FIN.plusHours(1), otra)).isFalse();

        repositorio.guardarSesion(sesion.id(), sesion.accesoId(), EVA, FIN, FIN.plusHours(1), FIN, false);
        assertThat(repositorio.bloquearSesion(sesion.id()).inicio()).isEqualTo(FIN.withOffsetSameInstant(ZoneOffset.UTC));
        repositorio.actualizarEstado(sesion.id(), Estado.CANCELADA, FIN);
        assertThat(repositorio.existeSuperposicion(EVA.id(), FIN, FIN.plusHours(1), otra)).isFalse();
        assertThat(repositorio.sesiones(EVA.id(), true)).extracting(Sesion::estado).containsExactly(Estado.CANCELADA);
    }

    @Test
    void agregaConteosPorCategoriaYFiltraElHistorialPorObservador() {
        var sesion = crearSesion(EVA);
        var otra = crearSesion(new Usuario(8, "Ana", Rol.OBSERVADOR, true));
        assertThat(sesion.conteo()).isEqualTo(new Conteo(0, 0, 0));
        assertThat(sesion.inicio()).isEqualTo(INICIO.withOffsetSameInstant(ZoneOffset.UTC));

        var visita = new RegistroRequest(UUID.randomUUID(), Categoria.VISITA);
        repositorio.insertarRegistro(sesion.id(), visita);
        repositorio.insertarRegistro(sesion.id(), new RegistroRequest(UUID.randomUUID(), Categoria.TRANSITO));
        repositorio.insertarRegistro(sesion.id(), new RegistroRequest(UUID.randomUUID(), Categoria.VISITA));
        assertThat(repositorio.registro(visita.id()))
                .isEqualTo(new OperacionesRepository.RegistroGuardado(sesion.id(), Categoria.VISITA));
        assertThat(repositorio.sesion(sesion.id()).conteo()).isEqualTo(new Conteo(1, 2, 0));
        assertThat(repositorio.sesiones(null, false)).extracting(Sesion::id)
                .containsExactlyInAnyOrder(sesion.id(), otra.id());
        assertThat(repositorio.sesiones(EVA.id(), true)).isEmpty();
    }

    @Test
    void distingueOcupacionEnCursoDeUnaFinalizacionFueraDeLaVentana() {
        var sesion = crearSesion(EVA);
        UUID otra = UUID.randomUUID();
        repositorio.actualizarEstado(sesion.id(), Estado.EN_CURSO, INICIO);
        assertThat(repositorio.existeOcupacionReciente(EVA.id(), otra, INICIO, FIN)).isTrue();
        assertThat(repositorio.existeOcupacionReciente(EVA.id(), sesion.id(), INICIO, FIN)).isFalse();

        repositorio.actualizarEstado(sesion.id(), Estado.FINALIZADA, FIN);
        assertThat(repositorio.existeOcupacionReciente(EVA.id(), otra, INICIO, FIN)).isTrue();
        assertThat(repositorio.existeOcupacionReciente(EVA.id(), otra, FIN, FIN.plusHours(1))).isFalse();
    }

    private Sesion crearSesion(Usuario observador) {
        var acceso = new Acceso(UUID.randomUUID(), "Acceso " + observador.id(), true);
        repositorio.guardarAcceso(acceso, "acceso " + observador.id(), true);
        UUID id = UUID.randomUUID();
        repositorio.guardarSesion(id, acceso.id(), observador, INICIO, FIN, INICIO, true);
        return repositorio.sesion(id);
    }
}
