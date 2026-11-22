package cl.duocuc.rinconsur.user.service;

import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duocuc.rinconsur.user.data.entity.AuditoriaUsuarioEntity;
import cl.duocuc.rinconsur.user.data.entity.UsuarioEntity;
import cl.duocuc.rinconsur.user.data.repository.AuditoriaUsuarioRepository;
import cl.duocuc.rinconsur.user.data.repository.UsuarioRepository;
import cl.duocuc.rinconsur.user.dto.ActividadUsuario;
import cl.duocuc.rinconsur.user.dto.PaginaUsuariosResponse;
import cl.duocuc.rinconsur.user.dto.PerfilRequest;
import cl.duocuc.rinconsur.user.dto.PerfilResponse;
import cl.duocuc.rinconsur.user.dto.UsuarioRequest;
import cl.duocuc.rinconsur.user.dto.UsuarioResponse;
import cl.duocuc.rinconsur.user.exception.CorreoRegistradoException;
import cl.duocuc.rinconsur.user.exception.UsuarioNoEncontradoException;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository repositorio;
    private final AuditoriaUsuarioRepository auditoria;

    public UsuarioService(UsuarioRepository repositorio, AuditoriaUsuarioRepository auditoria) {
        this.repositorio = repositorio;
        this.auditoria = auditoria;
    }

    public PaginaUsuariosResponse obtenerUsuarios(int pagina, int cantidad) {
        Page<UsuarioEntity> resultado = repositorio.findAll(PageRequest.of(pagina, cantidad, Sort.by("id")));
        List<UsuarioResponse> usuarios = resultado.map(this::respuesta).getContent();

        return new PaginaUsuariosResponse(
                usuarios,
                resultado.getNumber(),
                resultado.getTotalPages(),
                resultado.getTotalElements());
    }

    public UsuarioResponse obtenerUsuario(long id) {
        return respuesta(buscar(id));
    }

    public UsuarioResponse obtenerUsuarioPorCorreo(String correo) {
        UsuarioEntity usuario = repositorio.findByCorreo(normalizar(correo));
        if (usuario == null) {
            throw new UsuarioNoEncontradoException();
        }

        return respuesta(usuario);
    }

    @Transactional
    public UsuarioResponse crearUsuario(UsuarioRequest datos, Long actor) {
        String correo = normalizar(datos.correo());
        if (repositorio.existsByCorreo(correo)) {
            throw new CorreoRegistradoException();
        }

        UsuarioEntity usuario = new UsuarioEntity(
                null,
                datos.nombre().strip(),
                correo,
                datos.rol(),
                datos.activo() == null || datos.activo(),
                "",
                "",
                null,
                null);

        UsuarioResponse creado = respuesta(repositorio.saveAndFlush(usuario));
        registrar(actor, "USUARIO_CREADO", creado.id());
        return creado;
    }

    @Transactional
    public UsuarioResponse actualizarUsuario(long id, UsuarioRequest datos, Long actor) {
        UsuarioEntity usuario = buscar(id);
        String correo = normalizar(datos.correo());

        if (repositorio.existsByCorreoAndIdNot(correo, id)) {
            throw new CorreoRegistradoException();
        }

        usuario.setNombre(datos.nombre().strip());
        usuario.setCorreo(correo);
        usuario.setRol(datos.rol());

        UsuarioResponse actualizado = respuesta(repositorio.saveAndFlush(usuario));
        registrar(actor, "USUARIO_ACTUALIZADO", id);
        return actualizado;
    }

    @Transactional
    public UsuarioResponse cambiarEstado(long id, boolean activo, Long actor) {
        UsuarioEntity usuario = buscar(id);
        if (usuario.getActivo() == activo) {
            return respuesta(usuario);
        }

        usuario.setActivo(activo);

        UsuarioResponse actualizado = respuesta(repositorio.saveAndFlush(usuario));
        String accion = activo ? "USUARIO_ACTIVADO" : "USUARIO_DESACTIVADO";
        registrar(actor, accion, id);
        return actualizado;
    }

    public List<ActividadUsuario> actividadUsuarios() {
        return auditoria
                .findTop100ByOrderByFechaDescIdDesc()
                .stream()
                .map(actividad -> new ActividadUsuario(
                        "usuario-" + actividad.getId(),
                        actividad.getActor(),
                        actividad.getAccion(),
                        Long.toString(actividad.getReferencia()),
                        actividad.getFecha()))
                .toList();
    }

    public PerfilResponse perfil(long id) {
        return perfil(buscar(id));
    }

    @Transactional
    public PerfilResponse actualizarPerfil(long id, PerfilRequest datos) {
        UsuarioEntity usuario = buscar(id);
        String correo = normalizar(datos.correo());

        if (repositorio.existsByCorreoAndIdNot(correo, id)) {
            throw new CorreoRegistradoException();
        }
        if (datos.foto() != null && !fotoValida(datos.foto())) {
            throw new IllegalArgumentException("La foto debe ser una imagen JPEG válida");
        }

        usuario.setNombre(datos.nombre().strip());
        usuario.setApellido(datos.apellido().strip());
        usuario.setCorreo(correo);
        usuario.setTelefono(datos.telefono().strip());
        usuario.setFoto(datos.foto());

        return perfil(repositorio.saveAndFlush(usuario));
    }

    private void registrar(Long actor, String accion, long referencia) {
        if (actor == null) {
            return;
        }

        UsuarioEntity cuenta = buscar(actor);
        AuditoriaUsuarioEntity registro = new AuditoriaUsuarioEntity(
                null,
                actor,
                cuenta.getNombre(),
                accion,
                referencia,
                OffsetDateTime.now());
        auditoria.save(registro);
    }

    private boolean fotoValida(String foto) {
        try {
            byte[] bytes = Base64.getDecoder().decode(foto);

            return bytes.length >= 5
                    && bytes[0] == (byte) 0xff
                    && bytes[1] == (byte) 0xd8
                    && bytes[2] == (byte) 0xff
                    && bytes[bytes.length - 2] == (byte) 0xff
                    && bytes[bytes.length - 1] == (byte) 0xd9;
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private PerfilResponse perfil(UsuarioEntity usuario) {
        return new PerfilResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo(),
                usuario.getTelefono(),
                usuario.getRol(),
                usuario.getActivo(),
                usuario.getFoto());
    }

    private UsuarioResponse respuesta(UsuarioEntity usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol(),
                usuario.getActivo());
    }

    private UsuarioEntity buscar(long id) {
        return repositorio
                .findById(id)
                .orElseThrow(UsuarioNoEncontradoException::new);
    }

    private String normalizar(String correo) {
        return correo.strip().toLowerCase(Locale.ROOT);
    }
}
