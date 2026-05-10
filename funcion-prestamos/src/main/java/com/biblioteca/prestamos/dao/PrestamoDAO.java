package com.biblioteca.prestamos.dao;

import com.biblioteca.prestamos.model.Prestamo;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para operaciones CRUD de préstamos contra Oracle DB.
 * La disponibilidad del libro es manejada por el evento PRESTAMO_CREADO
 * que procesa el microservicio-eventos.
 */
public class PrestamoDAO {

    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(System.getenv().getOrDefault("DB_URL",
                "jdbc:oracle:thin:@biblioteca_high"));
        config.setUsername(System.getenv().getOrDefault("DB_USER", "biblioteca"));
        config.setPassword(System.getenv().getOrDefault("DB_PASSWORD", "biblioteca123"));
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        dataSource = new HikariDataSource(config);
    }

    public Prestamo crear(Prestamo prestamo) throws SQLException {
        if (!verificarDisponibilidad(prestamo.getIdLibro())) {
            throw new SQLException("Libro no disponible para préstamo");
        }

        String sqlInsert = "INSERT INTO prestamos (id, id_usuario, id_libro, fecha_devolucion_esperada, estado) "
                + "VALUES (seq_prestamos.NEXTVAL, ?, ?, ?, 'PRESTADO')";
        String sqlGetId = "SELECT seq_prestamos.CURRVAL FROM DUAL";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmtInsert = conn.prepareStatement(sqlInsert);
             PreparedStatement stmtGetId   = conn.prepareStatement(sqlGetId)) {

            stmtInsert.setLong(1, prestamo.getIdUsuario());
            stmtInsert.setLong(2, prestamo.getIdLibro());
            stmtInsert.setDate(3, prestamo.getFechaDevolucionEsperada());
            stmtInsert.executeUpdate();

            ResultSet rs = stmtGetId.executeQuery();
            if (rs.next()) {
                prestamo.setId(rs.getLong(1));
            }
            return obtenerPorId(prestamo.getId());
        }
    }

    public Prestamo obtenerPorId(Long id) throws SQLException {
        String sql = "SELECT * FROM prestamos WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapear(rs);
            return null;
        }
    }

    public List<Prestamo> listarTodos() throws SQLException {
        String sql = "SELECT * FROM prestamos ORDER BY id DESC";
        List<Prestamo> lista = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public List<Prestamo> listarPorUsuario(Long idUsuario) throws SQLException {
        String sql = "SELECT * FROM prestamos WHERE id_usuario = ? ORDER BY id DESC";
        List<Prestamo> lista = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, idUsuario);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Prestamo actualizar(Long id, Prestamo prestamo) throws SQLException {
        String sql = "UPDATE prestamos SET id_usuario=?, id_libro=?, "
                + "fecha_devolucion_esperada=?, fecha_devolucion_real=?, estado=? WHERE id=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, prestamo.getIdUsuario());
            stmt.setLong(2, prestamo.getIdLibro());
            stmt.setDate(3, prestamo.getFechaDevolucionEsperada());
            stmt.setTimestamp(4, prestamo.getFechaDevolucionReal());
            stmt.setString(5, prestamo.getEstado());
            stmt.setLong(6, id);
            if (stmt.executeUpdate() == 0) return null;
            return obtenerPorId(id);
        }
    }

    public Prestamo devolverLibro(Long id) throws SQLException {
        String sql = "UPDATE prestamos SET fecha_devolucion_real = CURRENT_TIMESTAMP, "
                + "estado = 'DEVUELTO' WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            if (stmt.executeUpdate() == 0) return null;
            return obtenerPorId(id);
        }
    }

    public boolean eliminar(Long id) throws SQLException {
        String sql = "DELETE FROM prestamos WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public int eliminarPorUsuario(Long idUsuario) throws SQLException {
        String sql = "DELETE FROM prestamos WHERE id_usuario = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, idUsuario);
            return stmt.executeUpdate();
        }
    }

    private boolean verificarDisponibilidad(Long idLibro) throws SQLException {
        String sql = "SELECT cantidad_disponible FROM libros WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, idLibro);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("cantidad_disponible") > 0;
            return false;
        }
    }

    private Prestamo mapear(ResultSet rs) throws SQLException {
        return new Prestamo(
                rs.getLong("id"),
                rs.getLong("id_usuario"),
                rs.getLong("id_libro"),
                rs.getTimestamp("fecha_prestamo"),
                rs.getDate("fecha_devolucion_esperada"),
                rs.getTimestamp("fecha_devolucion_real"),
                rs.getString("estado")
        );
    }
}
