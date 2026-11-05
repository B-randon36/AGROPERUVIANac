package pe.edu.upeu.bomerp.ventas.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.bomerp.ventas.entity.RegistroAuditoria;
import pe.edu.upeu.bomerp.ventas.repository.RegistroAuditoriaRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final RegistroAuditoriaRepository repository;

    @PostConstruct
    public void inicializarDatosDemo() {
        if (repository.count() == 0) {
            registrar("c1a8d052-7e44-42b7-a36a-f76e3d749911", "CREAR", "VENTA", 101L, "total=400.00 | ticket=TKT-20261105-001");
            registrar("e4b9f123-9c88-43d1-b55a-a12b3c4d5e6f", "ANULAR", "VENTA", 101L, "ticket=TKT-20261105-001 | reposicion_stock=2");
        }
    }

    @Transactional
    public void registrar(String actor, String accion, String entidad, Long entidadId, String detalle) {
        RegistroAuditoria registro = RegistroAuditoria.builder()
                .actor(actor != null && !actor.isBlank() ? actor : "ANONYMOUS")
                .accion(accion)
                .entidad(entidad)
                .entidadId(entidadId)
                .fecha(LocalDateTime.now())
                .detalle(detalle)
                .build();
        repository.save(registro);
    }

    @Transactional(readOnly = true)
    public List<RegistroAuditoria> obtenerPorEntidad(String entidad, Long entidadId) {
        return repository.findByEntidadAndEntidadIdOrderByFechaDesc(entidad, entidadId);
    }

    @Transactional(readOnly = true)
    public List<RegistroAuditoria> listarTodasPorEntidad(String entidad) {
        return repository.findByEntidadOrderByFechaDesc(entidad);
    }
}

