package ar.edu.tpi.misiones.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "modelo_dron")
public class ModeloDron {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    /** Capacidad del tanque en litros. Mayor a 0 habilita PULVERIZACION (RN-05). */
    @Column(name = "capacidad_l", nullable = false, precision = 6, scale = 1)
    private BigDecimal capacidadL;

    /** Rendimiento en hectareas por hora (RN-14). */
    @Column(name = "ha_por_hora", nullable = false, precision = 6, scale = 2)
    private BigDecimal haPorHora;

    /** Hectareas que cubre una bateria (RN-14). */
    @Column(name = "ha_por_vuelo", nullable = false, precision = 6, scale = 2)
    private BigDecimal haPorVuelo;

    /** Rafaga maxima que tolera el modelo (tabla de la seccion 5.3). */
    @Column(name = "limite_rafaga_kmh", nullable = false, precision = 5, scale = 1)
    private BigDecimal limiteRafagaKmh;

    /** Necesaria para MONITOREO (RN-05). */
    @Column(name = "tiene_camara", nullable = false)
    private boolean tieneCamara;

    protected ModeloDron() {
    }

    public ModeloDron(String nombre, BigDecimal capacidadL, BigDecimal haPorHora, BigDecimal haPorVuelo,
                      BigDecimal limiteRafagaKmh, boolean tieneCamara) {
        this.nombre = nombre;
        this.capacidadL = capacidadL;
        this.haPorHora = haPorHora;
        this.haPorVuelo = haPorVuelo;
        this.limiteRafagaKmh = limiteRafagaKmh;
        this.tieneCamara = tieneCamara;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getCapacidadL() {
        return capacidadL;
    }

    public BigDecimal getHaPorHora() {
        return haPorHora;
    }

    public BigDecimal getHaPorVuelo() {
        return haPorVuelo;
    }

    public BigDecimal getLimiteRafagaKmh() {
        return limiteRafagaKmh;
    }

    public boolean isTieneCamara() {
        return tieneCamara;
    }
}
