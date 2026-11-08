package pe.edu.upeu.bomerp.inventario.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.bomerp.inventario.entity.EstadoLote;
import pe.edu.upeu.bomerp.inventario.entity.Lote;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Long> {

    Optional<Lote> findByCodigoLote(String codigoLote);

    List<Lote> findByProductoIdOrderByFechaVencimientoAsc(Long productoId);

    List<Lote> findAllByOrderByFechaVencimientoAsc();

    /**
     * Consulta base del algoritmo FEFO:
     * Retorna los lotes activos de un producto que tienen stock disponible (> 0),
     * ordenados estrictamente por fecha de vencimiento ascendente (los más antiguos/próximos a vencer primero).
     */
    @Query("""
        SELECT l FROM Lote l
        WHERE l.productoId = :productoId
          AND l.stockActual > 0
          AND l.estado IN :estados
        ORDER BY l.fechaVencimiento ASC, l.id ASC
    """)
    List<Lote> findLotesParaDespachoFEFO(
            @Param("productoId") Long productoId,
            @Param("estados") Collection<EstadoLote> estados
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT l FROM Lote l
        WHERE l.productoId = :productoId
          AND l.stockActual > 0
          AND l.estado IN :estados
        ORDER BY l.fechaVencimiento ASC, l.id ASC
    """)
    List<Lote> findLotesParaDespachoFEFOForUpdate(
            @Param("productoId") Long productoId,
            @Param("estados") Collection<EstadoLote> estados
    );

    @Query("""
        SELECT COALESCE(SUM(l.stockActual), 0)
        FROM Lote l
        WHERE l.productoId = :productoId
          AND l.estado IN :estados
    """)
    BigDecimal sumarStockDisponiblePorProducto(
            @Param("productoId") Long productoId,
            @Param("estados") Collection<EstadoLote> estados
    );

    List<Lote> findByFechaVencimientoLessThanEqualAndStockActualGreaterThan(LocalDate fecha, BigDecimal minStock);

    long countByProductoId(Long productoId);
}
