package ar.edu.tpi.misiones.motor;

import java.util.List;

/** Pronostico horario de un punto, ordenado por instante, con las horas de luz de cada dia. */
public record Pronostico(List<CondicionHora> horas, List<LuzSolar> luz) {

    public Pronostico {
        horas = List.copyOf(horas);
        luz = List.copyOf(luz);
    }
}
