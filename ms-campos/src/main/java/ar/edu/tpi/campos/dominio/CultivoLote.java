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
import java.time.Instant;

/** Registro del historial de cultivo y etapa de un lote. Nunca se modifica: cada cambio agrega una fila. */
@Entity
@Table(name = "cultivo_lote")
public class CultivoLote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id", nullable = false)
    private Lote lote;

    @Column(nullable = false, length = 60)
    private String cultivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EtapaCultivo etapa;

    @Column(nullable = false)
    private Instant desde;

    protected CultivoLote() {
    }

    public CultivoLote(Lote lote, String cultivo, EtapaCultivo etapa, Instant desde) {
        this.lote = lote;
        this.cultivo = cultivo;
        this.etapa = etapa;
        this.desde = desde;
    }

    public Long getId() {
        return id;
    }

    public Lote getLote() {
        return lote;
    }

    public String getCultivo() {
        return cultivo;
    }

    public EtapaCultivo getEtapa() {
        return etapa;
    }

    public Instant getDesde() {
        return desde;
    }
}
