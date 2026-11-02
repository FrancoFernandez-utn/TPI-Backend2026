package ar.edu.tpi.misiones.api.dto;

import ar.edu.tpi.misiones.dominio.TipoMision;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/** Perfil del piloto autenticado: licencia, horas de vuelo registradas y habilitaciones. */
public record PilotoRequest(
        @NotBlank @Size(max = 40) String licenciaNro,
        @NotNull LocalDate licenciaVence,
        @NotNull @PositiveOrZero @Digits(integer = 7, fraction = 1) BigDecimal horasVuelo,
        @NotEmpty Set<@NotNull TipoMision> habilitaciones) {
}
