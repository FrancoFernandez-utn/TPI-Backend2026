package ar.edu.tpi.campos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "lote")
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establecimiento_id", nullable = false)
    private Establecimiento establecimiento;

    @Column(nullable = false, length = 120)
    private String nombre;

    /** Latitud del centro del lote. */
    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lat;

    /** Longitud del centro del lote. */
    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lon;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal hectareas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoLote estado;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected Lote() {
    }

    public Lote(Establecimiento establecimiento, String nombre, BigDecimal lat, BigDecimal lon,
                BigDecimal hectareas, Instant creadoEn) {
        this.establecimiento = establecimiento;
        this.nombre = nombre;
        this.lat = lat;
        this.lon = lon;
        this.hectareas = hectareas;
        this.estado = EstadoLote.ACTIVO;
        this.creadoEn = creadoEn;
    }

    /** El titular del lote es el productor de su establecimiento. */
    public String getTitularSub() {
        return establecimiento.getProductorSub();
    }

    public Long getId() {
        return id;
    }

    public Establecimiento getEstablecimiento() {
        return establecimiento;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getLat() {
        return lat;
    }

    public BigDecimal getLon() {
        return lon;
    }

    public BigDecimal getHectareas() {
        return hectareas;
    }

    public EstadoLote getEstado() {
        return estado;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
