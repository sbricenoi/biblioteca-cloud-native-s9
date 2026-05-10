package com.biblioteca.prestamos;

import com.biblioteca.prestamos.dao.PrestamoDAO;
import com.biblioteca.prestamos.events.EventPublisher;
import com.biblioteca.prestamos.graphql.PrestamosGraphQL;
import com.biblioteca.prestamos.model.ApiResponse;
import com.biblioteca.prestamos.model.Prestamo;
import com.google.gson.*;
import spark.Request;
import spark.Response;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static spark.Spark.*;

/**
 * Función Serverless de Préstamos.
 * Expone REST (/prestamos/*) + GraphQL (/graphql).
 * Publica eventos a RabbitMQ al crear/devolver préstamos.
 */
public class PrestamosHandler {

    /** TypeAdapter que acepta "yyyy-MM-dd" y "yyyy-MM-dd'T'HH:mm:ss" para java.sql.Date */
    private static final JsonDeserializer<Date> SQL_DATE_DESERIALIZER = (json, type, ctx) -> {
        String s = json.getAsString();
        try { return Date.valueOf(s); } catch (IllegalArgumentException e) {
            // Intenta quitar la parte de tiempo si viene con formato completo
            return Date.valueOf(s.substring(0, 10));
        }
    };

    private static final JsonSerializer<Date> SQL_DATE_SERIALIZER =
            (date, type, ctx) -> new JsonPrimitive(date.toString());

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .registerTypeAdapter(Date.class, SQL_DATE_DESERIALIZER)
            .registerTypeAdapter(Date.class, SQL_DATE_SERIALIZER)
            .create();

    private static final PrestamoDAO prestamoDAO    = new PrestamoDAO();
    private static final EventPublisher eventPublisher = new EventPublisher();
    private static final PrestamosGraphQL prestamosGraphQL =
            new PrestamosGraphQL(prestamoDAO, eventPublisher);

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8082"));
        port(port);

        configurarCORS();
        configurarRutas();

        System.out.println("=== Función Préstamos v2.0 iniciada en puerto " + port + " ===");
        System.out.println("  REST  : http://localhost:" + port + "/prestamos");
        System.out.println("  GraphQL: http://localhost:" + port + "/graphql");
    }

    private static void configurarCORS() {
        options("/*", (req, res) -> {
            String h = req.headers("Access-Control-Request-Headers");
            if (h != null) res.header("Access-Control-Allow-Headers", h);
            String m = req.headers("Access-Control-Request-Method");
            if (m != null) res.header("Access-Control-Allow-Methods", m);
            return "OK";
        });
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Content-Type", "application/json");
        });
    }

    private static void configurarRutas() {
        // ─── REST ──────────────────────────────────────────────────────────────
        path("/prestamos", () -> {
            post("",                PrestamosHandler::crearPrestamo);
            get("",                 PrestamosHandler::listarPrestamos);
            get("/:id",             PrestamosHandler::obtenerPrestamo);
            put("/:id",             PrestamosHandler::actualizarPrestamo);
            put("/:id/devolver",    PrestamosHandler::devolverLibro);
            delete("/:id",          PrestamosHandler::eliminarPrestamo);
        });

        get("/prestamos/usuario/:idUsuario", PrestamosHandler::listarPorUsuario);

        // ─── GraphQL ───────────────────────────────────────────────────────────
        post("/graphql", PrestamosHandler::procesarGraphQL);

        get("/health", (req, res) -> gson.toJson(
                ApiResponse.success(Map.of("rest", "OK", "graphql", "OK", "events", "RabbitMQ"),
                        "Servicio de préstamos operativo")
        ));
    }

    // ── REST handlers ──────────────────────────────────────────────────────────

    private static String crearPrestamo(Request req, Response res) {
        try {
            Prestamo prestamo = gson.fromJson(req.body(), Prestamo.class);
            if (!validar(prestamo)) {
                res.status(400);
                return gson.toJson(ApiResponse.error("Datos incompletos: idUsuario, idLibro y fechaDevolucionEsperada son requeridos"));
            }
            Prestamo creado = prestamoDAO.crear(prestamo);
            // Publica evento: microservicio-eventos actualizará disponibilidad del libro
            eventPublisher.publicarPrestamoCreado(creado.getId(), creado.getIdUsuario(), creado.getIdLibro());
            res.status(201);
            return gson.toJson(ApiResponse.success(creado, "Préstamo creado y evento publicado exitosamente"));
        } catch (SQLException e) {
            res.status(e.getMessage().contains("no disponible") ? 400 : 500);
            return gson.toJson(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    private static String listarPrestamos(Request req, Response res) {
        try {
            return gson.toJson(ApiResponse.success(prestamoDAO.listarTodos(), "Préstamos obtenidos exitosamente"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    private static String obtenerPrestamo(Request req, Response res) {
        try {
            Long id = Long.parseLong(req.params(":id"));
            Prestamo prestamo = prestamoDAO.obtenerPorId(id);
            if (prestamo == null) { res.status(404); return gson.toJson(ApiResponse.error("Préstamo no encontrado")); }
            return gson.toJson(ApiResponse.success(prestamo, "Préstamo obtenido exitosamente"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    private static String listarPorUsuario(Request req, Response res) {
        try {
            Long idUsuario = Long.parseLong(req.params(":idUsuario"));
            return gson.toJson(ApiResponse.success(
                    prestamoDAO.listarPorUsuario(idUsuario),
                    "Préstamos del usuario obtenidos exitosamente"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    private static String actualizarPrestamo(Request req, Response res) {
        try {
            Long id = Long.parseLong(req.params(":id"));
            Prestamo prestamo = gson.fromJson(req.body(), Prestamo.class);
            Prestamo actualizado = prestamoDAO.actualizar(id, prestamo);
            if (actualizado == null) { res.status(404); return gson.toJson(ApiResponse.error("Préstamo no encontrado")); }
            return gson.toJson(ApiResponse.success(actualizado, "Préstamo actualizado exitosamente"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    private static String devolverLibro(Request req, Response res) {
        try {
            Long id = Long.parseLong(req.params(":id"));
            Prestamo devuelto = prestamoDAO.devolverLibro(id);
            if (devuelto == null) { res.status(404); return gson.toJson(ApiResponse.error("Préstamo no encontrado")); }
            eventPublisher.publicarPrestamoDevuelto(devuelto.getId(), devuelto.getIdLibro());
            return gson.toJson(ApiResponse.success(devuelto, "Libro devuelto y evento publicado exitosamente"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    private static String eliminarPrestamo(Request req, Response res) {
        try {
            Long id = Long.parseLong(req.params(":id"));
            boolean eliminado = prestamoDAO.eliminar(id);
            if (!eliminado) { res.status(404); return gson.toJson(ApiResponse.error("Préstamo no encontrado")); }
            return gson.toJson(ApiResponse.success(null, "Préstamo eliminado exitosamente"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    // ── GraphQL handler ────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static String procesarGraphQL(Request req, Response res) {
        try {
            Map<String, Object> body = gson.fromJson(req.body(), Map.class);
            String query = (String) body.get("query");
            String operationName = (String) body.get("operationName");
            Map<String, Object> variables = (Map<String, Object>) body.get("variables");

            if (query == null || query.isBlank()) {
                res.status(400);
                return gson.toJson(Map.of("errors", List.of(Map.of("message", "El campo 'query' es requerido"))));
            }

            return gson.toJson(prestamosGraphQL.ejecutar(query, operationName, variables));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errors", List.of(Map.of("message", "Error: " + e.getMessage()))));
        }
    }

    private static boolean validar(Prestamo p) {
        return p != null
                && p.getIdUsuario() != null
                && p.getIdLibro() != null
                && p.getFechaDevolucionEsperada() != null;
    }
}
