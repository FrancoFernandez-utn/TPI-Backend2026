package ar.edu.tpi.misiones.repositorio;

import ar.edu.tpi.misiones.dominio.TipoMision;
import ar.edu.tpi.misiones.dominio.UmbralClima;
import ar.edu.tpi.misiones.dominio.UmbralClimaId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UmbralClimaRepository extends JpaRepository<UmbralClima, UmbralClimaId> {

    @Query("select u from UmbralClima u where u.id.tipoMision = :tipo order by u.id.variable")
    List<UmbralClima> buscarPorTipo(@Param("tipo") TipoMision tipo);

    @Modifying
    @Query("delete from UmbralClima u where u.id.tipoMision = :tipo")
    void borrarPorTipo(@Param("tipo") TipoMision tipo);
}
