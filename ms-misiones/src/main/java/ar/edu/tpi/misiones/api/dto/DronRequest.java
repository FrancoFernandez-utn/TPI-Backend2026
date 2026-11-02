package ar.edu.tpi.misiones.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Alta de un dron. El estado inicial lo fija el sistema (OPERATIVO). */
public record DronRequest(
        @NotNull Long modeloId,
        @NotBlank @Size(max = 30) String matricula,
        @NotNull LocalDate registroVence,
        @NotNull LocalDate seguroVence) {
}
