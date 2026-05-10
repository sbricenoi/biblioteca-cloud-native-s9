package com.biblioteca.eventos.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de RabbitMQ para el microservicio de eventos.
 *
 * Topología:
 *   Exchange: biblioteca.events (tipo: topic, durable)
 *   Colas:
 *     - cola.prestamo.creado   → routing key: prestamo.creado
 *     - cola.prestamo.devuelto → routing key: prestamo.devuelto
 *     - cola.usuario.eliminado → routing key: usuario.eliminado
 *     - cola.usuario.creado    → routing key: usuario.creado
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "biblioteca.events";

    public static final String COLA_PRESTAMO_CREADO   = "cola.prestamo.creado";
    public static final String COLA_PRESTAMO_DEVUELTO = "cola.prestamo.devuelto";
    public static final String COLA_USUARIO_ELIMINADO = "cola.usuario.eliminado";
    public static final String COLA_USUARIO_CREADO    = "cola.usuario.creado";

    // ── Exchange ──────────────────────────────────────────────────────────────

    @Bean
    public TopicExchange bibliotecaExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    // ── Colas ─────────────────────────────────────────────────────────────────

    @Bean public Queue colaPrestamoCreado()   { return QueueBuilder.durable(COLA_PRESTAMO_CREADO).build(); }
    @Bean public Queue colaPrestamoDevuelto() { return QueueBuilder.durable(COLA_PRESTAMO_DEVUELTO).build(); }
    @Bean public Queue colaUsuarioEliminado() { return QueueBuilder.durable(COLA_USUARIO_ELIMINADO).build(); }
    @Bean public Queue colaUsuarioCreado()    { return QueueBuilder.durable(COLA_USUARIO_CREADO).build(); }

    // ── Bindings ──────────────────────────────────────────────────────────────

    @Bean
    public Binding bindingPrestamoCreado(Queue colaPrestamoCreado, TopicExchange bibliotecaExchange) {
        return BindingBuilder.bind(colaPrestamoCreado).to(bibliotecaExchange).with("prestamo.creado");
    }

    @Bean
    public Binding bindingPrestamoDevuelto(Queue colaPrestamoDevuelto, TopicExchange bibliotecaExchange) {
        return BindingBuilder.bind(colaPrestamoDevuelto).to(bibliotecaExchange).with("prestamo.devuelto");
    }

    @Bean
    public Binding bindingUsuarioEliminado(Queue colaUsuarioEliminado, TopicExchange bibliotecaExchange) {
        return BindingBuilder.bind(colaUsuarioEliminado).to(bibliotecaExchange).with("usuario.eliminado");
    }

    @Bean
    public Binding bindingUsuarioCreado(Queue colaUsuarioCreado, TopicExchange bibliotecaExchange) {
        return BindingBuilder.bind(colaUsuarioCreado).to(bibliotecaExchange).with("usuario.creado");
    }

    // Nota: Se usa el SimpleMessageConverter por defecto (auto-configurado por Spring Boot AMQP).
    // Los listeners reciben String (JSON crudo) y lo deserializan manualmente con ObjectMapper.
    // Esto es compatible con los publishers que usan amqp-client puro (sin Spring AMQP).
}
