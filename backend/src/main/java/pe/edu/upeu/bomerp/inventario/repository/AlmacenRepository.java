package pe.edu.upeu.bomerp.inventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.bomerp.inventario.entity.Almacen;

import java.util.List;
import java.util.Optional;

public interface AlmacenRepository extends JpaRepository<Almacen, Long> {

    Optional<Almacen> findByCodigoIgnoreCase(String codigo);

    Optional<Almacen> findByNombreIgnoreCase(String nombre);

    List<Almacen> findAllByOrderByCodigoAsc();
}

