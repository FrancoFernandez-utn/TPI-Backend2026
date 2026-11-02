package ar.edu.tpi.misiones.api.dto;

import ar.edu.tpi.misiones.dominio.EtapaCultivo;
import ar.edu.tpi.misiones.dominio.Producto;
import java.util.Set;
import java.util.TreeSet;

public record ProductoResponse(Long id, String nombre, int intervaloDias, Set<EtapaCultivo> etapasProhibidas) {

    public static ProductoResponse de(Producto p) {
        return new ProductoResponse(p.getId(), p.getNombre(), p.getIntervaloDias(),
                new TreeSet<>(p.getEtapasProhibidas()));
    }
}
