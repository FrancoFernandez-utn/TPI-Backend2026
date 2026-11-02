package ar.edu.tpi.misiones.repositorio;

import ar.edu.tpi.misiones.dominio.Dron;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DronRepository extends JpaRepository<Dron, Long> {

    boolean existsByMatriculaIgnoreCase(String matricula);
}
