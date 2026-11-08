package pe.edu.upeu.bomerp.inventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.bomerp.inventario.entity.MovimientoKardex;

import java.util.List;

@Repository
public interface MovimientoKardexRepository extends JpaRepository<MovimientoKardex, Long> {

    List<MovimientoKardex> findByProductoIdOrderByFechaHoraDesc(Long productoId);

    List<MovimientoKardex> findTop100ByOrderByFechaHoraDesc();

    List<MovimientoKardex> findByLoteIdOrderByFechaHoraDesc(Long loteId);

    List<MovimientoKardex> findByDocumentoReferenciaAndTipo(String documentoReferencia, pe.edu.upeu.bomerp.inventario.entity.TipoMovimientoKardex tipo);

    boolean existsByDocumentoReferenciaAndMotivo(String documentoReferencia, String motivo);
}
