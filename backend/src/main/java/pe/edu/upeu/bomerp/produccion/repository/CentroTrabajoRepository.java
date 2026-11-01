package pe.edu.upeu.bomerp.produccion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.bomerp.produccion.entity.CentroTrabajo;

import java.util.List;
import java.util.Optional;

public interface CentroTrabajoRepository extends JpaRepository<CentroTrabajo, Long> {

    Optional<CentroTrabajo> findByCodigoIgnoreCase(String codigo);

    Optional<CentroTrabajo> findByNombreIgnoreCase(String nombre);

    List<CentroTrabajo> findAllByOrderByCodigoAsc();
}

