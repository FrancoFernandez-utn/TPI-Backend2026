package ar.edu.tpi.campos.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EstablecimientoRequest(
        @NotBlank @Size(max = 120) String nombre) {
}
