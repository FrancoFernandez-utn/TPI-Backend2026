package ar.edu.tpi.misiones.servicio;

import ar.edu.tpi.misiones.dominio.EvaluacionMision;
import ar.edu.tpi.misiones.dominio.Mision;
import ar.edu.tpi.misiones.repositorio.EvaluacionMisionRepository;
import ar.edu.tpi.misiones.repositorio.MisionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Guarda una mision y, si se consulto el clima, la evaluacion que justifico la decision. */
@Service
public class RegistroMisiones {

    private final MisionRepository misiones;
    private final EvaluacionMisionRepository evaluaciones;

    public RegistroMisiones(MisionRepository misiones, EvaluacionMisionRepository evaluaciones) {
        this.misiones = misiones;
        this.evaluaciones = evaluaciones;
    }

    /** Se une a la transaccion en curso si la hay (por ejemplo, la de la asignacion con bloqueo). */
    @Transactional
    public Mision guardar(Mision mision, DatosEvaluacion evaluacion) {
        Mision guardada = misiones.save(mision);
        if (evaluacion != null) {
            evaluaciones.save(new EvaluacionMision(guardada, evaluacion.consultadaEn(), evaluacion.fuente(),
                    evaluacion.resultado(), evaluacion.variablesJson(), evaluacion.ventanasJson(),
                    evaluacion.motivosJson()));
        }
        return guardada;
    }
}
