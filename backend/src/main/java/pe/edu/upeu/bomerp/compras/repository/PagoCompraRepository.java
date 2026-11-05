package pe.edu.upeu.bomerp.compras.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.bomerp.compras.entity.PagoCompra;

import java.util.List;

@Repository
public interface PagoCompraRepository extends JpaRepository<PagoCompra, Long> {
    List<PagoCompra> findByCompraIdOrderByFechaPagoAsc(Long compraId);
}
