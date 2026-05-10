package com.biblioteca.bff.controller;

import com.biblioteca.bff.client.ServerlessClient;
import com.biblioteca.bff.model.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller REST del BFF para usuarios.
 * Orquesta llamadas a la función serverless funcion-usuarios.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final ServerlessClient client;

    public UsuarioController(ServerlessClient client) {
        this.client = client;
    }

    @GetMapping
    public ResponseEntity<List<Usuario>> listar() {
        return ResponseEntity.ok(client.listarUsuarios());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> obtener(@PathVariable Long id) {
        Usuario u = client.obtenerUsuario(id);
        if (u == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(u);
    }

    @PostMapping
    public ResponseEntity<Usuario> crear(@RequestBody Map<String, Object> datos) {
        Usuario u = client.crearUsuario(datos);
        return ResponseEntity.status(HttpStatus.CREATED).body(u);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizar(@PathVariable Long id,
                                               @RequestBody Map<String, Object> datos) {
        Usuario u = client.actualizarUsuario(id, datos);
        if (u == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(u);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> eliminar(@PathVariable Long id) {
        boolean ok = client.eliminarUsuario(id);
        if (!ok) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("success", true, "message", "Usuario eliminado exitosamente"));
    }
}
