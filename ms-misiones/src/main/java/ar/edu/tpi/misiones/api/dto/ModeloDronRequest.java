package ar.edu.tpi.misiones.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ModeloDronRequest(
        @NotBlank @Size(max = 80) String nombre,
        @NotNull @PositiveOrZero @Digits(integer = 5, fraction = 1) BigDecimal capacidadL,
        @NotNull @Positive @Digits(integer = 4, fraction = 2) BigDecimal haPorHora,
        @NotNull @Positive @Digits(integer = 4, fraction = 2) BigDecimal haPorVuelo,
        @NotNull @Positive @Digits(integer = 4, fraction = 1) BigDecimal limiteRafagaKmh,
        @NotNull Boolean tieneCamara) {
}
