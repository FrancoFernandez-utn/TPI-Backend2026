package ar.edu.tpi.campos.repositorio;

import ar.edu.tpi.campos.dominio.CultivoLote;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CultivoLoteRepository extends JpaRepository<CultivoLote, Long> {

    /** Cultivo y etapa vigentes: el registro mas reciente del historial. */
    Optional<CultivoLote> findFirstByLoteIdOrderByDesdeDescIdDesc(Long loteId);

    /** Historial de varios lotes, del mas reciente al mas antiguo dentro de cada lote. */
    @Query("select c from CultivoLote c where c.lote.id in :loteIds order by c.lote.id, c.desde desc, c.id desc")
    List<CultivoLote> buscarHistorialDeLotes(@Param("loteIds") Collection<Long> loteIds);
}
