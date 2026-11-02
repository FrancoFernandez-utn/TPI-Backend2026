package ar.edu.tpi.misiones.servicio;

import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import ar.edu.tpi.commons.seguridad.UsuarioActual;
import ar.edu.tpi.misiones.api.dto.MisionResponse;
import ar.edu.tpi.misiones.api.dto.SolicitudMisionRequest;
import ar.edu.tpi.misiones.api.dto.VentanaPropuesta;
import ar.edu.tpi.misiones.dominio.EvaluacionMision;
import ar.edu.tpi.misiones.dominio.Mision;
import ar.edu.tpi.misiones.dominio.Motivo;
import ar.edu.tpi.misiones.repositorio.EvaluacionMisionRepository;
import ar.edu.tpi.misiones.repositorio.MisionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

/**
 * Casos de uso de la mision expuestos por la API: solicitar, consultar, ver ventanas y cancelar.
 *
 * <p>Visibilidad: el PRODUCTOR ve sus misiones, el PILOTO las que tiene asignadas y el ADMIN todas.
 * Cualquier otra combinacion responde 404, como si la mision no existiera.
 */
@Service
public class MisionService {

    private static final TypeReference<List<Motivo>> LISTA_MOTIVOS = new TypeReference<>() {
    };
    private static final TypeReference<List<VentanaPropuesta>> LISTA_VENTANAS = new TypeReference<>() {
    };

    private final SolicitudMisionService solicitudes;
    private final MisionRepository misiones;
    private final EvaluacionMisionRepository evaluaciones;
    private final Json json;
    private final Clock reloj;

    public MisionService(SolicitudMisionService solicitudes, MisionRepository misiones,
                         EvaluacionMisionRepository evaluaciones, Json json, Clock reloj) {
        this.solicitudes = solicitudes;
        this.misiones = misiones;
        this.evaluaciones = evaluaciones;
        this.json = json;
        this.reloj = reloj;
    }

    public MisionResponse solicitar(SolicitudMisionRequest req) {
        Mision mision = solicitudes.solicitar(UsuarioActual.sub(), req);
        return aRespuesta(mision);
    }

    @Transactional(readOnly = true)
    public MisionResponse obtener(Long id) {
        return aRespuesta(visible(id));
    }

    @Transactional(readOnly = true)
    public List<MisionResponse> mias() {
        String sub = UsuarioActual.sub();
        List<Mision> lista = UsuarioActual.tieneRol("PRODUCTOR")
                ? misiones.findByProductorSubOrderByCreadaEnDesc(sub)
                : misiones.findByPilotoUsuarioSubOrderByInicioPrevAsc(sub);
        return lista.stream().map(this::aRespuesta).toList();
    }

    /** Ventanas aptas encontradas en la ultima evaluacion climatica de la mision. */
    @Transactional(readOnly = true)
    public List<VentanaPropuesta> ventanas(Long id) {
        Mision mision = visible(id);
        return evaluaciones.findFirstByMisionIdOrderByConsultadaEnDescIdDesc(mision.getId())
                .map(e -> json.leer(e.getVentanasJson(), LISTA_VENTANAS))
                .orElse(List.of());
    }

    /** D-06: el productor cancela una mision AUTORIZADA o PROGRAMADA; libera piloto y dron. */
    @Transactional
    public MisionResponse cancelar(Long id) {
        Mision mision = misiones.findById(id)
                .filter(m -> m.perteneceA(UsuarioActual.sub()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Mision", id));
        mision.cancelar(Instant.now(reloj));
        return aRespuesta(misiones.save(mision));
    }

    private Mision visible(Long id) {
        String sub = UsuarioActual.sub();
        return misiones.findById(id)
                .filter(m -> UsuarioActual.tieneRol("ADMIN")
                        || (UsuarioActual.tieneRol("PRODUCTOR") && m.perteneceA(sub))
                        || (UsuarioActual.tieneRol("PILOTO") && m.asignadaA(sub)))
                .orElseThrow(() -> new RecursoNoEncontradoException("Mision", id));
    }

    private MisionResponse aRespuesta(Mision m) {
        EvaluacionMision evaluacion = m.getId() == null ? null
                : evaluaciones.findFirstByMisionIdOrderByConsultadaEnDescIdDesc(m.getId()).orElse(null);
        return new MisionResponse(
                m.getId(), m.getEstado(), m.getTipo(), m.getLoteId(),
                m.getProducto() == null ? null : m.getProducto().getId(),
                m.getRecetaNro(), m.getFechaLimite(),
                m.getInicioPrev() == null ? null : new MisionResponse.VentanaAsignada(m.getInicioPrev(), m.getFinPrev()),
                m.getPiloto() == null ? null : m.getPiloto().getId(),
                m.getDron() == null ? null : m.getDron().getId(),
                m.getVuelos(), m.getDuracionMin(),
                json.leer(m.getMotivosJson(), LISTA_MOTIVOS),
                evaluacion == null ? null : new MisionResponse.EvaluacionResumen(evaluacion.getId(),
                        evaluacion.getConsultadaEn(), evaluacion.getFuente(), evaluacion.getResultado()),
                m.getCreadaEn());
    }
}
