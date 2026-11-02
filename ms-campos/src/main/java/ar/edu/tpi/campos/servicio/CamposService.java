package ar.edu.tpi.campos.servicio;

import ar.edu.tpi.campos.api.dto.ContextoLoteResponse;
import ar.edu.tpi.campos.api.dto.CultivoRequest;
import ar.edu.tpi.campos.api.dto.EstablecimientoRequest;
import ar.edu.tpi.campos.api.dto.EstablecimientoResponse;
import ar.edu.tpi.campos.api.dto.LoteRequest;
import ar.edu.tpi.campos.api.dto.LoteResponse;
import ar.edu.tpi.campos.dominio.CultivoLote;
import ar.edu.tpi.campos.dominio.Establecimiento;
import ar.edu.tpi.campos.dominio.Lote;
import ar.edu.tpi.campos.repositorio.CultivoLoteRepository;
import ar.edu.tpi.campos.repositorio.EstablecimientoRepository;
import ar.edu.tpi.campos.repositorio.LoteRepository;
import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logica de ms-campos: titularidad de establecimientos y lotes, e historial de cultivo y etapa.
 *
 * <p>Regla de propiedad: un productor solo opera sobre sus recursos. Ante un recurso ajeno se
 * responde como si no existiera (404), para no revelar informacion de otros productores.
 */
@Service
public class CamposService {

    private final EstablecimientoRepository establecimientos;
    private final LoteRepository lotes;
    private final CultivoLoteRepository cultivos;
    private final Clock reloj;

    public CamposService(EstablecimientoRepository establecimientos, LoteRepository lotes,
                         CultivoLoteRepository cultivos, Clock reloj) {
        this.establecimientos = establecimientos;
        this.lotes = lotes;
        this.cultivos = cultivos;
        this.reloj = reloj;
    }

    @Transactional
    public EstablecimientoResponse crearEstablecimiento(String productorSub, EstablecimientoRequest req) {
        Establecimiento nuevo = establecimientos.save(
                new Establecimiento(productorSub, req.nombre().trim(), Instant.now(reloj)));
        return EstablecimientoResponse.de(nuevo);
    }

    @Transactional
    public LoteResponse crearLote(String productorSub, Long establecimientoId, LoteRequest req) {
        Establecimiento establecimiento = establecimientos.findById(establecimientoId)
                .filter(e -> e.perteneceA(productorSub))
                .orElseThrow(() -> new RecursoNoEncontradoException("Establecimiento", establecimientoId));
        Instant ahora = Instant.now(reloj);
        Lote lote = lotes.save(new Lote(establecimiento, req.nombre().trim(), req.lat(), req.lon(),
                req.hectareas(), ahora));
        CultivoLote cultivo = cultivos.save(new CultivoLote(lote, req.cultivo().trim(), req.etapa(), ahora));
        return LoteResponse.de(lote, cultivo);
    }

    /** Registra un nuevo cultivo o etapa. No pisa el anterior: agrega una fila al historial. */
    @Transactional
    public LoteResponse cambiarCultivo(String productorSub, Long loteId, CultivoRequest req) {
        Lote lote = lotePropio(productorSub, loteId);
        CultivoLote cultivo = cultivos.save(
                new CultivoLote(lote, req.cultivo().trim(), req.etapa(), Instant.now(reloj)));
        return LoteResponse.de(lote, cultivo);
    }

    @Transactional(readOnly = true)
    public List<LoteResponse> lotesDe(String productorSub) {
        List<Lote> propios = lotes.buscarPorTitular(productorSub);
        if (propios.isEmpty()) {
            return List.of();
        }
        Map<Long, CultivoLote> actuales = new LinkedHashMap<>();
        for (CultivoLote c : cultivos.buscarHistorialDeLotes(propios.stream().map(Lote::getId).toList())) {
            actuales.putIfAbsent(c.getLote().getId(), c);
        }
        return propios.stream().map(l -> LoteResponse.de(l, actuales.get(l.getId()))).toList();
    }

    /** Contexto completo del lote para ms-misiones. No filtra por titular: lo hace quien decide la mision. */
    @Transactional(readOnly = true)
    public ContextoLoteResponse contexto(Long loteId) {
        Lote lote = lotes.buscarConEstablecimiento(loteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Lote", loteId));
        CultivoLote actual = cultivos.findFirstByLoteIdOrderByDesdeDescIdDesc(loteId).orElse(null);
        return ContextoLoteResponse.de(lote, actual);
    }

    private Lote lotePropio(String productorSub, Long loteId) {
        return lotes.buscarConEstablecimiento(loteId)
                .filter(l -> l.getTitularSub().equals(productorSub))
                .orElseThrow(() -> new RecursoNoEncontradoException("Lote", loteId));
    }
}
