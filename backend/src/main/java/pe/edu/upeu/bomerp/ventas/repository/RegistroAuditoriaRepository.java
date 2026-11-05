package pe.edu.upeu.bomerp.ventas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.bomerp.ventas.entity.RegistroAuditoria;

import java.util.List;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {
    List<RegistroAuditoria> findByEntidadAndEntidadIdOrderByFechaDesc(String entidad, Long entidadId);
    List<RegistroAuditoria> findByEntidadOrderByFechaDesc(String entidad);
}

