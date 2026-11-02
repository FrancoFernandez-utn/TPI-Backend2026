package ar.edu.tpi.misiones.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UmbralClimaId implements Serializable {

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_mision", length = 30)
    private TipoMision tipoMision;

    @Enumerated(EnumType.STRING)
    @Column(name = "variable", length = 20)
    private VariableClima variable;

    protected UmbralClimaId() {
    }

    public UmbralClimaId(TipoMision tipoMision, VariableClima variable) {
        this.tipoMision = tipoMision;
        this.variable = variable;
    }

    public TipoMision getTipoMision() {
        return tipoMision;
    }

    public VariableClima getVariable() {
        return variable;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof UmbralClimaId otro && tipoMision == otro.tipoMision && variable == otro.variable;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tipoMision, variable);
    }
}
