package cl.duocuc.rinconsur.auth.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import cl.duocuc.rinconsur.auth.data.entity.CredencialEntity;

public interface CredencialRepository extends JpaRepository<CredencialEntity, Long> {

    CredencialEntity findByUsuarioId(long usuarioId);

    @Query(value = """
            SELECT id, usuario_id, contrasena_hash, version
            FROM credenciales WHERE usuario_id = :usuarioId FOR UPDATE
            """, nativeQuery = true)
    CredencialEntity bloquear(long usuarioId);

    boolean existsByUsuarioId(long usuarioId);
}
