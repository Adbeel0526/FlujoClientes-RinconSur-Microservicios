package cl.duocuc.rinconsur.auth.data.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "recuperaciones")
public class RecuperacionEntity {

    @Id
    @Column(name = "usuario_id")
    private long usuarioId;

    @Column(unique = true, nullable = false, length = 64)
    private String huella;

    @Column(nullable = false)
    private Instant emitida;

    public RecuperacionEntity(long usuarioId, String huella, Instant emitida) {
        this.usuarioId = usuarioId;
        this.huella = huella;
        this.emitida = emitida;
    }

    public long getUsuarioId() {
        return this.usuarioId;
    }

    public void setUsuarioId(long valor) {
        this.usuarioId = valor;
    }

    public String getHuella() {
        return this.huella;
    }

    public void setHuella(String valor) {
        this.huella = valor;
    }

    public Instant getEmitida() {
        return this.emitida;
    }

    public void setEmitida(Instant valor) {
        this.emitida = valor;
    }

    protected RecuperacionEntity() {
    }
}
