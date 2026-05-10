package com.biblioteca.bff.controller;

import com.biblioteca.bff.config.ServerlessConfig;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Proxy GraphQL del BFF.
 * Reenvía peticiones GraphQL a las funciones serverless FaaS.
 * Evita problemas de CORS entre puertos distintos en el navegador.
 *
 * /proxy/usuarios/graphql  → http://funcion-usuarios:8081/graphql
 * /proxy/prestamos/graphql → http://funcion-prestamos:8082/graphql
 */
@RestController
@RequestMapping("/proxy")
public class GraphQLProxyController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ServerlessConfig config;

    public GraphQLProxyController(ServerlessConfig config) {
        this.config = config;
    }

    @PostMapping("/usuarios/graphql")
    public ResponseEntity<String> proxyUsuarios(@RequestBody String body) {
        return forward(config.getUsuarios().getUrl() + "/graphql", body);
    }

    @PostMapping("/prestamos/graphql")
    public ResponseEntity<String> proxyPrestamos(@RequestBody String body) {
        return forward(config.getPrestamos().getUrl() + "/graphql", body);
    }

    private ResponseEntity<String> forward(String url, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        try {
            return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"errors\":[{\"message\":\"Error al conectar con función FaaS: " + e.getMessage() + "\"}]}");
        }
    }
}
