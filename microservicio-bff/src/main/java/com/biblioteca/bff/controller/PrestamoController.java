package com.biblioteca.bff.controller;

import com.biblioteca.bff.client.ServerlessClient;
import com.biblioteca.bff.model.Prestamo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller REST del BFF para préstamos.
 * Orquesta llamadas a la función serverless funcion-prestamos.
 */
@RestController
@RequestMapping("/api/prestamos")
public class PrestamoController {

    private final ServerlessClient client;

    public PrestamoController(ServerlessClient client) {
        this.client = client;
    }

    @GetMapping
    public ResponseEntity<List<Prestamo>> listar() {
        return ResponseEntity.ok(client.listarPrestamos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Prestamo> obtener(@PathVariable Long id) {
        Prestamo p = client.obtenerPrestamo(id);
        if (p == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(p);
    }

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<Prestamo>> listarPorUsuario(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(client.listarPrestamosPorUsuario(idUsuario));
    }

    @PostMapping
    public ResponseEntity<Prestamo> crear(@RequestBody Map<String, Object> datos) {
        Prestamo p = client.crearPrestamo(datos);
        return ResponseEntity.status(HttpStatus.CREATED).body(p);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Prestamo> actualizar(@PathVariable Long id,
                                                @RequestBody Map<String, Object> datos) {
        Prestamo p = client.actualizarPrestamo(id, datos);
        if (p == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(p);
    }

    @PutMapping("/{id}/devolver")
    public ResponseEntity<Prestamo> devolver(@PathVariable Long id) {
        Prestamo p = client.devolverPrestamo(id);
        if (p == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(p);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> eliminar(@PathVariable Long id) {
        boolean ok = client.eliminarPrestamo(id);
        if (!ok) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("success", true, "message", "Préstamo eliminado exitosamente"));
    }
}
