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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/operations")
public class OperacionesController {

    private final OperacionesService operaciones;
    private final Identidad identidad;

    public OperacionesController(OperacionesService operaciones, Identidad identidad) {
        this.operaciones = operaciones;
        this.identidad = identidad;
    }

    @GetMapping("/accesos")
    public List<Acceso> accesos(
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        Usuario usuario = identidad.autenticar(token);
        return operaciones.accesos(usuario);
    }

    @PostMapping("/accesos")
    @ResponseStatus(HttpStatus.CREATED)
    public Acceso crearAcceso(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody AccesoRequest datos
    ) {
        Usuario usuario = identidad.autenticar(token);
        return operaciones.guardarAcceso(usuario, null, datos);
    }

    @PutMapping("/accesos/{id}")
    public Acceso editarAcceso(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable UUID id,
            @Valid @RequestBody AccesoRequest datos
    ) {
        Usuario usuario = identidad.autenticar(token);
        return operaciones.guardarAcceso(usuario, id, datos);
    }

    @GetMapping("/configuracion")
    public Configuracion configuracion(
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        identidad.autenticar(token);
        return operaciones.configuracion();
    }

    @PutMapping("/configuracion")
    public Configuracion configurar(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody HorarioRequest datos
    ) {
        Usuario usuario = identidad.autenticar(token);
        return operaciones.configurar(usuario, datos);
    }

    @GetMapping("/observadores")
    public List<Observador> observadores(
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        Usuario usuario = identidad.autenticar(token);
        return operaciones.observadores(usuario);
    }

    @GetMapping("/sesiones")
    public List<Sesion> sesiones(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(defaultValue = "false") boolean historial
    ) {
        Usuario usuario = identidad.autenticar(token);
        return operaciones.sesiones(usuario, historial);
    }

    @PostMapping("/sesiones")
    @ResponseStatus(HttpStatus.CREATED)
    public Sesion programar(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody Programacion datos
    ) {
        Usuario usuario = identidad.autenticar(token);
        return operaciones.programar(usuario, null, datos);
    }

    @PutMapping("/sesiones/{id}")
    public Sesion reprogramar(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable UUID id,
            @Valid @RequestBody Programacion datos
    ) {
        Usuario usuario = identidad.autenticar(token);
        return operaciones.programar(usuario, id, datos);
    }

    @PutMapping("/sesiones/{id}/estado/{estado}")
    public Sesion transicion(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable UUID id,
            @PathVariable Estado estado
    ) {
        Usuario usuario = identidad.autenticar(token);
        return operaciones.transicion(usuario, id, estado);
    }

    @PostMapping("/sesiones/{id}/registros")
    public Sesion registrar(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable UUID id,
            @Valid @RequestBody RegistroRequest datos
    ) {
        Usuario usuario = identidad.autenticar(token);
        return operaciones.registrar(usuario, id, datos);
    }

    @GetMapping("/auditoria")
    public List<Actividad> auditoria(
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        Usuario usuario = identidad.autenticar(token);
        return operaciones.actividad(usuario);
    }

    @GetMapping("/dashboard")
    public Dashboard dashboard(
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        Usuario usuario = identidad.autenticar(token);
        return operaciones.dashboard(usuario);
    }
}
