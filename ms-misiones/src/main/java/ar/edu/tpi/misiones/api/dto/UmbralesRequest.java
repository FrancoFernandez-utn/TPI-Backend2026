package ar.edu.tpi.misiones.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Reemplaza todos los umbrales de un tipo de mision. Una variable ausente queda sin restriccion. */
public record UmbralesRequest(@NotNull List<@Valid @NotNull UmbralDto> umbrales) {
}
