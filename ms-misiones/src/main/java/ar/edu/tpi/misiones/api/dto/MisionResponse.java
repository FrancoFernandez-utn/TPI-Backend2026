package ar.edu.tpi.misiones.api.dto;

import ar.edu.tpi.misiones.dominio.EstadoMision;
import ar.edu.tpi.misiones.dominio.Motivo;
import ar.edu.tpi.misiones.dominio.ResultadoEvaluacion;
import ar.edu.tpi.misiones.dominio.TipoMision;
import java.time.Instant;
import java.util.List;

public record MisionResponse(
        Long id,
        EstadoMision estado,
        TipoMision tipo,
        Long loteId,
        Long productoId,
        String recetaNro,
        Instant fechaLimite,
        VentanaAsignada ventana,
        Long pilotoId,
        Long dronId,
        Integer vuelos,
        Integer duracionEstimadaMin,
        List<Motivo> motivos,
        EvaluacionResumen evaluacion,
        Instant creadaEn) {

    public record VentanaAsignada(Instant inicio, Instant fin) {
    }

    public record EvaluacionResumen(Long id, Instant consultadaEn, String fuente, ResultadoEvaluacion resultado) {
    }
}
