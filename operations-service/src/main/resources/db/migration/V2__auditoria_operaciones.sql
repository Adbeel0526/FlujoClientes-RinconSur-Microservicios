CREATE TABLE auditoria (
    id CHAR(36) PRIMARY KEY,
    actor_id BIGINT NOT NULL,
    actor VARCHAR(100) NOT NULL,
    accion VARCHAR(40) NOT NULL,
    referencia CHAR(36),
    fecha DATETIME(6) NOT NULL
);

CREATE INDEX auditoria_fecha ON auditoria(fecha);
