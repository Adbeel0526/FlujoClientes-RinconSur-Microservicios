package cl.duocuc.rinconsur.operations;

import java.sql.Types;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AuditoriaOperaciones {

    private final JdbcClient jdbc;

    public AuditoriaOperaciones(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void guardar(Usuario usuario, AccionAuditada accion, UUID referencia) {
        jdbc.sql("""
            INSERT INTO auditoria(id, actor_id, actor, accion, referencia, fecha)
            VALUES (?, ?, ?, ?, ?, UTC_TIMESTAMP(6))
            """)
                .param(1, UUID.randomUUID().toString())
                .param(2, usuario.id())
                .param(3, usuario.nombre())
                .param(4, accion.name())
                .param(5, referencia == null ? null : referencia.toString(), Types.VARCHAR)
                .update();
    }

    public List<Actividad> recientes() {
        String sql = "SELECT id, actor_id, actor, accion, referencia, fecha FROM auditoria"
                + " ORDER BY fecha DESC, id LIMIT 100";

        return jdbc.sql(sql)
                .query((fila, numero) -> {
                    String referencia = fila.getString("referencia");
                    return new Actividad(
                            UUID.fromString(fila.getString("id")),
                            fila.getLong("actor_id"),
                            fila.getString("actor"),
                            AccionAuditada.valueOf(fila.getString("accion")),
                            referencia == null ? null : UUID.fromString(referencia),
                            OperacionesRepository.fechaUtc(fila, "fecha")
                    );
                })
                .list();
    }
}
