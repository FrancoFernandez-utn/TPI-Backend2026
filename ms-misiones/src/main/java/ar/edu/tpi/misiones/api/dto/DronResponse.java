package ar.edu.tpi.misiones.api.dto;

import ar.edu.tpi.misiones.dominio.Dron;
import ar.edu.tpi.misiones.dominio.EstadoDron;
import java.time.LocalDate;

public record DronResponse(
        Long id,
        Long modeloId,
        String modeloNombre,
        String matricula,
        LocalDate registroVence,
        LocalDate seguroVence,
        EstadoDron estado) {

    public static DronResponse de(Dron d) {
        return new DronResponse(d.getId(), d.getModelo().getId(), d.getModelo().getNombre(), d.getMatricula(),
                d.getRegistroVence(), d.getSeguroVence(), d.getEstado());
    }
}
