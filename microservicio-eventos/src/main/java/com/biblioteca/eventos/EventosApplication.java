package com.biblioteca.eventos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio de Eventos v2.0.
 * Consume eventos de RabbitMQ y ejecuta lógica de negocio orientada a eventos:
 *
 *  - PRESTAMO_CREADO  → Decrementa disponibilidad del libro en 1
 *  - PRESTAMO_DEVUELTO → Incrementa disponibilidad del libro en 1
 *  - USUARIO_ELIMINADO → Elimina todos los préstamos del usuario
 *  - USUARIO_CREADO   → Registra auditoría
 */
@SpringBootApplication
public class EventosApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventosApplication.class, args);
    }
}
