package pe.edu.upeu.bomerp.inventario.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.bomerp.exception.BusinessConflictException;
import pe.edu.upeu.bomerp.exception.ResourceNotFoundException;
import pe.edu.upeu.bomerp.inventario.dto.AlmacenRequest;
import pe.edu.upeu.bomerp.inventario.dto.AlmacenResponse;
import pe.edu.upeu.bomerp.inventario.entity.Almacen;
import pe.edu.upeu.bomerp.inventario.repository.AlmacenRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AlmacenService {

    private final AlmacenRepository almacenRepository;

    @Transactional(readOnly = true)
    public List<AlmacenResponse> listar() {
        return almacenRepository.findAllByOrderByCodigoAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AlmacenResponse obtener(Long id) {
        Almacen entity = almacenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Almacén no encontrado con ID: " + id));
        return toResponse(entity);
    }

    public AlmacenResponse crear(AlmacenRequest request) {
        String codigo = request.getCodigo().trim().toUpperCase();
        String nombre = request.getNombre().trim();

        if (almacenRepository.findByCodigoIgnoreCase(codigo).isPresent()) {
            throw new BusinessConflictException("Ya existe un almacén con el código: " + codigo);
        }
        if (almacenRepository.findByNombreIgnoreCase(nombre).isPresent()) {
            throw new BusinessConflictException("Ya existe un almacén con el nombre: " + nombre);
        }

        BigDecimal ocupacion = request.getOcupacionActual() != null ? request.getOcupacionActual() : BigDecimal.ZERO;
        if (ocupacion.compareTo(request.getCapacidadMaxima()) > 0) {
            throw new BusinessConflictException("La ocupación actual no puede exceder la capacidad máxima del almacén.");
        }

        Almacen entity = Almacen.builder()
                .codigo(codigo)
                .nombre(nombre)
                .ubicacion(request.getUbicacion())
                .tipo(request.getTipo() != null && !request.getTipo().isBlank()
                        ? request.getTipo().trim().toUpperCase()
                        : "PRODUCTO_TERMINADO")
                .capacidadMaxima(request.getCapacidadMaxima())
                .ocupacionActual(ocupacion)
                .responsable(request.getResponsable())
                .activo(request.getActivo() != null ? request.getActivo() : true)
                .build();

        return toResponse(almacenRepository.save(entity));
    }

    public AlmacenResponse actualizar(Long id, AlmacenRequest request) {
        Almacen entity = almacenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Almacén no encontrado con ID: " + id));

        String codigo = request.getCodigo().trim().toUpperCase();
        String nombre = request.getNombre().trim();

        almacenRepository.findByCodigoIgnoreCase(codigo)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessConflictException("Ya existe otro almacén con el código: " + codigo);
                });

        almacenRepository.findByNombreIgnoreCase(nombre)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessConflictException("Ya existe otro almacén con el nombre: " + nombre);
                });

        BigDecimal ocupacion = request.getOcupacionActual() != null ? request.getOcupacionActual() : entity.getOcupacionActual();
        if (ocupacion.compareTo(request.getCapacidadMaxima()) > 0) {
            throw new BusinessConflictException("La ocupación actual no puede exceder la capacidad máxima del almacén.");
        }

        entity.setCodigo(codigo);
        entity.setNombre(nombre);
        entity.setUbicacion(request.getUbicacion());
        if (request.getTipo() != null && !request.getTipo().isBlank()) {
            entity.setTipo(request.getTipo().trim().toUpperCase());
        }
        entity.setCapacidadMaxima(request.getCapacidadMaxima());
        entity.setOcupacionActual(ocupacion);
        entity.setResponsable(request.getResponsable());
        if (request.getActivo() != null) {
            entity.setActivo(request.getActivo());
        }

        return toResponse(almacenRepository.save(entity));
    }

    public void eliminar(Long id) {
        Almacen entity = almacenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Almacén no encontrado con ID: " + id));
        almacenRepository.delete(entity);
    }

    private AlmacenResponse toResponse(Almacen a) {
        double pct = 0.0;
        if (a.getCapacidadMaxima() != null && a.getCapacidadMaxima().compareTo(BigDecimal.ZERO) > 0 && a.getOcupacionActual() != null) {
            pct = (a.getOcupacionActual().doubleValue() * 100.0) / a.getCapacidadMaxima().doubleValue();
            pct = Math.round(pct * 100.0) / 100.0;
        }
        String nivel = pct >= 90.0 ? "CRITICO" : (pct >= 70.0 ? "ALTO" : "OPTIMO");

        return AlmacenResponse.builder()
                .id(a.getId())
                .codigo(a.getCodigo())
                .nombre(a.getNombre())
                .ubicacion(a.getUbicacion())
                .tipo(a.getTipo())
                .capacidadMaxima(a.getCapacidadMaxima())
                .ocupacionActual(a.getOcupacionActual())
                .porcentajeOcupacion(pct)
                .nivelOcupacion(nivel)
                .responsable(a.getResponsable())
                .activo(a.getActivo())
                .version(a.getVersion())
                .build();
    }
}

