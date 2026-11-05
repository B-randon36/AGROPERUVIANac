package pe.edu.upeu.bomerp.ventas.service;

import pe.edu.upeu.bomerp.ventas.dto.ReporteOmnicanalResponse;
import pe.edu.upeu.bomerp.ventas.dto.VentaReporte;
import pe.edu.upeu.bomerp.ventas.dto.VentaRequest;
import pe.edu.upeu.bomerp.ventas.dto.VentaResponse;
import pe.edu.upeu.bomerp.ventas.entity.EstadoVenta;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaService {
    default List<VentaResponse> buscar(EstadoVenta estado, LocalDateTime desde, LocalDateTime hasta, String ordenarPor, String direccion) {
        return buscar(estado, desde, hasta, ordenarPor, direccion, null);
    }
    List<VentaResponse> buscar(EstadoVenta estado, LocalDateTime desde, LocalDateTime hasta, String ordenarPor, String direccion, String vendedorId);

    default VentaResponse obtener(Long id) {
        return obtener(id, null);
    }
    VentaResponse obtener(Long id, String vendedorId);

    default VentaResponse crear(VentaRequest request) {
        return crear(request, null);
    }
    VentaResponse crear(VentaRequest request, String vendedorId);

    VentaResponse anular(Long id);
    VentaReporte reporte(EstadoVenta estado, LocalDateTime desde, LocalDateTime hasta);
    ReporteOmnicanalResponse obtenerReporteOmnicanal();
    void eliminarTodas();
    void eliminar(Long id);
}
