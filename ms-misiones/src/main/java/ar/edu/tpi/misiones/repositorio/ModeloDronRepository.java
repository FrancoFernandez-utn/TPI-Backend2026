package ar.edu.tpi.misiones.repositorio;

import ar.edu.tpi.misiones.dominio.ModeloDron;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModeloDronRepository extends JpaRepository<ModeloDron, Long> {

    boolean existsByNombreIgnoreCase(String nombre);
}
