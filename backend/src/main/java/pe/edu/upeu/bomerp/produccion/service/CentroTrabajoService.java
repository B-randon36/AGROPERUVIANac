package pe.edu.upeu.bomerp.produccion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.bomerp.exception.BusinessConflictException;
import pe.edu.upeu.bomerp.exception.ResourceNotFoundException;
import pe.edu.upeu.bomerp.produccion.dto.CentroTrabajoRequest;
import pe.edu.upeu.bomerp.produccion.dto.CentroTrabajoResponse;
import pe.edu.upeu.bomerp.produccion.entity.CentroTrabajo;
import pe.edu.upeu.bomerp.produccion.repository.CentroTrabajoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CentroTrabajoService {

    private final CentroTrabajoRepository centroTrabajoRepository;

    @Transactional(readOnly = true)
    public List<CentroTrabajoResponse> listar() {
        return centroTrabajoRepository.findAllByOrderByCodigoAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CentroTrabajoResponse obtener(Long id) {
        CentroTrabajo entity = centroTrabajoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Centro de trabajo no encontrado con ID: " + id));
        return toResponse(entity);
    }

    public CentroTrabajoResponse crear(CentroTrabajoRequest request) {
        String codigo = request.getCodigo().trim().toUpperCase();
        String nombre = request.getNombre().trim();

        if (centroTrabajoRepository.findByCodigoIgnoreCase(codigo).isPresent()) {
            throw new BusinessConflictException("Ya existe un centro de trabajo con el código: " + codigo);
        }
        if (centroTrabajoRepository.findByNombreIgnoreCase(nombre).isPresent()) {
            throw new BusinessConflictException("Ya existe un centro de trabajo con el nombre: " + nombre);
        }

        CentroTrabajo entity = CentroTrabajo.builder()
                .codigo(codigo)
                .nombre(nombre)
                .descripcion(request.getDescripcion())
                .capacidad(request.getCapacidad())
                .unidadCapacidad(request.getUnidadCapacidad() != null && !request.getUnidadCapacidad().isBlank()
                        ? request.getUnidadCapacidad().trim().toUpperCase()
                        : "UNID/HORA")
                .costoHora(request.getCostoHora())
                .activo(request.getActivo() != null ? request.getActivo() : true)
                .build();

        return toResponse(centroTrabajoRepository.save(entity));
    }

    public CentroTrabajoResponse actualizar(Long id, CentroTrabajoRequest request) {
        CentroTrabajo entity = centroTrabajoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Centro de trabajo no encontrado con ID: " + id));

        String codigo = request.getCodigo().trim().toUpperCase();
        String nombre = request.getNombre().trim();

        centroTrabajoRepository.findByCodigoIgnoreCase(codigo)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessConflictException("Ya existe otro centro de trabajo con el código: " + codigo);
                });

        centroTrabajoRepository.findByNombreIgnoreCase(nombre)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessConflictException("Ya existe otro centro de trabajo con el nombre: " + nombre);
                });

        entity.setCodigo(codigo);
        entity.setNombre(nombre);
        entity.setDescripcion(request.getDescripcion());
        entity.setCapacidad(request.getCapacidad());
        if (request.getUnidadCapacidad() != null && !request.getUnidadCapacidad().isBlank()) {
            entity.setUnidadCapacidad(request.getUnidadCapacidad().trim().toUpperCase());
        }
        entity.setCostoHora(request.getCostoHora());
        if (request.getActivo() != null) {
            entity.setActivo(request.getActivo());
        }

        return toResponse(centroTrabajoRepository.save(entity));
    }

    public void eliminar(Long id) {
        CentroTrabajo entity = centroTrabajoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Centro de trabajo no encontrado con ID: " + id));
        centroTrabajoRepository.delete(entity);
    }

    private CentroTrabajoResponse toResponse(CentroTrabajo c) {
        return CentroTrabajoResponse.builder()
                .id(c.getId())
                .codigo(c.getCodigo())
                .nombre(c.getNombre())
                .descripcion(c.getDescripcion())
                .capacidad(c.getCapacidad())
                .unidadCapacidad(c.getUnidadCapacidad())
                .costoHora(c.getCostoHora())
                .activo(c.getActivo())
                .version(c.getVersion())
                .build();
    }
}

