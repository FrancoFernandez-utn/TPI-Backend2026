package ar.edu.tpi.misiones.api.dto;

import ar.edu.tpi.misiones.dominio.EtapaCultivo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record ProductoRequest(
        @NotBlank @Size(max = 80) String nombre,
        @NotNull @PositiveOrZero @Max(3650) Integer intervaloDias,
        Set<@NotNull EtapaCultivo> etapasProhibidas) {
}
