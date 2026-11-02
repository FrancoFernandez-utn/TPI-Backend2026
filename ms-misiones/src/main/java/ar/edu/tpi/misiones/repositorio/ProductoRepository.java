package ar.edu.tpi.misiones.repositorio;

import ar.edu.tpi.misiones.dominio.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    boolean existsByNombreIgnoreCase(String nombre);
}
