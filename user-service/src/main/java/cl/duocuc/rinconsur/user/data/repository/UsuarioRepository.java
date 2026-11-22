package cl.duocuc.rinconsur.user.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duocuc.rinconsur.user.data.entity.UsuarioEntity;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {

    UsuarioEntity findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByCorreoAndIdNot(String correo, long id);
}
