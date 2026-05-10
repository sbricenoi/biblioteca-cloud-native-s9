package com.biblioteca.bff.graphql;

import com.biblioteca.bff.client.ServerlessClient;
import com.biblioteca.bff.model.Prestamo;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolver GraphQL del BFF para el tipo Prestamo.
 * Llama a la función serverless via REST a través de ServerlessClient.
 * Expuesto en el endpoint /graphql del BFF.
 */
@Controller
public class PrestamoResolver {

    private final ServerlessClient client;

    public PrestamoResolver(ServerlessClient client) {
        this.client = client;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @QueryMapping
    public List<Prestamo> prestamos() {
        return client.listarPrestamos();
    }

    @QueryMapping
    public Prestamo prestamo(@Argument String id) {
        return client.obtenerPrestamo(Long.parseLong(id));
    }

    @QueryMapping
    public List<Prestamo> prestamosPorUsuario(@Argument String idUsuario) {
        return client.listarPrestamosPorUsuario(Long.parseLong(idUsuario));
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @MutationMapping
    public Prestamo crearPrestamo(
            @Argument String idUsuario,
            @Argument String idLibro,
            @Argument String fechaDevolucionEsperada) {

        Map<String, Object> datos = new HashMap<>();
        datos.put("idUsuario", Long.parseLong(idUsuario));
        datos.put("idLibro", Long.parseLong(idLibro));
        datos.put("fechaDevolucionEsperada", fechaDevolucionEsperada);
        return client.crearPrestamo(datos);
    }

    @MutationMapping
    public Prestamo actualizarPrestamo(
            @Argument String id,
            @Argument String idUsuario,
            @Argument String idLibro,
            @Argument String fechaDevolucionEsperada,
            @Argument String estado) {

        Map<String, Object> datos = new HashMap<>();
        if (idUsuario != null)              datos.put("idUsuario", Long.parseLong(idUsuario));
        if (idLibro != null)                datos.put("idLibro", Long.parseLong(idLibro));
        if (fechaDevolucionEsperada != null) datos.put("fechaDevolucionEsperada", fechaDevolucionEsperada);
        if (estado != null)                 datos.put("estado", estado);
        return client.actualizarPrestamo(Long.parseLong(id), datos);
    }

    @MutationMapping
    public Prestamo devolverPrestamo(@Argument String id) {
        return client.devolverPrestamo(Long.parseLong(id));
    }

    @MutationMapping
    public Boolean eliminarPrestamo(@Argument String id) {
        return client.eliminarPrestamo(Long.parseLong(id));
    }
}
