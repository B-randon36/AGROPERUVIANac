package pe.edu.upeu.bomerp.terceros.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.bomerp.terceros.entity.Tercero;

import java.util.List;
import java.util.Optional;

@Repository
public interface TerceroRepository extends JpaRepository<Tercero, Long> {
    List<Tercero> findByActivoTrueOrderByIdDesc();
    List<Tercero> findByTipoTerceroAndActivoTrueOrderByIdDesc(String tipoTercero);
    Optional<Tercero> findByNumeroDocumento(String numeroDocumento);
}
