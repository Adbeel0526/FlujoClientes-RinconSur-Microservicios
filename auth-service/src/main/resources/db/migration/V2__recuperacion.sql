CREATE TABLE recuperaciones (
    usuario_id BIGINT PRIMARY KEY,
    huella VARCHAR(64) UNIQUE NOT NULL,
    emitida DATETIME(6) NOT NULL,
    FOREIGN KEY (usuario_id) REFERENCES credenciales(usuario_id)
);
