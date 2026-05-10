package com.biblioteca.eventos.listener;

import com.biblioteca.eventos.config.RabbitMQConfig;
import com.biblioteca.eventos.dao.EventoDAO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Listener de eventos relacionados con usuarios.
 *
 * Eventos manejados:
 *  - USUARIO_ELIMINADO → Elimina todos los préstamos activos del usuario
 *  - USUARIO_CREADO    → Registra log de auditoría
 *
 * El evento USUARIO_ELIMINADO es crítico: garantiza consistencia
 * entre la eliminación del usuario y sus préstamos asociados,
 * implementado mediante arquitectura orientada a eventos.
 */
@Component
public class UsuarioEventListener {

    private final EventoDAO eventoDAO;
    private final ObjectMapper objectMapper;

    public UsuarioEventListener(EventoDAO eventoDAO) {
        this.eventoDAO = eventoDAO;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Procesa el evento USUARIO_ELIMINADO.
     * Elimina todos los préstamos del usuario para mantener consistencia.
     * (La BD tiene ON DELETE CASCADE, pero el evento permite procesar
     * lógica adicional como liberar stock de los libros prestados.)
     */
    @RabbitListener(queues = RabbitMQConfig.COLA_USUARIO_ELIMINADO)
    public void procesarUsuarioEliminado(String mensaje) {
        System.out.println("\n[EVENTO] >>> Recibido USUARIO_ELIMINADO: " + mensaje);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> datos = objectMapper.readValue(mensaje, Map.class);

            Long idUsuario = getLong(datos, "idUsuario");
            System.out.printf("[EVENTO] Procesando eliminación del usuario %d%n", idUsuario);

            int prestamosEliminados = eventoDAO.eliminarPrestamosPorUsuario(idUsuario);

            System.out.printf("[EVENTO] ✓ USUARIO_ELIMINADO procesado. " +
                    "Usuario %d: %d préstamos eliminados por evento%n",
                    idUsuario, prestamosEliminados);

        } catch (Exception e) {
            System.err.println("[EVENTO] Error al procesar USUARIO_ELIMINADO: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Procesa el evento USUARIO_CREADO.
     * Registra log de auditoría del nuevo usuario.
     */
    @RabbitListener(queues = RabbitMQConfig.COLA_USUARIO_CREADO)
    public void procesarUsuarioCreado(String mensaje) {
        System.out.println("\n[EVENTO] >>> Recibido USUARIO_CREADO: " + mensaje);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> datos = objectMapper.readValue(mensaje, Map.class);
            Long idUsuario = getLong(datos, "idUsuario");
            String email   = (String) datos.getOrDefault("email", "desconocido");

            System.out.printf("[EVENTO] ✓ USUARIO_CREADO registrado. ID=%d, email=%s%n", idUsuario, email);

        } catch (Exception e) {
            System.err.println("[EVENTO] Error al procesar USUARIO_CREADO: " + e.getMessage());
        }
    }

    private Long getLong(Map<String, Object> datos, String clave) {
        Object valor = datos.get(clave);
        if (valor instanceof Integer) return ((Integer) valor).longValue();
        if (valor instanceof Long) return (Long) valor;
        if (valor instanceof Double) return ((Double) valor).longValue();
        if (valor instanceof String) return Long.parseLong((String) valor);
        throw new IllegalArgumentException("Campo '" + clave + "' no encontrado en el evento");
    }
}
