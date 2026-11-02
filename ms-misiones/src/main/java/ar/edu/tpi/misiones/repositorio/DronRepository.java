package ar.edu.tpi.misiones.repositorio;

import ar.edu.tpi.misiones.dominio.Dron;
import ar.edu.tpi.misiones.dominio.EstadoDron;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DronRepository extends JpaRepository<Dron, Long> {

    boolean existsByMatriculaIgnoreCase(String matricula);

    List<Dron> findByEstadoOrderByIdAsc(EstadoDron estado);

    /**
     * Bloqueo pesimista (SELECT ... FOR UPDATE) de los drones candidatos, siempre en el mismo orden (por id)
     * para que dos asignaciones simultaneas no se bloqueen mutuamente (D-15).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Dron d where d.id in :ids order by d.id")
    List<Dron> bloquear(@Param("ids") Collection<Long> ids);
}
