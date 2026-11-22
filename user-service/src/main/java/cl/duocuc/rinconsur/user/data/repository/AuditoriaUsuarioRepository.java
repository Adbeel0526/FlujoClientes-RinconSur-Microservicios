package cl.duocuc.rinconsur.user.data.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duocuc.rinconsur.user.data.entity.AuditoriaUsuarioEntity;

public interface AuditoriaUsuarioRepository extends JpaRepository<AuditoriaUsuarioEntity, Long> {

    List<AuditoriaUsuarioEntity> findTop100ByOrderByFechaDescIdDesc();
}
