package com.biblioteca.prestamos.model;

import java.sql.Date;
import java.sql.Timestamp;

public class Prestamo {
    private Long id;
    private Long idUsuario;
    private Long idLibro;
    private Timestamp fechaPrestamo;
    private Date fechaDevolucionEsperada;
    private Timestamp fechaDevolucionReal;
    private String estado;

    public Prestamo() {}

    public Prestamo(Long id, Long idUsuario, Long idLibro, Timestamp fechaPrestamo,
                    Date fechaDevolucionEsperada, Timestamp fechaDevolucionReal, String estado) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.idLibro = idLibro;
        this.fechaPrestamo = fechaPrestamo;
        this.fechaDevolucionEsperada = fechaDevolucionEsperada;
        this.fechaDevolucionReal = fechaDevolucionReal;
        this.estado = estado;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }
    public Long getIdLibro() { return idLibro; }
    public void setIdLibro(Long idLibro) { this.idLibro = idLibro; }
    public Timestamp getFechaPrestamo() { return fechaPrestamo; }
    public void setFechaPrestamo(Timestamp fechaPrestamo) { this.fechaPrestamo = fechaPrestamo; }
    public Date getFechaDevolucionEsperada() { return fechaDevolucionEsperada; }
    public void setFechaDevolucionEsperada(Date fechaDevolucionEsperada) { this.fechaDevolucionEsperada = fechaDevolucionEsperada; }
    public Timestamp getFechaDevolucionReal() { return fechaDevolucionReal; }
    public void setFechaDevolucionReal(Timestamp fechaDevolucionReal) { this.fechaDevolucionReal = fechaDevolucionReal; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
