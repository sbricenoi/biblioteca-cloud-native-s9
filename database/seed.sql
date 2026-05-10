-- ═══════════════════════════════════════════════════════════════════════════
-- Datos de prueba - Sistema de Biblioteca
-- DSY2207 - Semana 9
-- ═══════════════════════════════════════════════════════════════════════════

-- ─── Libros de ejemplo ────────────────────────────────────────────────────────
INSERT INTO libros (id, titulo, autor, isbn, categoria, cantidad_disponible, cantidad_total)
VALUES (seq_libros.NEXTVAL, 'Clean Code', 'Robert C. Martin', '978-0132350884', 'Programacion', 5, 5);

INSERT INTO libros (id, titulo, autor, isbn, categoria, cantidad_disponible, cantidad_total)
VALUES (seq_libros.NEXTVAL, 'The Pragmatic Programmer', 'David Thomas', '978-0201616224', 'Programacion', 3, 3);

INSERT INTO libros (id, titulo, autor, isbn, categoria, cantidad_disponible, cantidad_total)
VALUES (seq_libros.NEXTVAL, 'Designing Data-Intensive Applications', 'Martin Kleppmann', '978-1449373320', 'Arquitectura', 4, 4);

INSERT INTO libros (id, titulo, autor, isbn, categoria, cantidad_disponible, cantidad_total)
VALUES (seq_libros.NEXTVAL, 'Domain-Driven Design', 'Eric Evans', '978-0321125217', 'Arquitectura', 2, 2);

INSERT INTO libros (id, titulo, autor, isbn, categoria, cantidad_disponible, cantidad_total)
VALUES (seq_libros.NEXTVAL, 'Microservices Patterns', 'Chris Richardson', '978-1617294549', 'Cloud Native', 6, 6);

-- ─── Usuarios de ejemplo ──────────────────────────────────────────────────────
INSERT INTO usuarios (id, nombre, apellido, email, rut, telefono, estado)
VALUES (seq_usuarios.NEXTVAL, 'Juan', 'Perez', 'juan.perez@ejemplo.cl', '12345678-9', '+56912345678', 'ACTIVO');

INSERT INTO usuarios (id, nombre, apellido, email, rut, telefono, estado)
VALUES (seq_usuarios.NEXTVAL, 'Maria', 'González', 'maria.gonzalez@ejemplo.cl', '98765432-1', '+56987654321', 'ACTIVO');

INSERT INTO usuarios (id, nombre, apellido, email, rut, telefono, estado)
VALUES (seq_usuarios.NEXTVAL, 'Carlos', 'López', 'carlos.lopez@ejemplo.cl', '11223344-5', '+56911223344', 'ACTIVO');

COMMIT;
