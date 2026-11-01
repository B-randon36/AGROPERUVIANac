package pe.edu.upeu.bomerp.produccion.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.bomerp.produccion.entity.ListaMateriales;

import java.util.List;
import java.util.Optional;

public interface ListaMaterialesRepository extends JpaRepository<ListaMateriales, Long> {

    @EntityGraph(attributePaths = "detalles")
    Optional<ListaMateriales> findByCodigoIgnoreCase(String codigo);

    @EntityGraph(attributePaths = "detalles")
    Optional<ListaMateriales> findFirstByProductoIdAndActivoTrueOrderByIdDesc(Long productoId);

    @EntityGraph(attributePaths = "detalles")
    List<ListaMateriales> findByProductoIdOrderByIdDesc(Long productoId);

    @EntityGraph(attributePaths = "detalles")
    List<ListaMateriales> findAllByOrderByCodigoAsc();
}

