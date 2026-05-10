package com.biblioteca.eventos.listener;

import com.biblioteca.eventos.config.RabbitMQConfig;
import com.biblioteca.eventos.dao.EventoDAO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Listener de eventos relacionados con préstamos.
 *
 * Eventos manejados:
 *  - PRESTAMO_CREADO  → Decrementa disponibilidad del libro (-1)
 *  - PRESTAMO_DEVUELTO → Incrementa disponibilidad del libro (+1)
 *
 * Este componente demuestra la arquitectura orientada a eventos:
 * las funciones serverless publican eventos y este servicio reacciona.
 */
@Component
public class PrestamoEventListener {

    private final EventoDAO eventoDAO;
    private final ObjectMapper objectMapper;

    public PrestamoEventListener(EventoDAO eventoDAO) {
        this.eventoDAO = eventoDAO;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Procesa el evento PRESTAMO_CREADO.
     * Decrementa la disponibilidad del libro en 1.
     */
    @RabbitListener(queues = RabbitMQConfig.COLA_PRESTAMO_CREADO)
    public void procesarPrestamoCreado(String mensaje) {
        System.out.println("\n[EVENTO] >>> Recibido PRESTAMO_CREADO: " + mensaje);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> datos = objectMapper.readValue(mensaje, Map.class);

            Long idPrestamo = getLong(datos, "idPrestamo");
            Long idUsuario  = getLong(datos, "idUsuario");
            Long idLibro    = getLong(datos, "idLibro");

            System.out.printf("[EVENTO] Procesando: préstamo=%d, usuario=%d, libro=%d%n",
                    idPrestamo, idUsuario, idLibro);

            eventoDAO.decrementarDisponibilidadLibro(idLibro);

            System.out.printf("[EVENTO] ✓ PRESTAMO_CREADO procesado exitosamente. Libro %d: -1 disponibilidad%n", idLibro);

        } catch (Exception e) {
            System.err.println("[EVENTO] Error al procesar PRESTAMO_CREADO: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Procesa el evento PRESTAMO_DEVUELTO.
     * Incrementa la disponibilidad del libro en 1.
     */
    @RabbitListener(queues = RabbitMQConfig.COLA_PRESTAMO_DEVUELTO)
    public void procesarPrestamoDevuelto(String mensaje) {
        System.out.println("\n[EVENTO] >>> Recibido PRESTAMO_DEVUELTO: " + mensaje);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> datos = objectMapper.readValue(mensaje, Map.class);

            Long idPrestamo = getLong(datos, "idPrestamo");
            Long idLibro    = getLong(datos, "idLibro");

            System.out.printf("[EVENTO] Procesando devolución: préstamo=%d, libro=%d%n", idPrestamo, idLibro);

            eventoDAO.incrementarDisponibilidadLibro(idLibro);

            System.out.printf("[EVENTO] ✓ PRESTAMO_DEVUELTO procesado. Libro %d: +1 disponibilidad%n", idLibro);

        } catch (Exception e) {
            System.err.println("[EVENTO] Error al procesar PRESTAMO_DEVUELTO: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Long getLong(Map<String, Object> datos, String clave) {
        Object valor = datos.get(clave);
        if (valor instanceof Integer) return ((Integer) valor).longValue();
        if (valor instanceof Long) return (Long) valor;
        if (valor instanceof Double) return ((Double) valor).longValue();
        if (valor instanceof String) return Long.parseLong((String) valor);
        throw new IllegalArgumentException("Campo '" + clave + "' no encontrado o inválido en el evento");
    }
}
