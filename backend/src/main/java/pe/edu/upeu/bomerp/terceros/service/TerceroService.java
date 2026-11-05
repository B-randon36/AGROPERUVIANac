package pe.edu.upeu.bomerp.terceros.service;

import pe.edu.upeu.bomerp.terceros.dto.TerceroRequest;
import pe.edu.upeu.bomerp.terceros.dto.TerceroResponse;

import java.util.List;

public interface TerceroService {
    List<TerceroResponse> listar(String tipo);
    TerceroResponse obtener(Long id);
    TerceroResponse crear(TerceroRequest request);
    TerceroResponse actualizar(Long id, TerceroRequest request);
    void eliminar(Long id);
}
