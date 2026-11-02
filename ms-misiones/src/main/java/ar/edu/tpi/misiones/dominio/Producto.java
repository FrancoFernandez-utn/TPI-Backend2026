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
import java.util.EnumSet;
import java.util.Set;

/** Producto fitosanitario del catalogo (solo aplica a PULVERIZACION). */
@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    /** Dias minimos entre dos aplicaciones de este producto sobre el mismo lote (RN-10). */
    @Column(name = "intervalo_dias", nullable = false)
    private int intervaloDias;

    /** Etapas del cultivo en las que no puede aplicarse (RN-09). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "producto_etapa_prohibida", joinColumns = @JoinColumn(name = "producto_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "etapa", length = 30)
    private Set<EtapaCultivo> etapasProhibidas = EnumSet.noneOf(EtapaCultivo.class);

    protected Producto() {
    }

    public Producto(String nombre, int intervaloDias, Set<EtapaCultivo> etapasProhibidas) {
        this.nombre = nombre;
        this.intervaloDias = intervaloDias;
        this.etapasProhibidas = etapasProhibidas.isEmpty()
                ? EnumSet.noneOf(EtapaCultivo.class) : EnumSet.copyOf(etapasProhibidas);
    }

    public boolean prohibidoEn(EtapaCultivo etapa) {
        return etapa != null && etapasProhibidas.contains(etapa);
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getIntervaloDias() {
        return intervaloDias;
    }

    public Set<EtapaCultivo> getEtapasProhibidas() {
        return Set.copyOf(etapasProhibidas);
    }
}
