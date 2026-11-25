package cl.duocuc.rinconsur.operations;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@Tag(name = "Operaciones", description = "Gestión de accesos, horarios, sesiones de observación y estadísticas.")
@RestController
@RequestMapping("/api/v1/operations")
public class OperacionesController {

    private final OperacionesService operaciones;
    private final Identidad identidad;

    public OperacionesController(OperacionesService operaciones, Identidad identidad) {
        this.operaciones = operaciones;
        this.identidad = identidad;
    }

    @Operation(
            summary = "Listar accesos",
            description = "Muestra los accesos disponibles. El administrador también puede ver los inactivos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de accesos disponible."),
            @ApiResponse(responseCode = "401", description = "Sesión ausente o inválida."),
            @ApiResponse(responseCode = "503", description = "El servicio de identidad no está disponible.")
    })
    @GetMapping("/accesos")
    public List<Acceso> accesos(
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        Usuario usuario = identidad.autenticar(token);
        return operaciones.accesos(usuario);
    }

    @Operation(
            summary = "Crear acceso",
            description = "Registra un nuevo punto de acceso para las sesiones de observación.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Acceso creado."),
            @ApiResponse(responseCode = "400", description = "Los datos de la solicitud son inválidos."),
            @ApiResponse(responseCode = "401", description = "Sesión ausente o inválida."),
            @ApiResponse(responseCode = "403", description = "Se requiere el rol de administrador."),
            @ApiResponse(responseCode = "409", description = "Ya existe un acceso con ese nombre."),
            @ApiResponse(responseCode = "503", description = "El servicio de identidad no está disponible.")
    })
    @PostMapping("/accesos")
    @ResponseStatus(HttpStatus.CREATED)
    public Acceso crearAcceso(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody AccesoRequest datos
    ) {
        Usuario usuario = identidad.autenticar(token);
        return operaciones.guardarAcceso(usuario, null, datos);
    }

    @Operation(
            summary = "Editar acceso",
            description = "Cambia el nombre y el estado de un acceso existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acceso actualizado."),
            @ApiResponse(responseCode = "400", description = "Los datos de la solicitud son inválidos."),
            @ApiResponse(responseCode = "401", description = "Sesión ausente o inválida."),
            @ApiResponse(responseCode = "403", description = "Se requiere el rol de administrador."),
            @ApiResponse(responseCode = "404", description = "No se encontró el acceso."),
            @ApiResponse(responseCode = "409", description = "Ya existe otro acceso con ese nombre."),
            @ApiResponse(responseCode = "503", description = "El servicio de identidad no está disponible.")
    })
    @PutMapping("/accesos/{id}")
    public Acceso editarAcceso(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Parameter(description = "Identificador del acceso.")
            @PathVariable UUID id,
            @Valid @RequestBody AccesoRequest datos
    ) {
        Usuario usuario = identidad.autenticar(token);
        return operaciones.guardarAcceso(usuario, id, datos);
    }

    @Operation(
            summary = "Consultar jornada",
            description = "Muestra el horario de apertura y cierre y la duración permitida de las sesiones.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuración de jornada disponible."),
            @ApiResponse(responseCode = "401", description = "Sesión ausente o inválida."),
            @ApiResponse(responseCode = "503", description = "El servicio de identidad no está disponible.")
    })
    @GetMapping("/configuracion")
    public Configuracion configuracion(
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        identidad.autenticar(token);
        return operaciones.configuracion();
    }

    @Operation(
            summary = "Actualizar jornada",
            description = "Cambia el horario laboral y los límites de duración de las sesiones.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuración de jornada actualizada."),
            @ApiResponse(responseCode = "400", description = "El horario o los límites de duración son inválidos."),
            @ApiResponse(responseCode = "401", description = "Sesión ausente o inválida."),
            @ApiResponse(responseCode = "403", description = "Se requiere el rol de administrador."),
            @ApiResponse(responseCode = "503", description = "El servicio de identidad no está disponible.")
    })
    @PutMapping("/configuracion")
    public Configuracion configurar(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody HorarioRequest datos
    ) {
        Usuario usuario = identidad.autenticar(token);
        return operaciones.configurar(usuario, datos);
    }

    @Operation(
            summary = "Listar observadores activos",
            description = "Muestra los observadores activos que pueden asignarse a una sesión.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de observadores disponible."),
            @ApiResponse(responseCode = "401", description = "Sesión ausente o inválida."),
            @ApiResponse(responseCode = "403", description = "Se requiere el rol de supervisor o administrador."),
            @ApiResponse(responseCode = "503", description = "El servicio de identidad no está disponible.")
    })
    @GetMapping("/observadores")
    public List<Observador> observadores(
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        Usuario usuario = identidad.autenticar(token);
        return operaciones.observadores(usuario);
    }

    @Operation(
            summary = "Listar sesiones",
            description = "Muestra las sesiones visibles según el rol. Un observador solo ve las que tiene asignadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de sesiones disponible."),
            @ApiResponse(responseCode = "400", description = "El filtro de historial es inválido."),
            @ApiResponse(responseCode = "401", description = "Sesión ausente o inválida."),
            @ApiResponse(responseCode = "503", description = "El servicio de identidad no está disponible.")
    })
    @GetMapping("/sesiones")
    public List<Sesion> sesiones(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Parameter(description = "Si es verdadero, muestra solo las sesiones finalizadas o canceladas.")
            @RequestParam(defaultValue = "false") boolean historial
    ) {
        Usuario usuario = identidad.autenticar(token);
        return operaciones.sesiones(usuario, historial);
    }

    @Operation(
            summary = "Programar sesión",
            description = "Agenda una sesión para un observador en un acceso y horario determinados.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sesión programada."),
            @ApiResponse(responseCode = "400", description = "El horario, el acceso o el observador son inválidos."),
            @ApiResponse(responseCode = "401", description = "Sesión ausente o inválida."),
            @ApiResponse(responseCode = "403", description = "Se requiere el rol de supervisor."),
            @ApiResponse(responseCode = "409", description = "El observador tiene otra sesión que coincide o queda muy cerca."),
            @ApiResponse(responseCode = "503", description = "El servicio de identidad no está disponible.")
    })
    @PostMapping("/sesiones")
    @ResponseStatus(HttpStatus.CREATED)
    public Sesion programar(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody Programacion datos
    ) {
        Usuario usuario = identidad.autenticar(token);
        return operaciones.programar(usuario, null, datos);
    }

    @Operation(
            summary = "Reprogramar sesión",
            description = "Cambia el acceso, el observador o el horario de una sesión programada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión reprogramada."),
            @ApiResponse(responseCode = "400", description = "Los datos de la programación son inválidos."),
            @ApiResponse(responseCode = "401", description = "Sesión ausente o inválida."),
            @ApiResponse(responseCode = "403", description = "Se requiere el rol de supervisor o administrador."),
            @ApiResponse(responseCode = "404", description = "No se encontró la sesión."),
            @ApiResponse(responseCode = "409", description = "El estado de la sesión o el horario impiden reprogramarla."),
            @ApiResponse(responseCode = "503", description = "El servicio de identidad no está disponible.")
    })
    @PutMapping("/sesiones/{id}")
    public Sesion reprogramar(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Parameter(description = "Identificador de la sesión.")
            @PathVariable UUID id,
            @Valid @RequestBody Programacion datos
    ) {
        Usuario usuario = identidad.autenticar(token);
        return operaciones.programar(usuario, id, datos);
    }

    @Operation(
            summary = "Cambiar estado de sesión",
            description = "Inicia, finaliza o cancela una sesión según el rol y su estado actual.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado de la sesión actualizado."),
            @ApiResponse(responseCode = "400", description = "El identificador o el estado solicitado son inválidos."),
            @ApiResponse(responseCode = "401", description = "Sesión ausente o inválida."),
            @ApiResponse(responseCode = "403", description = "El rol no permite realizar esta operación."),
            @ApiResponse(responseCode = "404", description = "No se encontró la sesión o no está asignada al observador."),
            @ApiResponse(responseCode = "409", description = "El estado actual o el horario impiden realizar el cambio."),
            @ApiResponse(responseCode = "503", description = "El servicio de identidad no está disponible.")
    })
    @PutMapping("/sesiones/{id}/estado/{estado}")
    public Sesion transicion(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Parameter(description = "Identificador de la sesión.")
            @PathVariable UUID id,
            @Parameter(description = "Estado de destino: EN_CURSO, FINALIZADA o CANCELADA.")
            @PathVariable Estado estado
    ) {
        Usuario usuario = identidad.autenticar(token);
        return operaciones.transicion(usuario, id, estado);
    }

    @Operation(
            summary = "Registrar un conteo",
            description = "Agrega un registro por categoría a una sesión en curso y devuelve sus totales actualizados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión con sus conteos actualizados."),
            @ApiResponse(responseCode = "400", description = "Los datos de la solicitud son inválidos."),
            @ApiResponse(responseCode = "401", description = "Sesión ausente o inválida."),
            @ApiResponse(responseCode = "403", description = "Se requiere el rol de observador."),
            @ApiResponse(responseCode = "404", description = "No se encontró la sesión o no está asignada al observador."),
            @ApiResponse(responseCode = "409", description = "La sesión no está en curso o el identificador del registro ya está en uso."),
            @ApiResponse(responseCode = "503", description = "El servicio de identidad no está disponible.")
    })
    @PostMapping("/sesiones/{id}/registros")
    public Sesion registrar(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Parameter(description = "Identificador de la sesión.")
            @PathVariable UUID id,
            @Valid @RequestBody RegistroRequest datos
    ) {
        Usuario usuario = identidad.autenticar(token);
        return operaciones.registrar(usuario, id, datos);
    }

    @Operation(
            summary = "Consultar auditoría",
            description = "Muestra las acciones recientes sobre accesos, horarios y sesiones.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actividad reciente disponible."),
            @ApiResponse(responseCode = "401", description = "Sesión ausente o inválida."),
            @ApiResponse(responseCode = "403", description = "Se requiere el rol de administrador."),
            @ApiResponse(responseCode = "503", description = "El servicio de identidad no está disponible.")
    })
    @GetMapping("/auditoria")
    public List<Actividad> auditoria(
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        Usuario usuario = identidad.autenticar(token);
        return operaciones.actividad(usuario);
    }

    @Operation(
            summary = "Consultar tablero de operaciones",
            description = "Muestra un resumen de las sesiones y sus conteos según el rol del usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen de operaciones disponible."),
            @ApiResponse(responseCode = "401", description = "Sesión ausente o inválida."),
            @ApiResponse(responseCode = "503", description = "El servicio de identidad no está disponible.")
    })
    @GetMapping("/dashboard")
    public Dashboard dashboard(
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        Usuario usuario = identidad.autenticar(token);
        return operaciones.dashboard(usuario);
    }
}
