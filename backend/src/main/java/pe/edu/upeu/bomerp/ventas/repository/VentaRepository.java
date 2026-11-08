package pe.edu.upeu.bomerp.ventas.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.bomerp.ventas.dto.VentaAgregado;
import pe.edu.upeu.bomerp.ventas.dto.VentaResumen;
import pe.edu.upeu.bomerp.ventas.entity.EstadoVenta;
import pe.edu.upeu.bomerp.ventas.entity.MetodoPagoVenta;
import pe.edu.upeu.bomerp.ventas.entity.Venta;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    @Override
    @EntityGraph(attributePaths = "detalles")
    Optional<Venta> findById(Long id);

    @EntityGraph(attributePaths = "detalles")
    @Query("""
        SELECT v FROM Venta v
        WHERE (:estado IS NULL OR v.estado = :estado)
          AND (:metodoPago IS NULL OR v.metodoPago = :metodoPago)
          AND (:desde IS NULL OR v.fecha >= :desde)
          AND (:hasta IS NULL OR v.fecha <= :hasta)
          AND (:vendedorId IS NULL OR v.vendedorId = :vendedorId)
        """)
    Page<Venta> buscar(@Param("estado") EstadoVenta estado,
                       @Param("metodoPago") MetodoPagoVenta metodoPago,
                       @Param("desde") LocalDateTime desde,
                       @Param("hasta") LocalDateTime hasta,
                       @Param("vendedorId") String vendedorId,
                       Pageable pageable);

    @EntityGraph(attributePaths = "detalles")
    @Query("""
        SELECT v FROM Venta v
        WHERE (:estado IS NULL OR v.estado = :estado)
          AND (:metodoPago IS NULL OR v.metodoPago = :metodoPago)
          AND (:desde IS NULL OR v.fecha >= :desde)
          AND (:hasta IS NULL OR v.fecha <= :hasta)
          AND (:vendedorId IS NULL OR v.vendedorId = :vendedorId)
        """)
    List<Venta> buscar(@Param("estado") EstadoVenta estado,
                       @Param("metodoPago") MetodoPagoVenta metodoPago,
                       @Param("desde") LocalDateTime desde,
                       @Param("hasta") LocalDateTime hasta,
                       @Param("vendedorId") String vendedorId,
                       Sort sort);

    @Query("""
        SELECT new pe.edu.upeu.bomerp.ventas.dto.VentaResumen(
            v.id, v.numeroTicket, v.fecha, v.estado, v.total, SIZE(v.detalles))
        FROM Venta v
        WHERE (:estado IS NULL OR v.estado = :estado)
          AND (:metodoPago IS NULL OR v.metodoPago = :metodoPago)
          AND (:desde IS NULL OR v.fecha >= :desde)
          AND (:hasta IS NULL OR v.fecha <= :hasta)
        """)
    List<VentaResumen> buscarResumen(@Param("estado") EstadoVenta estado,
                                     @Param("metodoPago") MetodoPagoVenta metodoPago,
                                     @Param("desde") LocalDateTime desde,
                                     @Param("hasta") LocalDateTime hasta,
                                     Sort sort);

    @Query("""
        SELECT new pe.edu.upeu.bomerp.ventas.dto.VentaAgregado(
            COUNT(v),
            COALESCE(SUM(CASE WHEN v.estado != pe.edu.upeu.bomerp.ventas.entity.EstadoVenta.ANULADA THEN v.total ELSE 0BD END), 0BD),
            COUNT(CASE WHEN v.estado = pe.edu.upeu.bomerp.ventas.entity.EstadoVenta.ANULADA THEN 1 END),
            COALESCE(SUM(CASE WHEN v.estado = pe.edu.upeu.bomerp.ventas.entity.EstadoVenta.ANULADA THEN v.total ELSE 0BD END), 0BD)
        )
        FROM Venta v
        WHERE (:estado IS NULL OR v.estado = :estado)
          AND (:metodoPago IS NULL OR v.metodoPago = :metodoPago)
          AND (:desde IS NULL OR v.fecha >= :desde)
          AND (:hasta IS NULL OR v.fecha <= :hasta)
        """)
    VentaAgregado agregados(@Param("estado") EstadoVenta estado,
                            @Param("metodoPago") MetodoPagoVenta metodoPago,
                            @Param("desde") LocalDateTime desde,
                            @Param("hasta") LocalDateTime hasta);
}
