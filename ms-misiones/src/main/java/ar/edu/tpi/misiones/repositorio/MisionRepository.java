package ar.edu.tpi.misiones.repositorio;

import ar.edu.tpi.misiones.dominio.EstadoMision;
import ar.edu.tpi.misiones.dominio.Mision;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MisionRepository extends JpaRepository<Mision, Long> {

    List<Mision> findByProductorSubOrderByCreadaEnDesc(String productorSub);

    List<Mision> findByPilotoUsuarioSubOrderByInicioPrevAsc(String pilotoSub);

    /** RN-06: el dron ya tiene una mision que reserva recursos y se solapa con [inicio, fin). */
    @Query("""
            select count(m) > 0 from Mision m
            where m.dron.id = :dronId and m.estado in :estados
              and m.inicioPrev < :fin and m.finPrev > :inicio""")
    boolean dronOcupado(@Param("dronId") Long dronId, @Param("inicio") Instant inicio, @Param("fin") Instant fin,
                        @Param("estados") Collection<EstadoMision> estados);

    /** RN-03: el piloto ya tiene una mision que se solapa con [inicio, fin). */
    @Query("""
            select count(m) > 0 from Mision m
            where m.piloto.id = :pilotoId and m.estado in :estados
              and m.inicioPrev < :fin and m.finPrev > :inicio""")
    boolean pilotoOcupado(@Param("pilotoId") Long pilotoId, @Param("inicio") Instant inicio,
                          @Param("fin") Instant fin, @Param("estados") Collection<EstadoMision> estados);

    /** RN-03: minutos ya programados para el piloto en el dia [desde, hasta). */
    @Query("""
            select coalesce(sum(m.duracionMin), 0) from Mision m
            where m.piloto.id = :pilotoId and m.estado in :estados
              and m.inicioPrev >= :desde and m.inicioPrev < :hasta""")
    long minutosProgramados(@Param("pilotoId") Long pilotoId, @Param("desde") Instant desde,
                            @Param("hasta") Instant hasta, @Param("estados") Collection<EstadoMision> estados);

    /** RN-10: fin de la ultima aplicacion COMPLETADA del producto sobre el lote. */
    @Query("""
            select max(m.finPrev) from Mision m
            where m.loteId = :loteId and m.producto.id = :productoId
              and m.estado = ar.edu.tpi.misiones.dominio.EstadoMision.COMPLETADA""")
    Instant ultimaAplicacionCompletada(@Param("loteId") Long loteId, @Param("productoId") Long productoId);
}
