package cl.duocuc.rinconsur.auth.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "credenciales")
public class CredencialEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", unique = true, nullable = false)
    private long usuarioId;

    @Column(name = "contrasena_hash", nullable = false, length = 100)
    private String contrasenaHash;

    @Version
    @Column(nullable = false)
    private Long version;

    public CredencialEntity(Long id, long usuarioId, String contrasenaHash, Long version) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.contrasenaHash = contrasenaHash;
        this.version = version;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long valor) {
        this.id = valor;
    }

    public long getUsuarioId() {
        return this.usuarioId;
    }

    public void setUsuarioId(long valor) {
        this.usuarioId = valor;
    }

    public String getContrasenaHash() {
        return this.contrasenaHash;
    }

    public void setContrasenaHash(String valor) {
        this.contrasenaHash = valor;
    }

    public Long getVersion() {
        return this.version;
    }

    public void setVersion(Long valor) {
        this.version = valor;
    }

    protected CredencialEntity() {
    }
}
