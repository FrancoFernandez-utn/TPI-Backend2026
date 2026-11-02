package ar.edu.tpi.misiones.api.dto;

import ar.edu.tpi.misiones.dominio.TipoMision;
import java.util.List;

public record UmbralesTipoResponse(TipoMision tipo, List<UmbralDto> umbrales) {
}
