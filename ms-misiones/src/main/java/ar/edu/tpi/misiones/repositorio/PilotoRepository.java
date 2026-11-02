package ar.edu.tpi.misiones.repositorio;

import ar.edu.tpi.misiones.dominio.EstadoPiloto;
import ar.edu.tpi.misiones.dominio.Piloto;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PilotoRepository extends JpaRepository<Piloto, Long> {

    Optional<Piloto> findByUsuarioSub(String usuarioSub);

    boolean existsByUsuarioSub(String usuarioSub);

    /** Bloqueo pesimista de los pilotos en el estado indicado, ordenados por id (D-15). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Piloto p where p.estado = :estado order by p.id")
    List<Piloto> bloquearPorEstado(@Param("estado") EstadoPiloto estado);
}
