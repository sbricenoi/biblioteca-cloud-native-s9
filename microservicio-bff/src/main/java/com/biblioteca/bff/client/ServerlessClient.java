package com.biblioteca.bff.client;

import com.biblioteca.bff.config.ServerlessConfig;
import com.biblioteca.bff.model.ApiResponse;
import com.biblioteca.bff.model.Prestamo;
import com.biblioteca.bff.model.Usuario;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP para comunicarse con las funciones serverless vía REST.
 * El BFF usa este cliente tanto desde los controllers REST como desde los resolvers GraphQL.
 */
@Component
public class ServerlessClient {

    private final RestTemplate restTemplate;
    private final ServerlessConfig config;
    private final ObjectMapper objectMapper;

    public ServerlessClient(ServerlessConfig config) {
        this.restTemplate = new RestTemplate();
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    // ── Usuarios ──────────────────────────────────────────────────────────────

    public List<Usuario> listarUsuarios() {
        String url = config.getUsuarios().getUrl() + "/usuarios";
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        return extraerLista(response.getBody(), Usuario.class);
    }

    public Usuario obtenerUsuario(Long id) {
        String url = config.getUsuarios().getUrl() + "/usuarios/" + id;
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        return extraerObjeto(response.getBody(), Usuario.class);
    }

    public Usuario crearUsuario(Map<String, Object> datos) {
        String url = config.getUsuarios().getUrl() + "/usuarios";
        ResponseEntity<Map> response = postJson(url, datos);
        return extraerObjeto(response.getBody(), Usuario.class);
    }

    public Usuario actualizarUsuario(Long id, Map<String, Object> datos) {
        String url = config.getUsuarios().getUrl() + "/usuarios/" + id;
        HttpEntity<Map<String, Object>> entity = jsonEntity(datos);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Map.class);
        return extraerObjeto(response.getBody(), Usuario.class);
    }

    public Boolean eliminarUsuario(Long id) {
        String url = config.getUsuarios().getUrl() + "/usuarios/" + id;
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.DELETE, null, Map.class);
        Map<?, ?> body = response.getBody();
        return body != null && Boolean.TRUE.equals(body.get("success"));
    }

    // ── Préstamos ─────────────────────────────────────────────────────────────

    public List<Prestamo> listarPrestamos() {
        String url = config.getPrestamos().getUrl() + "/prestamos";
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        return extraerLista(response.getBody(), Prestamo.class);
    }

    public Prestamo obtenerPrestamo(Long id) {
        String url = config.getPrestamos().getUrl() + "/prestamos/" + id;
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        return extraerObjeto(response.getBody(), Prestamo.class);
    }

    public List<Prestamo> listarPrestamosPorUsuario(Long idUsuario) {
        String url = config.getPrestamos().getUrl() + "/prestamos/usuario/" + idUsuario;
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        return extraerLista(response.getBody(), Prestamo.class);
    }

    public Prestamo crearPrestamo(Map<String, Object> datos) {
        String url = config.getPrestamos().getUrl() + "/prestamos";
        ResponseEntity<Map> response = postJson(url, datos);
        return extraerObjeto(response.getBody(), Prestamo.class);
    }

    public Prestamo actualizarPrestamo(Long id, Map<String, Object> datos) {
        String url = config.getPrestamos().getUrl() + "/prestamos/" + id;
        HttpEntity<Map<String, Object>> entity = jsonEntity(datos);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Map.class);
        return extraerObjeto(response.getBody(), Prestamo.class);
    }

    public Prestamo devolverPrestamo(Long id) {
        String url = config.getPrestamos().getUrl() + "/prestamos/" + id + "/devolver";
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, null, Map.class);
        return extraerObjeto(response.getBody(), Prestamo.class);
    }

    public Boolean eliminarPrestamo(Long id) {
        String url = config.getPrestamos().getUrl() + "/prestamos/" + id;
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.DELETE, null, Map.class);
        Map<?, ?> body = response.getBody();
        return body != null && Boolean.TRUE.equals(body.get("success"));
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    private ResponseEntity<Map> postJson(String url, Map<String, Object> datos) {
        return restTemplate.exchange(url, HttpMethod.POST, jsonEntity(datos), Map.class);
    }

    private HttpEntity<Map<String, Object>> jsonEntity(Map<String, Object> datos) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(datos, headers);
    }

    @SuppressWarnings("unchecked")
    private <T> T extraerObjeto(Map<?, ?> body, Class<T> tipo) {
        if (body == null) return null;
        Object data = body.get("data");
        if (data == null) return null;
        return objectMapper.convertValue(data, tipo);
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> extraerLista(Map<?, ?> body, Class<T> tipo) {
        if (body == null) return List.of();
        Object data = body.get("data");
        if (data == null) return List.of();
        return objectMapper.convertValue(data, objectMapper.getTypeFactory()
                .constructCollectionType(List.class, tipo));
    }
}
