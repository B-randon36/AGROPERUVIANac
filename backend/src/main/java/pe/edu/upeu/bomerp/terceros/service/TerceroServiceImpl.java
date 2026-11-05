package pe.edu.upeu.bomerp.terceros.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.bomerp.compras.dto.ProveedorRequest;
import pe.edu.upeu.bomerp.compras.service.CompraService;
import pe.edu.upeu.bomerp.exception.BusinessConflictException;
import pe.edu.upeu.bomerp.exception.ResourceNotFoundException;
import pe.edu.upeu.bomerp.terceros.dto.TerceroRequest;
import pe.edu.upeu.bomerp.terceros.dto.TerceroResponse;
import pe.edu.upeu.bomerp.terceros.entity.Tercero;
import pe.edu.upeu.bomerp.terceros.repository.TerceroRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TerceroServiceImpl implements TerceroService {

    private final TerceroRepository terceroRepository;
    private final CompraService compraService;

    @Override
    @Transactional(readOnly = true)
    public List<TerceroResponse> listar(String tipo) {
        List<Tercero> lista;
        if (tipo != null && !tipo.isBlank() && !tipo.equalsIgnoreCase("TODOS")) {
            lista = terceroRepository.findByTipoTerceroAndActivoTrueOrderByIdDesc(tipo.trim().toUpperCase());
        } else {
            lista = terceroRepository.findByActivoTrueOrderByIdDesc();
        }
        return lista.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TerceroResponse obtener(Long id) {
        Tercero t = terceroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tercero comercial no encontrado con ID: " + id));
        return toResponse(t);
    }

    @Override
    @Transactional
    public TerceroResponse crear(TerceroRequest req) {
        terceroRepository.findByNumeroDocumento(req.getNumeroDocumento().trim()).ifPresent(t -> {
            throw new BusinessConflictException("Ya existe un tercero registrado con el documento: " + req.getNumeroDocumento());
        });

        String tipoNorm = req.getTipoTercero() != null ? req.getTipoTercero().trim().toUpperCase() : "CLIENTE";

        Tercero tercero = Tercero.builder()
                .tipoDocumento(req.getTipoDocumento().trim().toUpperCase())
                .numeroDocumento(req.getNumeroDocumento().trim())
                .razonSocial(req.getRazonSocial().trim())
                .tipoTercero(tipoNorm)
                .telefono(req.getTelefono() != null ? req.getTelefono().trim() : null)
                .direccion(req.getDireccion() != null ? req.getDireccion().trim() : null)
                .correo(req.getCorreo() != null ? req.getCorreo().trim() : null)
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build();

        Tercero guardado = terceroRepository.save(tercero);

        // Sincronización automática con Proveedores de Compras si es PROVEEDOR o AMBOS
        if ("PROVEEDOR".equalsIgnoreCase(tipoNorm) || "AMBOS".equalsIgnoreCase(tipoNorm)) {
            syncProveedor(guardado);
        }

        return toResponse(guardado);
    }

    @Override
    @Transactional
    public TerceroResponse actualizar(Long id, TerceroRequest req) {
        Tercero tercero = terceroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tercero comercial no encontrado con ID: " + id));

        terceroRepository.findByNumeroDocumento(req.getNumeroDocumento().trim()).ifPresent(otro -> {
            if (!otro.getId().equals(id)) {
                throw new BusinessConflictException("El número de documento " + req.getNumeroDocumento() + " ya pertenece a otro tercero.");
            }
        });

        String tipoNorm = req.getTipoTercero() != null ? req.getTipoTercero().trim().toUpperCase() : "CLIENTE";

        tercero.setTipoDocumento(req.getTipoDocumento().trim().toUpperCase());
        tercero.setNumeroDocumento(req.getNumeroDocumento().trim());
        tercero.setRazonSocial(req.getRazonSocial().trim());
        tercero.setTipoTercero(tipoNorm);
        tercero.setTelefono(req.getTelefono() != null ? req.getTelefono().trim() : null);
        tercero.setDireccion(req.getDireccion() != null ? req.getDireccion().trim() : null);
        tercero.setCorreo(req.getCorreo() != null ? req.getCorreo().trim() : null);

        Tercero guardado = terceroRepository.save(tercero);

        if ("PROVEEDOR".equalsIgnoreCase(tipoNorm) || "AMBOS".equalsIgnoreCase(tipoNorm)) {
            syncProveedor(guardado);
        }

        return toResponse(guardado);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Tercero t = terceroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tercero comercial no encontrado con ID: " + id));
        t.setActivo(false);
        terceroRepository.save(t);
    }

    private void syncProveedor(Tercero t) {
        if (compraService != null && t.getNumeroDocumento() != null && t.getNumeroDocumento().trim().length() == 11) {
            try {
                ProveedorRequest p = ProveedorRequest.builder()
                        .ruc(t.getNumeroDocumento().trim())
                        .razonSocial(t.getRazonSocial())
                        .telefono(t.getTelefono())
                        .email(t.getCorreo())
                        .direccion(t.getDireccion())
                        .build();
                compraService.crearProveedor(p);
                log.info("Sincronizado proveedor {} en compras desde CRM Terceros.", t.getRazonSocial());
            } catch (Exception e) {
                log.warn("No se pudo sincronizar proveedor en compras: {}", e.getMessage());
            }
        }
    }

    private TerceroResponse toResponse(Tercero t) {
        return TerceroResponse.builder()
                .id(t.getId())
                .tipoDocumento(t.getTipoDocumento())
                .numeroDocumento(t.getNumeroDocumento())
                .razonSocial(t.getRazonSocial())
                .tipoTercero(t.getTipoTercero())
                .telefono(t.getTelefono())
                .direccion(t.getDireccion())
                .correo(t.getCorreo())
                .activo(t.getActivo())
                .build();
    }
}
