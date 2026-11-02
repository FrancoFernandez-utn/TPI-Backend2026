package ar.edu.tpi.misiones.api.dto;

import ar.edu.tpi.misiones.dominio.TipoMision;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Solicitud de mision. RN-08 se valida como regla de entrada: toda PULVERIZACION trae producto y receta;
 * los otros tipos no llevan producto.
 */
public record SolicitudMisionRequest(
        @NotNull @Positive Long loteId,
        @NotNull TipoMision tipo,
        @Positive Long productoId,
        @Size(max = 40) String recetaNro,
        @NotNull @Future Instant fechaLimite) {

    @JsonIgnore
    @AssertTrue(message = "PULVERIZACION requiere productoId y recetaNro (RN-08)")
    public boolean isPulverizacionCompleta() {
        return tipo != TipoMision.PULVERIZACION
                || (productoId != null && recetaNro != null && !recetaNro.isBlank());
    }

    @JsonIgnore
    @AssertTrue(message = "solo PULVERIZACION lleva producto y receta")
    public boolean isSinProductoSiNoPulveriza() {
        return tipo == null || tipo == TipoMision.PULVERIZACION || (productoId == null && recetaNro == null);
    }
}
