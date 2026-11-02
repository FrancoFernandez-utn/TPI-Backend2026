package ar.edu.tpi.misiones.api.dto;

import ar.edu.tpi.misiones.dominio.ModeloDron;
import java.math.BigDecimal;

public record ModeloDronResponse(
        Long id,
        String nombre,
        BigDecimal capacidadL,
        BigDecimal haPorHora,
        BigDecimal haPorVuelo,
        BigDecimal limiteRafagaKmh,
        boolean tieneCamara) {

    public static ModeloDronResponse de(ModeloDron m) {
        return new ModeloDronResponse(m.getId(), m.getNombre(), m.getCapacidadL(), m.getHaPorHora(),
                m.getHaPorVuelo(), m.getLimiteRafagaKmh(), m.isTieneCamara());
    }
}
