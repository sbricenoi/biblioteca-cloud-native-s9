package com.biblioteca.bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de las URLs de las funciones serverless.
 * Se leen desde application.properties o variables de entorno.
 */
@Configuration
@ConfigurationProperties(prefix = "serverless")
public class ServerlessConfig {

    private Usuarios usuarios = new Usuarios();
    private Prestamos prestamos = new Prestamos();

    public Usuarios getUsuarios() { return usuarios; }
    public void setUsuarios(Usuarios usuarios) { this.usuarios = usuarios; }
    public Prestamos getPrestamos() { return prestamos; }
    public void setPrestamos(Prestamos prestamos) { this.prestamos = prestamos; }

    public static class Usuarios {
        private String url = "http://localhost:8081";
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public static class Prestamos {
        private String url = "http://localhost:8082";
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}
