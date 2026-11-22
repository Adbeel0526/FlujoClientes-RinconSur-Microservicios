CREATE TABLE credenciales (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    contrasena_hash VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_credenciales_usuario UNIQUE (usuario_id),
    CONSTRAINT ck_credenciales_usuario CHECK (usuario_id > 0)
);
