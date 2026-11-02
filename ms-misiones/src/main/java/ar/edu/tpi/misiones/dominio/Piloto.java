package ar.edu.tpi.misiones.dominio;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "piloto")
public class Piloto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario de Keycloak (claim sub) al que pertenece el perfil. */
    @Column(name = "usuario_sub", nullable = false, unique = true, length = 64)
    private String usuarioSub;

    @Column(name = "licencia_nro", nullable = false, length = 40)
    private String licenciaNro;

    @Column(name = "licencia_vence", nullable = false)
    private LocalDate licenciaVence;

    @Column(name = "horas_vuelo", nullable = false, precision = 8, scale = 1)
    private BigDecimal horasVuelo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPiloto estado;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "habilitacion", joinColumns = @JoinColumn(name = "piloto_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_mision", length = 30)
    private Set<TipoMision> habilitaciones = EnumSet.noneOf(TipoMision.class);

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected Piloto() {
    }

    public Piloto(String usuarioSub, String licenciaNro, LocalDate licenciaVence, BigDecimal horasVuelo,
                  Set<TipoMision> habilitaciones, Instant creadoEn) {
        this.usuarioSub = usuarioSub;
        this.licenciaNro = licenciaNro;
        this.licenciaVence = licenciaVence;
        this.horasVuelo = horasVuelo;
        this.habilitaciones = EnumSet.copyOf(habilitaciones);
        this.estado = EstadoPiloto.ACTIVO;
        this.creadoEn = creadoEn;
    }

    public Long getId() {
        return id;
    }

    public String getUsuarioSub() {
        return usuarioSub;
    }

    public String getLicenciaNro() {
        return licenciaNro;
    }

    public LocalDate getLicenciaVence() {
        return licenciaVence;
    }

    public BigDecimal getHorasVuelo() {
        return horasVuelo;
    }

    public EstadoPiloto getEstado() {
        return estado;
    }

    public Set<TipoMision> getHabilitaciones() {
        return Set.copyOf(habilitaciones);
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
