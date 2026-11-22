package cl.duocuc.rinconsur.operations;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperacionesService {

    private static final int PAUSA_MINUTOS = 10;

    private final OperacionesRepository datos;
    private final Identidad identidad;
    private final AuditoriaOperaciones auditoria;
    private final ZoneId calendario;
    private final Clock reloj;

    @Autowired
    public OperacionesService(
            OperacionesRepository datos,
            Identidad identidad,
            AuditoriaOperaciones auditoria,
            @Value("${tienda.zona}") String zona
    ) {
        this(datos, identidad, auditoria, zona, Clock.systemUTC());
    }

    OperacionesService(
            OperacionesRepository datos,
            Identidad identidad,
            AuditoriaOperaciones auditoria,
            String zona,
            Clock reloj
    ) {
        this.datos = datos;
        this.identidad = identidad;
        this.auditoria = auditoria;
        calendario = ZoneId.of(zona);
        this.reloj = reloj.withZone(calendario);
    }

    public Configuracion configuracion() {
        return datos.configuracion(calendario.getId(), false);
    }

    public List<Acceso> accesos(Usuario usuario) {
        return datos.accesos(usuario.rol() != Rol.ADMINISTRADOR);
    }

    public List<Observador> observadores(Usuario usuario) {
        exigir(usuario, Rol.SUPERVISOR, Rol.ADMINISTRADOR);

        return identidad.usuariosActivos()
                .stream()
                .filter(usuarioActivo -> usuarioActivo.rol() == Rol.OBSERVADOR)
                .sorted(Comparator.comparing(Usuario::nombre))
                .map(usuarioActivo -> new Observador(usuarioActivo.id(), usuarioActivo.nombre()))
                .toList();
    }

    @Transactional
    public Configuracion configurar(Usuario usuario, HorarioRequest horario) {
        exigir(usuario, Rol.ADMINISTRADOR);

        if (horario.apertura().getSecond() != 0
                || horario.apertura().getNano() != 0
                || horario.cierre().getSecond() != 0
                || horario.cierre().getNano() != 0) {
            throw invalido("Indica la apertura y el cierre con horas y minutos");
        }
        if (!horario.cierre().isAfter(horario.apertura())) {
            throw invalido("El cierre debe ser posterior a la apertura en el mismo día");
        }
        if (horario.maximo() < horario.minimo()) {
            throw invalido("La duración máxima debe ser igual o superior a la mínima");
        }
        if (horario.maximo() > Duration.between(horario.apertura(), horario.cierre()).toMinutes()) {
            throw invalido("La duración máxima debe caber dentro de la jornada laboral");
        }

        datos.configuracion(calendario.getId(), true);
        datos.actualizarHorario(horario);
        auditoria.guardar(usuario, AccionAuditada.CONFIGURACION_ACTUALIZADA, null);
        return configuracion();
    }

    @Transactional
    public Acceso guardarAcceso(Usuario usuario, UUID id, AccesoRequest solicitud) {
        exigir(usuario, Rol.ADMINISTRADOR);

        String nombre = solicitud.nombre().strip();
        String normalizado = nombre.toLowerCase(Locale.ROOT);
        if (nombre.isBlank() || normalizado.length() > 80) {
            throw invalido("El nombre del acceso es inválido");
        }
        if (id != null && solicitud.activo() == null) {
            throw invalido("Indica si el acceso está activo");
        }
        Acceso acceso = new Acceso(
                id == null ? UUID.randomUUID() : id,
                nombre,
                solicitud.activo() == null || solicitud.activo()
        );

        datos.configuracion(calendario.getId(), true);
        if (datos.guardarAcceso(acceso, normalizado, id == null) == 0) {
            throw noExiste("Acceso inexistente");
        }

        AccionAuditada accion = id == null ? AccionAuditada.ACCESO_CREADO : AccionAuditada.ACCESO_ACTUALIZADO;
        auditoria.guardar(usuario, accion, acceso.id());
        return acceso;
    }

    @Transactional
    public Sesion programar(Usuario usuario, UUID id, Programacion solicitud) {
        if (id == null) {
            exigir(usuario, Rol.SUPERVISOR);
        } else {
            exigir(usuario, Rol.SUPERVISOR, Rol.ADMINISTRADOR);
        }

        Usuario observador = identidad.observador(solicitud.observadorId());
        Configuracion horario = datos.configuracion(calendario.getId(), true);
        if (id != null) {
            comprobarEstado(bloquear(id).estado(), Estado.PROGRAMADA);
        }
        if (!datos.accesoActivo(solicitud.accesoId())) {
            throw invalido("Selecciona un acceso activo");
        }

        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        OffsetDateTime inicio = convertirHora(solicitud.inicio());
        OffsetDateTime fin = convertirHora(solicitud.fin());
        validarHorario(horario, inicio, fin, ahora);

        UUID sesionId = id == null ? UUID.randomUUID() : id;
        if (hayCruceDeHorario(observador.id(), sesionId, inicio, fin)
                || datos.existeOcupacionReciente(observador.id(), sesionId, inicio.minusMinutes(PAUSA_MINUTOS), ahora)) {
            throw conflicto("El observador ya tiene una sesión en ese horario o a menos de 10 minutos. "
                    + "Deja al menos 10 minutos entre sesiones.");
        }

        datos.guardarSesion(sesionId, solicitud.accesoId(), observador, inicio, fin, ahora, id == null);
        AccionAuditada accion = id == null ? AccionAuditada.SESION_PROGRAMADA : AccionAuditada.SESION_REPROGRAMADA;
        auditoria.guardar(usuario, accion, sesionId);
        return datos.sesion(sesionId);
    }

    public List<Sesion> sesiones(Usuario usuario, boolean historial) {
        return datos.sesiones(usuario.rol() == Rol.OBSERVADOR ? usuario.id() : null, historial);
    }

    @Transactional
    public Sesion transicion(Usuario usuario, UUID id, Estado destino) {
        AccionAuditada accion = switch (destino) {
            case CANCELADA -> {
                exigir(usuario, Rol.SUPERVISOR, Rol.ADMINISTRADOR);
                datos.configuracion(calendario.getId(), true);
                comprobarEstado(bloquear(id).estado(), Estado.PROGRAMADA);
                yield AccionAuditada.SESION_CANCELADA;
            }
            case EN_CURSO, FINALIZADA -> {
                exigir(usuario, Rol.OBSERVADOR);
                // Usa el mismo bloqueo que la programación antes de leer o cambiar una sesión.
                datos.configuracion(calendario.getId(), true);
                OperacionesRepository.SesionBloqueada sesion = bloquearPropia(usuario, id);
                if (destino == Estado.FINALIZADA) {
                    comprobarEstado(sesion.estado(), Estado.EN_CURSO);
                    yield AccionAuditada.SESION_FINALIZADA;
                }
                comprobarEstado(sesion.estado(), Estado.PROGRAMADA);
                OffsetDateTime ahora = OffsetDateTime.now(reloj);
                if (datos.existeOcupacionReciente(usuario.id(), id, ahora.minusMinutes(PAUSA_MINUTOS), ahora)) {
                    throw conflicto("Tienes otra sesión en curso o debes esperar 10 minutos desde que terminó.");
                }
                if (hayCruceDeHorario(usuario.id(), id, sesion.inicio(), sesion.fin())) {
                    throw conflicto("El horario cruza otra sesión asignada o deja menos de 10 minutos entre ellas.");
                }
                yield AccionAuditada.SESION_INICIADA;
            }
            case PROGRAMADA -> throw invalido("Para programar usa el formulario de sesiones");
        };

        datos.actualizarEstado(id, destino, OffsetDateTime.now(reloj));
        auditoria.guardar(usuario, accion, id);
        return datos.sesion(id);
    }

    @Transactional
    public Sesion registrar(Usuario usuario, UUID id, RegistroRequest registro) {
        exigir(usuario, Rol.OBSERVADOR);
        Estado estado = bloquearPropia(usuario, id).estado();
        OperacionesRepository.RegistroGuardado anterior = datos.registro(registro.id());
        if (anterior != null) {
            if (!anterior.sesionId().equals(id) || anterior.categoria() != registro.categoria()) {
                throw conflicto("El identificador del registro ya está en uso");
            }
        } else {
            comprobarEstado(estado, Estado.EN_CURSO);
            datos.insertarRegistro(id, registro);
            datos.actualizarEstado(id, estado, OffsetDateTime.now(reloj));
        }
        return datos.sesion(id);
    }

    public List<Actividad> actividad(Usuario usuario) {
        exigir(usuario, Rol.ADMINISTRADOR);
        return auditoria.recientes();
    }

    public Dashboard dashboard(Usuario usuario) {
        List<Sesion> sesiones = sesiones(usuario, false);
        Map<Estado, Integer> estados = sesiones.stream()
                .collect(Collectors.groupingBy(Sesion::estado, Collectors.summingInt(sesion -> 1)));

        boolean administrador = usuario.rol() == Rol.ADMINISTRADOR;
        boolean supervisor = usuario.rol() == Rol.SUPERVISOR;
        List<Usuario> activos = administrador ? identidad.usuariosActivos() : List.of();

        return new Dashboard(
                sesiones.size(),
                estados.getOrDefault(Estado.PROGRAMADA, 0),
                estados.getOrDefault(Estado.EN_CURSO, 0),
                estados.getOrDefault(Estado.FINALIZADA, 0),
                estados.getOrDefault(Estado.CANCELADA, 0),
                total(sesiones),
                supervisor ? agrupar(sesiones, sesion -> sesion.accesoId().toString(), Sesion::acceso) : List.of(),
                supervisor ? agrupar(sesiones, sesion -> Long.toString(sesion.observadorId()), Sesion::observador) : List.of(),
                administrador ? activos.size() : null,
                administrador ? (int) activos.stream().filter(activo -> activo.rol() == Rol.OBSERVADOR).count() : null,
                administrador ? (int) activos.stream().filter(activo -> activo.rol() == Rol.SUPERVISOR).count() : null,
                administrador ? datos.accesos(true).size() : null,
                administrador ? sesiones.stream()
                        .sorted(Comparator.comparing(Sesion::actualizada).reversed())
                        .limit(10)
                        .toList() : List.of()
        );
    }

    private List<Estadistica> agrupar(
            List<Sesion> sesiones,
            Function<Sesion, String> id,
            Function<Sesion, String> nombre
    ) {
        Map<String, List<Sesion>> grupos = sesiones.stream()
                .collect(Collectors.groupingBy(id));

        return grupos.entrySet()
                .stream()
                .map(grupo -> new Estadistica(
                        grupo.getKey(),
                        nombre.apply(grupo.getValue().getFirst()),
                        grupo.getValue().size(),
                        total(grupo.getValue())
                ))
                .sorted(Comparator.comparing(Estadistica::nombre))
                .toList();
    }

    private Conteo total(List<Sesion> sesiones) {
        return sesiones.stream()
                .map(Sesion::conteo)
                .reduce(new Conteo(), Conteo::plus);
    }

    private OffsetDateTime convertirHora(LocalDateTime hora) {
        List<ZoneOffset> desfases = calendario.getRules().getValidOffsets(hora);
        if (desfases.size() != 1) {
            throw invalido("La hora elegida es ambigua o inexistente en la zona de la tienda");
        }
        return hora.atOffset(desfases.getFirst());
    }

    private boolean hayCruceDeHorario(long observadorId, UUID id, OffsetDateTime inicio, OffsetDateTime fin) {
        OffsetDateTime inicioConPausa = inicio.minusMinutes(PAUSA_MINUTOS);
        OffsetDateTime finConPausa = fin.plusMinutes(PAUSA_MINUTOS);
        return datos.existeSuperposicion(observadorId, inicioConPausa, finConPausa, id);
    }

    private void validarHorario(Configuracion horario, OffsetDateTime inicio, OffsetDateTime fin, OffsetDateTime ahora) {
        if (!inicio.isAfter(ahora)) {
            throw invalido("La fecha y hora de inicio deben estar en el futuro");
        }
        if (!fin.isAfter(inicio)) {
            throw invalido("El fin debe ser posterior al inicio");
        }
        if (!inicio.toLocalDate().equals(fin.toLocalDate())
                || inicio.toLocalTime().isBefore(horario.apertura())
                || fin.toLocalTime().isAfter(horario.cierre())) {
            throw invalido("La sesión debe quedar dentro del horario laboral de "
                    + horario.apertura() + " a " + horario.cierre());
        }

        Duration duracion = Duration.between(inicio, fin);
        if (duracion.compareTo(Duration.ofMinutes(horario.minimo())) < 0) {
            throw invalido("La sesión no puede durar menos de " + horario.minimo() + " minutos");
        }
        if (duracion.compareTo(Duration.ofMinutes(horario.maximo())) > 0) {
            throw invalido("La sesión no puede durar más de " + horario.maximo() + " minutos");
        }
    }

    private OperacionesRepository.SesionBloqueada bloquear(UUID id) {
        OperacionesRepository.SesionBloqueada sesion = datos.bloquearSesion(id);
        if (sesion == null) {
            throw noExiste("Sesión inexistente");
        }
        return sesion;
    }

    private OperacionesRepository.SesionBloqueada bloquearPropia(Usuario usuario, UUID id) {
        OperacionesRepository.SesionBloqueada sesion = bloquear(id);
        if (sesion.observadorId() != usuario.id()) {
            throw noExiste("Sesión inexistente");
        }
        return sesion;
    }

    private void comprobarEstado(Estado actual, Estado requerido) {
        if (actual != requerido) {
            throw conflicto("La sesión está en estado " + actual);
        }
    }

    private void exigir(Usuario usuario, Rol... roles) {
        if (Arrays.stream(roles).noneMatch(rol -> rol == usuario.rol())) {
            throw new ErrorOperacion(HttpStatus.FORBIDDEN, "Tu rol no permite esta operación");
        }
    }

    private ErrorOperacion invalido(String mensaje) {
        return new ErrorOperacion(HttpStatus.BAD_REQUEST, mensaje);
    }

    private ErrorOperacion conflicto(String mensaje) {
        return new ErrorOperacion(HttpStatus.CONFLICT, mensaje);
    }

    private ErrorOperacion noExiste(String mensaje) {
        return new ErrorOperacion(HttpStatus.NOT_FOUND, mensaje);
    }
}
