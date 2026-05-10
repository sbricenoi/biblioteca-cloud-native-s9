package com.biblioteca.bff.graphql;

import com.biblioteca.bff.client.ServerlessClient;
import com.biblioteca.bff.model.Usuario;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolver GraphQL del BFF para el tipo Usuario.
 * Llama a la función serverless via REST a través de ServerlessClient.
 * Expuesto en el endpoint /graphql del BFF.
 */
@Controller
public class UsuarioResolver {

    private final ServerlessClient client;

    public UsuarioResolver(ServerlessClient client) {
        this.client = client;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @QueryMapping
    public List<Usuario> usuarios() {
        return client.listarUsuarios();
    }

    @QueryMapping
    public Usuario usuario(@Argument String id) {
        return client.obtenerUsuario(Long.parseLong(id));
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @MutationMapping
    public Usuario crearUsuario(
            @Argument String nombre,
            @Argument String apellido,
            @Argument String email,
            @Argument String rut,
            @Argument String telefono,
            @Argument String estado) {

        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", nombre);
        datos.put("apellido", apellido);
        datos.put("email", email);
        datos.put("rut", rut);
        datos.put("telefono", telefono);
        datos.put("estado", estado != null ? estado : "ACTIVO");
        return client.crearUsuario(datos);
    }

    @MutationMapping
    public Usuario actualizarUsuario(
            @Argument String id,
            @Argument String nombre,
            @Argument String apellido,
            @Argument String email,
            @Argument String rut,
            @Argument String telefono,
            @Argument String estado) {

        Map<String, Object> datos = new HashMap<>();
        if (nombre != null)    datos.put("nombre", nombre);
        if (apellido != null)  datos.put("apellido", apellido);
        if (email != null)     datos.put("email", email);
        if (rut != null)       datos.put("rut", rut);
        if (telefono != null)  datos.put("telefono", telefono);
        if (estado != null)    datos.put("estado", estado);
        return client.actualizarUsuario(Long.parseLong(id), datos);
    }

    @MutationMapping
    public Boolean eliminarUsuario(@Argument String id) {
        return client.eliminarUsuario(Long.parseLong(id));
    }
}
