package ar.edu.tpi.campos.api.dto;

import ar.edu.tpi.campos.dominio.EtapaCultivo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CultivoRequest(
        @NotBlank @Size(max = 60) String cultivo,
        @NotNull EtapaCultivo etapa) {
}
