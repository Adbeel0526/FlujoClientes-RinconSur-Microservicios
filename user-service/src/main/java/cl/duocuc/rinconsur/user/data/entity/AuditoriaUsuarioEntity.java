package cl.duocuc.rinconsur.user.data.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "auditoria_usuarios")
public class AuditoriaUsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id", nullable = false)
    private long actorId;

    @Column(nullable = false, length = 100)
    private String actor;

    @Column(nullable = false, length = 40)
    private String accion;

    @Column(nullable = false)
    private long referencia;

    @Column(nullable = false)
    private OffsetDateTime fecha;

    public AuditoriaUsuarioEntity(
            Long id,
            long actorId,
            String actor,
            String accion,
            long referencia,
            OffsetDateTime fecha) {
        this.id = id;
        this.actorId = actorId;
        this.actor = actor;
        this.accion = accion;
        this.referencia = referencia;
        this.fecha = fecha;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long valor) {
        this.id = valor;
    }

    public long getActorId() {
        return this.actorId;
    }

    public void setActorId(long valor) {
        this.actorId = valor;
    }

    public String getActor() {
        return this.actor;
    }

    public void setActor(String valor) {
        this.actor = valor;
    }

    public String getAccion() {
        return this.accion;
    }

    public void setAccion(String valor) {
        this.accion = valor;
    }

    public long getReferencia() {
        return this.referencia;
    }

    public void setReferencia(long valor) {
        this.referencia = valor;
    }

    public OffsetDateTime getFecha() {
        return this.fecha;
    }

    public void setFecha(OffsetDateTime valor) {
        this.fecha = valor;
    }

    protected AuditoriaUsuarioEntity() {
    }
}
