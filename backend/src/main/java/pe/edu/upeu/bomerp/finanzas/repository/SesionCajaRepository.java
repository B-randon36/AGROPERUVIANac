package pe.edu.upeu.bomerp.finanzas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.bomerp.finanzas.entity.EstadoCaja;
import pe.edu.upeu.bomerp.finanzas.entity.SesionCaja;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SesionCajaRepository extends JpaRepository<SesionCaja, Long> {

    Optional<SesionCaja> findByCodigoSesion(String codigoSesion);

    boolean existsByCodigoSesion(String codigoSesion);

    Optional<SesionCaja> findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(Long usuarioId, EstadoCaja estado);

    List<SesionCaja> findByCajeroAndEstadoOrderByFechaAperturaDesc(String cajero, EstadoCaja estado);

    Optional<SesionCaja> findFirstByEstadoOrderByFechaAperturaDesc(EstadoCaja estado);

    boolean existsByUsuarioIdAndEstado(Long usuarioId, EstadoCaja estado);

    List<SesionCaja> findAllByOrderByFechaAperturaDesc();

    @Query("SELECT s FROM SesionCaja s WHERE s.fechaApertura >= :inicio AND s.fechaApertura <= :fin ORDER BY s.fechaApertura DESC")
    List<SesionCaja> findByRangoFechas(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
