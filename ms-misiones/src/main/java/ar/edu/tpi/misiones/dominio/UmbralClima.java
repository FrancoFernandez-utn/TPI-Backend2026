package ar.edu.tpi.misiones.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Rango aceptable de una variable para un tipo de mision. Un extremo nulo significa "sin limite".
 * Los valores iniciales salen de la seccion 5.3 del dominio y el ADMIN puede editarlos.
 */
@Entity
@Table(name = "umbral_clima")
public class UmbralClima {

    @EmbeddedId
    private UmbralClimaId id;

    @Column(precision = 7, scale = 2)
    private BigDecimal minimo;

    @Column(name = "min_inclusivo", nullable = false)
    private boolean minInclusivo;

    @Column(precision = 7, scale = 2)
    private BigDecimal maximo;

    @Column(name = "max_inclusivo", nullable = false)
    private boolean maxInclusivo;

    protected UmbralClima() {
    }

    public UmbralClima(TipoMision tipo, VariableClima variable, BigDecimal minimo, boolean minInclusivo,
                       BigDecimal maximo, boolean maxInclusivo) {
        this.id = new UmbralClimaId(tipo, variable);
        this.minimo = minimo;
        this.minInclusivo = minInclusivo;
        this.maximo = maximo;
        this.maxInclusivo = maxInclusivo;
    }

    public TipoMision getTipo() {
        return id.getTipoMision();
    }

    public VariableClima getVariable() {
        return id.getVariable();
    }

    public BigDecimal getMinimo() {
        return minimo;
    }

    public boolean isMinInclusivo() {
        return minInclusivo;
    }

    public BigDecimal getMaximo() {
        return maximo;
    }

    public boolean isMaxInclusivo() {
        return maxInclusivo;
    }
}
