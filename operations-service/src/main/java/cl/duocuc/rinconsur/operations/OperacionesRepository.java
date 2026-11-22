package cl.duocuc.rinconsur.operations;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class OperacionesRepository {

    record SesionBloqueada(Estado estado, long observadorId, OffsetDateTime inicio, OffsetDateTime fin) {}

    record RegistroGuardado(UUID sesionId, Categoria categoria) {}

    private static final String RESUMEN = """
        SELECT s.*, a.nombre AS acceso, r.transito, r.visita, r.no_determinado
        FROM sesiones s JOIN accesos a ON a.id = s.acceso_id
        LEFT JOIN (
            SELECT sesion_id,
                SUM(CASE WHEN categoria = 'TRANSITO' THEN 1 ELSE 0 END) AS transito,
                SUM(CASE WHEN categoria = 'VISITA' THEN 1 ELSE 0 END) AS visita,
                SUM(CASE WHEN categoria = 'NO_DETERMINADO' THEN 1 ELSE 0 END) AS no_determinado
            FROM registros GROUP BY sesion_id
        ) r ON r.sesion_id = s.id
        """;

    private final JdbcClient jdbc;

    public OperacionesRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Configuracion configuracion(String zona, boolean bloquear) {
        String sql = "SELECT apertura, cierre, minimo, maximo FROM configuracion WHERE id = 1";
        return jdbc.sql(bloquear ? sql + " FOR UPDATE" : sql)
                .query((fila, numero) -> new Configuracion(
                        fila.getObject("apertura", LocalTime.class),
                        fila.getObject("cierre", LocalTime.class),
                        fila.getInt("minimo"),
                        fila.getInt("maximo"),
                        zona
                ))
                .single();
    }

    public void actualizarHorario(HorarioRequest datos) {
        jdbc.sql("UPDATE configuracion SET apertura = ?, cierre = ?, minimo = ?, maximo = ? WHERE id = 1")
                .params(datos.apertura(), datos.cierre(), datos.minimo(), datos.maximo())
                .update();
    }

    public List<Acceso> accesos(boolean soloActivos) {
        String sql = "SELECT id, nombre, activo FROM accesos"
                + " WHERE (? = FALSE OR activo = TRUE) ORDER BY nombre_normalizado";

        return jdbc.sql(sql)
                .param(soloActivos)
                .query((fila, numero) -> new Acceso(
                        UUID.fromString(fila.getString("id")),
                        fila.getString("nombre"),
                        fila.getBoolean("activo")
                ))
                .list();
    }

    public int guardarAcceso(Acceso acceso, String normalizado, boolean nuevo) {
        String sql = nuevo
                ? "INSERT INTO accesos(nombre, nombre_normalizado, activo, id) VALUES (?, ?, ?, ?)"
                : "UPDATE accesos SET nombre = ?, nombre_normalizado = ?, activo = ? WHERE id = ?";

        return jdbc.sql(sql)
                .params(acceso.nombre(), normalizado, acceso.activo(), acceso.id().toString())
                .update();
    }

    public boolean accesoActivo(UUID id) {
        return jdbc.sql("SELECT activo FROM accesos WHERE id = ?")
                .param(id.toString())
                .query(Boolean.class)
                .optional()
                .orElse(false);
    }

    public boolean existeSuperposicion(long observadorId, OffsetDateTime inicio, OffsetDateTime fin, UUID excluir) {
        return jdbc.sql("""
            SELECT EXISTS(SELECT 1 FROM sesiones
                WHERE observador_id = ? AND inicio < ? AND fin > ? AND id <> ? AND estado <> 'CANCELADA')
            """)
                .params(observadorId, utc(fin), utc(inicio), excluir.toString())
                .query(Boolean.class)
                .single();
    }

    public boolean existeOcupacionReciente(
            long observadorId,
            UUID excluir,
            OffsetDateTime limite,
            OffsetDateTime ahora
    ) {
        boolean contarEnCurso = ahora.isAfter(limite);
        return jdbc.sql("""
            SELECT EXISTS(SELECT 1 FROM sesiones
                WHERE observador_id = ? AND id <> ?
                AND ((estado = 'EN_CURSO' AND ?) OR (estado = 'FINALIZADA' AND actualizada > ?)))
            """)
                .params(observadorId, excluir.toString(), contarEnCurso, utc(limite))
                .query(Boolean.class)
                .single();
    }

    public void guardarSesion(
            UUID id,
            UUID accesoId,
            Usuario observador,
            OffsetDateTime inicio,
            OffsetDateTime fin,
            OffsetDateTime ahora,
            boolean nueva
    ) {
        String sql = nueva ? """
            INSERT INTO sesiones(acceso_id, observador_id, observador, inicio, fin, actualizada, id, estado)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'PROGRAMADA')
            """ : """
            UPDATE sesiones SET acceso_id = ?, observador_id = ?, observador = ?, inicio = ?, fin = ?, actualizada = ?
            WHERE id = ?
            """;
        jdbc.sql(sql)
                .params(
                        accesoId.toString(),
                        observador.id(),
                        observador.nombre(),
                        utc(inicio),
                        utc(fin),
                        utc(ahora),
                        id.toString()
                )
                .update();
    }

    public SesionBloqueada bloquearSesion(UUID id) {
        return jdbc.sql("SELECT estado, observador_id, inicio, fin FROM sesiones WHERE id = ? FOR UPDATE")
                .param(id.toString())
                .query((fila, numero) -> new SesionBloqueada(
                        Estado.valueOf(fila.getString("estado")),
                        fila.getLong("observador_id"),
                        fechaUtc(fila, "inicio"),
                        fechaUtc(fila, "fin")
                ))
                .optional()
                .orElse(null);
    }

    public void actualizarEstado(UUID id, Estado estado, OffsetDateTime ahora) {
        jdbc.sql("UPDATE sesiones SET estado = ?, actualizada = ? WHERE id = ?")
                .params(estado.name(), utc(ahora), id.toString())
                .update();
    }

    public RegistroGuardado registro(UUID id) {
        return jdbc.sql("SELECT sesion_id, categoria FROM registros WHERE id = ?")
                .param(id.toString())
                .query((fila, numero) -> new RegistroGuardado(
                        UUID.fromString(fila.getString(1)),
                        Categoria.valueOf(fila.getString(2))
                ))
                .optional()
                .orElse(null);
    }

    public void insertarRegistro(UUID sesionId, RegistroRequest datos) {
        jdbc.sql("INSERT INTO registros (id, sesion_id, categoria) VALUES (?, ?, ?)")
                .params(datos.id().toString(), sesionId.toString(), datos.categoria().name())
                .update();
    }

    public List<Sesion> sesiones(Long observador, boolean historial) {
        String estados = historial ? " AND estado IN ('FINALIZADA', 'CANCELADA')" : "";
        String filtro = observador == null ? "WHERE 1 = 1" : "WHERE observador_id = ?";
        JdbcClient.StatementSpec consulta = jdbc.sql(RESUMEN + filtro + estados + " ORDER BY s.inicio DESC, s.id");
        if (observador != null) {
            consulta.param(observador);
        }

        return consulta.query(this::mapear).list();
    }

    public Sesion sesion(UUID id) {
        return jdbc.sql(RESUMEN + "WHERE s.id = ?")
                .param(id.toString())
                .query(this::mapear)
                .single();
    }

    private Sesion mapear(ResultSet fila, int numero) throws SQLException {
        return new Sesion(
                UUID.fromString(fila.getString("id")),
                UUID.fromString(fila.getString("acceso_id")),
                fila.getString("acceso"),
                fila.getLong("observador_id"),
                fila.getString("observador"),
                fechaUtc(fila, "inicio"),
                fechaUtc(fila, "fin"),
                Estado.valueOf(fila.getString("estado")),
                fechaUtc(fila, "actualizada"),
                new Conteo(
                        fila.getLong("transito"),
                        fila.getLong("visita"),
                        fila.getLong("no_determinado")
                )
        );
    }

    static OffsetDateTime fechaUtc(ResultSet fila, String columna) throws SQLException {
        return fila.getObject(columna, LocalDateTime.class).atOffset(ZoneOffset.UTC);
    }

    private static LocalDateTime utc(OffsetDateTime fecha) {
        return fecha.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
