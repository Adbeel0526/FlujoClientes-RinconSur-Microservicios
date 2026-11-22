package cl.duocuc.rinconsur.user.data.entity;

import cl.duocuc.rinconsur.user.model.Rol;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "usuarios")
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(unique = true, nullable = false, length = 254)
    private String correo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(12)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, length = 30)
    private String telefono;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String foto;

    @Version
    @Column(nullable = false)
    private Long version;

    public UsuarioEntity(
            Long id,
            String nombre,
            String correo,
            Rol rol,
            boolean activo,
            String apellido,
            String telefono,
            String foto,
            Long version) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.rol = rol;
        this.activo = activo;
        this.apellido = apellido;
        this.telefono = telefono;
        this.foto = foto;
        this.version = version;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long valor) {
        this.id = valor;
    }

    public String getNombre() {
        return this.nombre;
    }

    public void setNombre(String valor) {
        this.nombre = valor;
    }

    public String getCorreo() {
        return this.correo;
    }

    public void setCorreo(String valor) {
        this.correo = valor;
    }

    public Rol getRol() {
        return this.rol;
    }

    public void setRol(Rol valor) {
        this.rol = valor;
    }

    public boolean getActivo() {
        return this.activo;
    }

    public void setActivo(boolean valor) {
        this.activo = valor;
    }

    public String getApellido() {
        return this.apellido;
    }

    public void setApellido(String valor) {
        this.apellido = valor;
    }

    public String getTelefono() {
        return this.telefono;
    }

    public void setTelefono(String valor) {
        this.telefono = valor;
    }

    public String getFoto() {
        return this.foto;
    }

    public void setFoto(String valor) {
        this.foto = valor;
    }

    public Long getVersion() {
        return this.version;
    }

    public void setVersion(Long valor) {
        this.version = valor;
    }

    protected UsuarioEntity() {
    }
}
