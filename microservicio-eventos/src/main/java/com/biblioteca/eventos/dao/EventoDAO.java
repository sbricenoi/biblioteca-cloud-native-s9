package com.biblioteca.eventos.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * DAO del microservicio de eventos.
 * Ejecuta operaciones en Oracle para procesar los eventos recibidos:
 *  - Actualizar disponibilidad de libros
 *  - Eliminar préstamos de usuario
 */
@Component
public class EventoDAO {

    @Value("${db.url}")
    private String dbUrl;

    @Value("${db.user}")
    private String dbUser;

    @Value("${db.password}")
    private String dbPassword;

    private HikariDataSource dataSource;

    @PostConstruct
    public void init() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000);
        this.dataSource = new HikariDataSource(config);
    }

    /**
     * Decrementa en 1 la disponibilidad de un libro.
     * Llamado cuando se recibe el evento PRESTAMO_CREADO.
     */
    public void decrementarDisponibilidadLibro(Long idLibro) throws SQLException {
        String sql = "UPDATE libros SET cantidad_disponible = cantidad_disponible - 1 " +
                     "WHERE id = ? AND cantidad_disponible > 0";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, idLibro);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.printf("[EVENTO-DAO] Libro %d: disponibilidad decrementada%n", idLibro);
            } else {
                System.out.printf("[EVENTO-DAO] AVISO: Libro %d sin disponibilidad o no encontrado%n", idLibro);
            }
        }
    }

    /**
     * Incrementa en 1 la disponibilidad de un libro.
     * Llamado cuando se recibe el evento PRESTAMO_DEVUELTO.
     */
    public void incrementarDisponibilidadLibro(Long idLibro) throws SQLException {
        String sql = "UPDATE libros SET cantidad_disponible = cantidad_disponible + 1 " +
                     "WHERE id = ? AND cantidad_disponible < cantidad_total";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, idLibro);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.printf("[EVENTO-DAO] Libro %d: disponibilidad incrementada%n", idLibro);
            }
        }
    }

    /**
     * Elimina todos los préstamos de un usuario.
     * Llamado cuando se recibe el evento USUARIO_ELIMINADO.
     */
    public int eliminarPrestamosPorUsuario(Long idUsuario) throws SQLException {
        String sql = "DELETE FROM prestamos WHERE id_usuario = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, idUsuario);
            int eliminados = stmt.executeUpdate();
            System.out.printf("[EVENTO-DAO] Usuario %d: %d préstamos eliminados%n", idUsuario, eliminados);
            return eliminados;
        }
    }
}
