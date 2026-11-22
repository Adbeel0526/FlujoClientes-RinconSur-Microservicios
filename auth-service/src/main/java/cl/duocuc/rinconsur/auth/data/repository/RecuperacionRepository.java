package cl.duocuc.rinconsur.auth.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import cl.duocuc.rinconsur.auth.data.entity.RecuperacionEntity;

public interface RecuperacionRepository extends JpaRepository<RecuperacionEntity, Long> {

    @Query("select r.usuarioId from RecuperacionEntity r where r.huella = :huella")
    Long usuarioPorHuella(String huella);
}
