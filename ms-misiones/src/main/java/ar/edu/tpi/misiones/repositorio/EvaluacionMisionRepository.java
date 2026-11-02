package ar.edu.tpi.misiones.repositorio;

import ar.edu.tpi.misiones.dominio.EvaluacionMision;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluacionMisionRepository extends JpaRepository<EvaluacionMision, Long> {

    Optional<EvaluacionMision> findFirstByMisionIdOrderByConsultadaEnDescIdDesc(Long misionId);
}
