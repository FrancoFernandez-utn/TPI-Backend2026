package ar.edu.tpi.campos.api.dto;

import ar.edu.tpi.campos.dominio.EtapaCultivo;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Alta de un lote: centro geografico, superficie y cultivo con su etapa actual.
 */
public record LoteRequest(
        @NotBlank @Size(max = 120) String nombre,
        @NotNull @DecimalMin("-90") @DecimalMax("90") @Digits(integer = 3, fraction = 6) BigDecimal lat,
        @NotNull @DecimalMin("-180") @DecimalMax("180") @Digits(integer = 3, fraction = 6) BigDecimal lon,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal hectareas,
        @NotBlank @Size(max = 60) String cultivo,
        @NotNull EtapaCultivo etapa) {
}
