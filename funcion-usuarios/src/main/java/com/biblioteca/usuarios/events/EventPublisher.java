package com.biblioteca.usuarios.events;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * Publicador de eventos hacia RabbitMQ.
 * Publica en el exchange "biblioteca.events" con routing key específico.
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
     * Publica un evento cuando un usuario es eliminado.
     * El microservicio-eventos consume este mensaje y elimina sus préstamos.
     */
    public void publicarUsuarioEliminado(Long idUsuario) {
        String mensaje = String.format(
                "{\"tipo\": \"USUARIO_ELIMINADO\", \"idUsuario\": %d, \"timestamp\": \"%s\"}",
                idUsuario, java.time.Instant.now().toString()
        );
        publicar("usuario.eliminado", mensaje);
    }

    /**
     * Publica un evento cuando se crea un usuario (para auditoría/notificaciones).
     */
    public void publicarUsuarioCreado(Long idUsuario, String email) {
        String mensaje = String.format(
                "{\"tipo\": \"USUARIO_CREADO\", \"idUsuario\": %d, \"email\": \"%s\", \"timestamp\": \"%s\"}",
                idUsuario, email, java.time.Instant.now().toString()
        );
        publicar("usuario.creado", mensaje);
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
            System.err.println("[EVENTO] Error al publicar evento '" + routingKey + "': " + e.getMessage());
        }
    }
}
