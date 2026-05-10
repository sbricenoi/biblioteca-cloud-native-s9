package com.biblioteca.prestamos.events;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * Publicador de eventos hacia RabbitMQ desde la función de préstamos.
 * Exchange: "biblioteca.events" (tipo topic, durable).
 */
public class EventPublisher {

    private static final String EXCHANGE_NAME = "biblioteca.events";
    private static final String EXCHANGE_TYPE = "topic";

    private final ConnectionFactory factory;

    public EventPublisher() {
        factory = new ConnectionFactory();
        factory.setHost(System.getenv().getOrDefault("RABBITMQ_HOST", "rabbitmq"));
        factory.setPort(Integer.parseInt(System.getenv().getOrDefault("RABBITMQ_PORT", "5672")));
        factory.setUsername(System.getenv().getOrDefault("RABBITMQ_USER", "guest"));
        factory.setPassword(System.getenv().getOrDefault("RABBITMQ_PASSWORD", "guest"));
    }

    /**
     * Publica evento PRESTAMO_CREADO para que microservicio-eventos
     * actualice la disponibilidad del libro (-1).
     */
    public void publicarPrestamoCreado(Long idPrestamo, Long idUsuario, Long idLibro) {
        String mensaje = String.format(
                "{\"tipo\": \"PRESTAMO_CREADO\", \"idPrestamo\": %d, \"idUsuario\": %d, \"idLibro\": %d, \"timestamp\": \"%s\"}",
                idPrestamo, idUsuario, idLibro, java.time.Instant.now().toString()
        );
        publicar("prestamo.creado", mensaje);
    }

    /**
     * Publica evento PRESTAMO_DEVUELTO para actualizar disponibilidad (+1).
     */
    public void publicarPrestamoDevuelto(Long idPrestamo, Long idLibro) {
        String mensaje = String.format(
                "{\"tipo\": \"PRESTAMO_DEVUELTO\", \"idPrestamo\": %d, \"idLibro\": %d, \"timestamp\": \"%s\"}",
                idPrestamo, idLibro, java.time.Instant.now().toString()
        );
        publicar("prestamo.devuelto", mensaje);
    }

    private void publicar(String routingKey, String mensaje) {
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(EXCHANGE_NAME, EXCHANGE_TYPE, true);
            channel.basicPublish(
                    EXCHANGE_NAME,
                    routingKey,
                    null,
                    mensaje.getBytes(StandardCharsets.UTF_8)
            );
            System.out.println("[EVENTO] Publicado '" + routingKey + "': " + mensaje);

        } catch (IOException | TimeoutException e) {
            System.err.println("[EVENTO] Error al publicar '" + routingKey + "': " + e.getMessage());
        }
    }
}
