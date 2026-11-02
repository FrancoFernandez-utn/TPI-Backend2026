package ar.edu.tpi.misiones.repositorio;

import ar.edu.tpi.misiones.dominio.Piloto;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PilotoRepository extends JpaRepository<Piloto, Long> {

    Optional<Piloto> findByUsuarioSub(String usuarioSub);

    boolean existsByUsuarioSub(String usuarioSub);
}
