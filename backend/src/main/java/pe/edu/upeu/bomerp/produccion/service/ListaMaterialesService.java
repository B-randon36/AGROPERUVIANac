package pe.edu.upeu.bomerp.produccion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.bomerp.exception.BusinessConflictException;
import pe.edu.upeu.bomerp.exception.ResourceNotFoundException;
import pe.edu.upeu.bomerp.produccion.dto.DetalleListaMaterialesRequest;
import pe.edu.upeu.bomerp.produccion.dto.DetalleListaMaterialesResponse;
import pe.edu.upeu.bomerp.produccion.dto.ListaMaterialesRequest;
import pe.edu.upeu.bomerp.produccion.dto.ListaMaterialesResponse;
import pe.edu.upeu.bomerp.produccion.entity.DetalleListaMateriales;
import pe.edu.upeu.bomerp.produccion.entity.ListaMateriales;
import pe.edu.upeu.bomerp.produccion.repository.ListaMaterialesRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ListaMaterialesService {

    private final ListaMaterialesRepository listaMaterialesRepository;

    @Transactional(readOnly = true)
    public List<ListaMaterialesResponse> listar() {
        return listaMaterialesRepository.findAllByOrderByCodigoAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ListaMaterialesResponse obtener(Long id) {
        ListaMateriales entity = listaMaterialesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lista de materiales (BOM) no encontrada con ID: " + id));
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public ListaMaterialesResponse obtenerActivaPorProducto(Long productoId) {
        ListaMateriales entity = listaMaterialesRepository.findFirstByProductoIdAndActivoTrueOrderByIdDesc(productoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una receta maestra BOM activa registrada para el producto ID: " + productoId));
        return toResponse(entity);
    }

    public ListaMaterialesResponse crear(ListaMaterialesRequest request) {
        String codigo = request.getCodigo().trim().toUpperCase();
        if (listaMaterialesRepository.findByCodigoIgnoreCase(codigo).isPresent()) {
            throw new BusinessConflictException("Ya existe una receta BOM con el código: " + codigo);
        }

        ListaMateriales entity = ListaMateriales.builder()
                .codigo(codigo)
                .productoId(request.getProductoId())
                .nombreProducto(request.getNombreProducto().trim())
                .cantidadBase(request.getCantidadBase())
                .observaciones(request.getObservaciones())
                .activo(request.getActivo() != null ? request.getActivo() : true)
                .detalles(new ArrayList<>())
                .build();

        for (DetalleListaMaterialesRequest d : request.getDetalles()) {
            entity.getDetalles().add(DetalleListaMateriales.builder()
                    .listaMateriales(entity)
                    .insumoId(d.getInsumoId())
                    .nombreInsumo(d.getNombreInsumo().trim())
                    .cantidadRequerida(d.getCantidadRequerida())
                    .unidadMedida(d.getUnidadMedida().trim().toUpperCase())
                    .build());
        }

        return toResponse(listaMaterialesRepository.save(entity));
    }

    public ListaMaterialesResponse actualizar(Long id, ListaMaterialesRequest request) {
        ListaMateriales entity = listaMaterialesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lista de materiales (BOM) no encontrada con ID: " + id));

        String codigo = request.getCodigo().trim().toUpperCase();
        listaMaterialesRepository.findByCodigoIgnoreCase(codigo)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessConflictException("Ya existe otra receta BOM con el código: " + codigo);
                });

        entity.setCodigo(codigo);
        entity.setProductoId(request.getProductoId());
        entity.setNombreProducto(request.getNombreProducto().trim());
        entity.setCantidadBase(request.getCantidadBase());
        entity.setObservaciones(request.getObservaciones());
        if (request.getActivo() != null) {
            entity.setActivo(request.getActivo());
        }

        entity.getDetalles().clear();
        for (DetalleListaMaterialesRequest d : request.getDetalles()) {
            entity.getDetalles().add(DetalleListaMateriales.builder()
                    .listaMateriales(entity)
                    .insumoId(d.getInsumoId())
                    .nombreInsumo(d.getNombreInsumo().trim())
                    .cantidadRequerida(d.getCantidadRequerida())
                    .unidadMedida(d.getUnidadMedida().trim().toUpperCase())
                    .build());
        }

        return toResponse(listaMaterialesRepository.save(entity));
    }

    public void eliminar(Long id) {
        ListaMateriales entity = listaMaterialesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lista de materiales (BOM) no encontrada con ID: " + id));
        listaMaterialesRepository.delete(entity);
    }

    private ListaMaterialesResponse toResponse(ListaMateriales b) {
        List<DetalleListaMaterialesResponse> detalles = b.getDetalles() != null
                ? b.getDetalles().stream().map(d -> DetalleListaMaterialesResponse.builder()
                .id(d.getId())
                .insumoId(d.getInsumoId())
                .nombreInsumo(d.getNombreInsumo())
                .cantidadRequerida(d.getCantidadRequerida())
                .unidadMedida(d.getUnidadMedida())
                .build()).toList()
                : List.of();

        return ListaMaterialesResponse.builder()
                .id(b.getId())
                .codigo(b.getCodigo())
                .productoId(b.getProductoId())
                .nombreProducto(b.getNombreProducto())
                .cantidadBase(b.getCantidadBase())
                .observaciones(b.getObservaciones())
                .activo(b.getActivo())
                .version(b.getVersion())
                .detalles(detalles)
                .build();
    }
}

