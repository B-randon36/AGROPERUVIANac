package pe.edu.upeu.bomerp.finanzas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.bomerp.finanzas.entity.EstadoFinanciero;
import pe.edu.upeu.bomerp.finanzas.entity.TipoFinanciero;
import pe.edu.upeu.bomerp.finanzas.entity.TransaccionFinanciera;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransaccionFinancieraRepository extends JpaRepository<TransaccionFinanciera, Long> {
    Optional<TransaccionFinanciera> findByCodigo(String codigo);
    Optional<TransaccionFinanciera> findFirstByNumeroComprobante(String numeroComprobante);
    boolean existsByCodigo(String codigo);
    boolean existsByCodigoAndIdNot(String codigo, Long id);
    List<TransaccionFinanciera> findByTipo(TipoFinanciero tipo);
    List<TransaccionFinanciera> findByEstado(EstadoFinanciero estado);
    List<TransaccionFinanciera> findByCategoriaFinancieraId(Long categoriaFinancieraId);
    List<TransaccionFinanciera> findByTipoAndEstado(TipoFinanciero tipo, EstadoFinanciero estado);
}
