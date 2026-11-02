package ar.edu.tpi.campos.repositorio;

import ar.edu.tpi.campos.dominio.Establecimiento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstablecimientoRepository extends JpaRepository<Establecimiento, Long> {
}
