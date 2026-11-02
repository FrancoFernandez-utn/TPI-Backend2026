package ar.edu.tpi.campos.repositorio;

import ar.edu.tpi.campos.dominio.Lote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoteRepository extends JpaRepository<Lote, Long> {

    @Query("select l from Lote l join fetch l.establecimiento where l.id = :id")
    Optional<Lote> buscarConEstablecimiento(@Param("id") Long id);

    @Query("select l from Lote l join fetch l.establecimiento e where e.productorSub = :sub order by l.id")
    List<Lote> buscarPorTitular(@Param("sub") String productorSub);
}
