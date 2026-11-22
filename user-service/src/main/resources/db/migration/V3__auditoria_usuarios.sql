CREATE TABLE auditoria_usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT NOT NULL,
    actor VARCHAR(100) NOT NULL,
    accion VARCHAR(40) NOT NULL,
    referencia BIGINT NOT NULL,
    fecha DATETIME(6) NOT NULL
);

CREATE INDEX auditoria_usuarios_fecha ON auditoria_usuarios(fecha);
