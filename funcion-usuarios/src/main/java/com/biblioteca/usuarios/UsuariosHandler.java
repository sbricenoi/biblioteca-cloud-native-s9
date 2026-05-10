package com.biblioteca.usuarios;

import com.biblioteca.usuarios.dao.UsuarioDAO;
import com.biblioteca.usuarios.events.EventPublisher;
import com.biblioteca.usuarios.graphql.UsuariosGraphQL;
import com.biblioteca.usuarios.model.ApiResponse;
import com.biblioteca.usuarios.model.Usuario;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import spark.Request;
import spark.Response;

import java.util.List;
import java.util.Map;

import static spark.Spark.*;

/**
 * Función Serverless de Usuarios.
 * Expone endpoints REST (/usuarios/*) y GraphQL (/graphql).
 * Publica eventos a RabbitMQ al crear/eliminar usuarios.
 */
public class UsuariosHandler {

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .create();

    private static final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private static final EventPublisher eventPublisher = new EventPublisher();
    private static final UsuariosGraphQL usuariosGraphQL = new UsuariosGraphQL(usuarioDAO, eventPublisher);

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8081"));
        port(port);

        configurarCORS();
        configurarRutas();

        System.out.println("=== Función Usuarios v2.0 iniciada en puerto " + port + " ===");
        System.out.println("  REST  : http://localhost:" + port + "/usuarios");
        System.out.println("  GraphQL: http://localhost:" + port + "/graphql");
    }

    private static void configurarCORS() {
        options("/*", (req, res) -> {
            String reqHeaders = req.headers("Access-Control-Request-Headers");
            if (reqHeaders != null) res.header("Access-Control-Allow-Headers", reqHeaders);
            String reqMethod = req.headers("Access-Control-Request-Method");
            if (reqMethod != null) res.header("Access-Control-Allow-Methods", reqMethod);
            return "OK";
        });

        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Content-Type", "application/json");
        });
    }

    private static void configurarRutas() {
        // ─────────────────────────────────────────
        // Endpoints REST (FaaS tradicional)
        // ─────────────────────────────────────────
        path("/usuarios", () -> {
            post("",        UsuariosHandler::crearUsuario);
            get("",         UsuariosHandler::listarUsuarios);
            get("/:id",     UsuariosHandler::obtenerUsuario);
            put("/:id",     UsuariosHandler::actualizarUsuario);
            delete("/:id",  UsuariosHandler::eliminarUsuario);
        });

        // ─────────────────────────────────────────
        // Endpoint GraphQL
        // ─────────────────────────────────────────
        post("/graphql", UsuariosHandler::procesarGraphQL);

        // Health check
        get("/health", (req, res) -> {
            res.type("application/json");
            return gson.toJson(ApiResponse.success(
                    Map.of("rest", "OK", "graphql", "OK", "events", "RabbitMQ"),
                    "Servicio de usuarios operativo"
            ));
        });
    }

    // ── REST handlers ──────────────────────────────────────────────────────────

    private static String crearUsuario(Request req, Response res) {
        try {
            Usuario usuario = gson.fromJson(req.body(), Usuario.class);
            if (!validar(usuario)) {
                res.status(400);
                return gson.toJson(ApiResponse.error("Datos de usuario incompletos: nombre, apellido, email y rut son requeridos"));
            }
            Usuario creado = usuarioDAO.crear(usuario);
            eventPublisher.publicarUsuarioCreado(creado.getId(), creado.getEmail());
            res.status(201);
            return gson.toJson(ApiResponse.success(creado, "Usuario creado exitosamente"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(ApiResponse.error("Error al crear usuario: " + e.getMessage()));
        }
    }

    private static String listarUsuarios(Request req, Response res) {
        try {
            List<Usuario> usuarios = usuarioDAO.listarTodos();
            return gson.toJson(ApiResponse.success(usuarios, "Usuarios obtenidos exitosamente"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(ApiResponse.error("Error al listar usuarios: " + e.getMessage()));
        }
    }

    private static String obtenerUsuario(Request req, Response res) {
        try {
            Long id = Long.parseLong(req.params(":id"));
            Usuario usuario = usuarioDAO.obtenerPorId(id);
            if (usuario == null) {
                res.status(404);
                return gson.toJson(ApiResponse.error("Usuario no encontrado"));
            }
            return gson.toJson(ApiResponse.success(usuario, "Usuario obtenido exitosamente"));
        } catch (NumberFormatException e) {
            res.status(400);
            return gson.toJson(ApiResponse.error("ID inválido"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    private static String actualizarUsuario(Request req, Response res) {
        try {
            Long id = Long.parseLong(req.params(":id"));
            Usuario usuario = gson.fromJson(req.body(), Usuario.class);
            Usuario actualizado = usuarioDAO.actualizar(id, usuario);
            if (actualizado == null) {
                res.status(404);
                return gson.toJson(ApiResponse.error("Usuario no encontrado"));
            }
            return gson.toJson(ApiResponse.success(actualizado, "Usuario actualizado exitosamente"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    private static String eliminarUsuario(Request req, Response res) {
        try {
            Long id = Long.parseLong(req.params(":id"));
            boolean eliminado = usuarioDAO.eliminar(id);
            if (!eliminado) {
                res.status(404);
                return gson.toJson(ApiResponse.error("Usuario no encontrado"));
            }
            // Publica evento para que microservicio-eventos elimine préstamos asociados
            eventPublisher.publicarUsuarioEliminado(id);
            return gson.toJson(ApiResponse.success(null, "Usuario eliminado y evento publicado exitosamente"));
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

            Map<String, Object> resultado = usuariosGraphQL.ejecutar(query, operationName, variables);
            return gson.toJson(resultado);
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errors", List.of(Map.of("message", "Error interno: " + e.getMessage()))));
        }
    }

    private static boolean validar(Usuario u) {
        return u != null
                && u.getNombre() != null && !u.getNombre().isBlank()
                && u.getApellido() != null && !u.getApellido().isBlank()
                && u.getEmail() != null && !u.getEmail().isBlank()
                && u.getRut() != null && !u.getRut().isBlank();
    }
}
