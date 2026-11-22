CREATE TABLE configuracion (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    apertura TIME NOT NULL,
    cierre TIME NOT NULL,
    minimo INTEGER NOT NULL,
    maximo INTEGER NOT NULL,
    CHECK (apertura < cierre),
    CHECK (minimo > 0 AND maximo BETWEEN minimo AND 1440)
);

INSERT INTO configuracion VALUES (1, TIME '08:00', TIME '22:00', 30, 60);

CREATE TABLE accesos (
    id CHAR(36) PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    nombre_normalizado VARCHAR(80) UNIQUE NOT NULL,
    activo BOOLEAN NOT NULL
);

CREATE TABLE sesiones (
    id CHAR(36) PRIMARY KEY,
    acceso_id CHAR(36) NOT NULL,
    observador_id BIGINT NOT NULL CHECK (observador_id > 0),
    observador VARCHAR(100) NOT NULL,
    inicio DATETIME(6) NOT NULL,
    fin DATETIME(6) NOT NULL,
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('PROGRAMADA', 'EN_CURSO', 'FINALIZADA', 'CANCELADA')),
    actualizada DATETIME(6) NOT NULL,
    FOREIGN KEY (acceso_id) REFERENCES accesos(id),
    CHECK (inicio < fin)
);

CREATE INDEX sesiones_observador_horario ON sesiones(observador_id, inicio, fin);

CREATE TABLE registros (
    id CHAR(36) PRIMARY KEY,
    sesion_id CHAR(36) NOT NULL,
    FOREIGN KEY (sesion_id) REFERENCES sesiones(id),
    categoria VARCHAR(20) NOT NULL CHECK (categoria IN ('TRANSITO', 'VISITA', 'NO_DETERMINADO'))
);

CREATE INDEX registros_sesion ON registros(sesion_id);
