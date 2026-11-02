package ar.edu.tpi.misiones.api.dto;

import ar.edu.tpi.misiones.dominio.UmbralClima;
import ar.edu.tpi.misiones.dominio.VariableClima;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Rango aceptable de una variable. Un extremo nulo significa "sin limite";
 * al menos uno de los dos tiene que estar definido.
 */
public record UmbralDto(
        @NotNull VariableClima variable,
        BigDecimal minimo,
        Boolean minInclusivo,
        BigDecimal maximo,
        Boolean maxInclusivo) {

    /** Si no se indica si un extremo es inclusivo, se toma como estricto. */
    public UmbralDto {
        minInclusivo = Boolean.TRUE.equals(minInclusivo);
        maxInclusivo = Boolean.TRUE.equals(maxInclusivo);
    }

    @JsonIgnore
    @AssertTrue(message = "cada umbral necesita al menos un extremo y el minimo no puede superar al maximo")
    public boolean isRangoValido() {
        if (minimo == null && maximo == null) {
            return false;
        }
        return minimo == null || maximo == null || minimo.compareTo(maximo) <= 0;
    }

    public static UmbralDto de(UmbralClima u) {
        return new UmbralDto(u.getVariable(), u.getMinimo(), u.isMinInclusivo(), u.getMaximo(), u.isMaxInclusivo());
    }
}
