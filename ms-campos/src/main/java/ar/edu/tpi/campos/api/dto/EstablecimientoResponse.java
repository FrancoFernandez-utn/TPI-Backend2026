package ar.edu.tpi.campos.api.dto;

import ar.edu.tpi.campos.dominio.Establecimiento;
import java.time.Instant;

public record EstablecimientoResponse(Long id, String nombre, Instant creadoEn) {

    public static EstablecimientoResponse de(Establecimiento e) {
        return new EstablecimientoResponse(e.getId(), e.getNombre(), e.getCreadoEn());
    }
}
