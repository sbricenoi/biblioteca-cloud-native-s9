-- ═══════════════════════════════════════════════════════════════════════════
-- Schema - Sistema de Biblioteca
-- Oracle XE - Semana 9 - DSY2207
-- ═══════════════════════════════════════════════════════════════════════════

-- Secuencias para IDs autoincrementales
CREATE SEQUENCE seq_usuarios  START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_libros    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_prestamos START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ─── Tabla de Usuarios ────────────────────────────────────────────────────────
CREATE TABLE usuarios (
    id             NUMBER        PRIMARY KEY,
    nombre         VARCHAR2(100) NOT NULL,
    apellido       VARCHAR2(100) NOT NULL,
    email          VARCHAR2(255) NOT NULL UNIQUE,
    rut            VARCHAR2(12)  NOT NULL UNIQUE,
    telefono       VARCHAR2(20),
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estado         VARCHAR2(20)  DEFAULT 'ACTIVO'
                   CHECK (estado IN ('ACTIVO', 'INACTIVO', 'SUSPENDIDO'))
);

CREATE INDEX idx_usuarios_email ON usuarios(email);
CREATE INDEX idx_usuarios_rut   ON usuarios(rut);

-- ─── Tabla de Libros ──────────────────────────────────────────────────────────
CREATE TABLE libros (
    id                  NUMBER        PRIMARY KEY,
    titulo              VARCHAR2(255) NOT NULL,
    autor               VARCHAR2(255) NOT NULL,
    isbn                VARCHAR2(20)  UNIQUE,
    categoria           VARCHAR2(100),
    cantidad_disponible NUMBER        DEFAULT 0  CHECK (cantidad_disponible >= 0),
    cantidad_total      NUMBER        NOT NULL   CHECK (cantidad_total >= 0),
    CONSTRAINT chk_cantidad CHECK (cantidad_disponible <= cantidad_total)
);

CREATE INDEX idx_libros_isbn      ON libros(isbn);
CREATE INDEX idx_libros_categoria ON libros(categoria);

-- ─── Tabla de Préstamos ───────────────────────────────────────────────────────
-- Nota: ON DELETE CASCADE permite que la BD elimine préstamos si el usuario
-- es eliminado directamente. El microservicio-eventos también lo hace vía evento
-- para demostrar la arquitectura orientada a eventos.
CREATE TABLE prestamos (
    id                       NUMBER      PRIMARY KEY,
    id_usuario               NUMBER      NOT NULL,
    id_libro                 NUMBER      NOT NULL,
    fecha_prestamo           TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    fecha_devolucion_esperada DATE        NOT NULL,
    fecha_devolucion_real    TIMESTAMP,
    estado                   VARCHAR2(20) DEFAULT 'PRESTADO'
                             CHECK (estado IN ('PRESTADO', 'DEVUELTO', 'RETRASADO')),
    CONSTRAINT fk_prestamos_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_prestamos_libro FOREIGN KEY (id_libro)
        REFERENCES libros(id)
);

CREATE INDEX idx_prestamos_usuario ON prestamos(id_usuario);
CREATE INDEX idx_prestamos_libro   ON prestamos(id_libro);
CREATE INDEX idx_prestamos_estado  ON prestamos(estado);

-- ─── Trigger: Marcar préstamo como RETRASADO ──────────────────────────────────
CREATE OR REPLACE TRIGGER trg_actualizar_estado_prestamo
BEFORE UPDATE ON prestamos
FOR EACH ROW
BEGIN
    IF :NEW.fecha_devolucion_real IS NULL
       AND :NEW.fecha_devolucion_esperada < SYSDATE THEN
        :NEW.estado := 'RETRASADO';
    END IF;
END;
/
