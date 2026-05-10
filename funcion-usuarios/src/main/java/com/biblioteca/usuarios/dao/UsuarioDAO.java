package com.biblioteca.usuarios.dao;

import com.biblioteca.usuarios.model.Usuario;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para operaciones CRUD de usuarios contra Oracle DB.
 * Utiliza HikariCP para pool de conexiones.
 */
public class UsuarioDAO {

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
        config.setIdleTimeout(600000);
        dataSource = new HikariDataSource(config);
    }

    public Usuario crear(Usuario usuario) throws SQLException {
        String sqlInsert = "INSERT INTO usuarios (id, nombre, apellido, email, rut, telefono, estado) "
                + "VALUES (seq_usuarios.NEXTVAL, ?, ?, ?, ?, ?, ?)";
        String sqlGetId = "SELECT seq_usuarios.CURRVAL FROM DUAL";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmtInsert = conn.prepareStatement(sqlInsert);
             PreparedStatement stmtGetId   = conn.prepareStatement(sqlGetId)) {

            stmtInsert.setString(1, usuario.getNombre());
            stmtInsert.setString(2, usuario.getApellido());
            stmtInsert.setString(3, usuario.getEmail());
            stmtInsert.setString(4, usuario.getRut());
            stmtInsert.setString(5, usuario.getTelefono());
            stmtInsert.setString(6, usuario.getEstado() != null ? usuario.getEstado() : "ACTIVO");
            stmtInsert.executeUpdate();

            ResultSet rs = stmtGetId.executeQuery();
            if (rs.next()) {
                usuario.setId(rs.getLong(1));
            }
            return obtenerPorId(usuario.getId());
        }
    }

    public Usuario obtenerPorId(Long id) throws SQLException {
        String sql = "SELECT * FROM usuarios WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapear(rs);
            return null;
        }
    }

    public List<Usuario> listarTodos() throws SQLException {
        String sql = "SELECT * FROM usuarios ORDER BY id";
        List<Usuario> lista = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Usuario actualizar(Long id, Usuario usuario) throws SQLException {
        String sql = "UPDATE usuarios SET nombre=?, apellido=?, email=?, rut=?, telefono=?, estado=? WHERE id=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuario.getNombre());
            stmt.setString(2, usuario.getApellido());
            stmt.setString(3, usuario.getEmail());
            stmt.setString(4, usuario.getRut());
            stmt.setString(5, usuario.getTelefono());
            stmt.setString(6, usuario.getEstado());
            stmt.setLong(7, id);
            int rows = stmt.executeUpdate();
            if (rows == 0) return null;
            return obtenerPorId(id);
        }
    }

    public boolean eliminar(Long id) throws SQLException {
        String sql = "DELETE FROM usuarios WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        return new Usuario(
                rs.getLong("id"),
                rs.getString("nombre"),
                rs.getString("apellido"),
                rs.getString("email"),
                rs.getString("rut"),
                rs.getString("telefono"),
                rs.getTimestamp("fecha_registro"),
                rs.getString("estado")
        );
    }
}
