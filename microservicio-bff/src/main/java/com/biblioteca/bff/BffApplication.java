package com.biblioteca.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Microservicio BFF (Backend For Frontend) v2.0.
 * Orquesta las funciones serverless de usuarios y préstamos.
 * Expone REST (/api/*) y GraphQL (/graphql) con GraphiQL en /graphiql.
 */
@SpringBootApplication
public class BffApplication {
    public static void main(String[] args) {
        SpringApplication.run(BffApplication.class, args);
    }
}
