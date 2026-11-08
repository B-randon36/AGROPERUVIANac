package pe.edu.upeu.bomerp.ventas.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pe.edu.upeu.bomerp.ventas.dto.ReporteOmnicanalResponse;
import pe.edu.upeu.bomerp.ventas.dto.VentaReporte;
import pe.edu.upeu.bomerp.ventas.dto.VentaRequest;
import pe.edu.upeu.bomerp.ventas.dto.VentaResponse;
import pe.edu.upeu.bomerp.ventas.entity.EstadoVenta;
import pe.edu.upeu.bomerp.ventas.entity.MetodoPagoVenta;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaService {
    Page<VentaResponse> buscar(EstadoVenta estado, MetodoPagoVenta metodoPago, LocalDateTime desde, LocalDateTime hasta, String vendedorId, Pageable pageable);

    default Page<VentaResponse> buscar(EstadoVenta estado, LocalDateTime desde, LocalDateTime hasta, String vendedorId, Pageable pageable) {
        return buscar(estado, null, desde, hasta, vendedorId, pageable);
    }

    default List<VentaResponse> buscar(EstadoVenta estado, LocalDateTime desde, LocalDateTime hasta, String ordenarPor, String direccion) {
        return buscar(estado, null, desde, hasta, ordenarPor, direccion, null);
    }
    List<VentaResponse> buscar(EstadoVenta estado, MetodoPagoVenta metodoPago, LocalDateTime desde, LocalDateTime hasta, String ordenarPor, String direccion, String vendedorId);

    default List<VentaResponse> buscar(EstadoVenta estado, LocalDateTime desde, LocalDateTime hasta, String ordenarPor, String direccion, String vendedorId) {
        return buscar(estado, null, desde, hasta, ordenarPor, direccion, vendedorId);
    }

    default VentaResponse obtener(Long id) {
        return obtener(id, null);
    }
    VentaResponse obtener(Long id, String vendedorId);

    default VentaResponse crear(VentaRequest request) {
        return crear(request, null);
    }
    VentaResponse crear(VentaRequest request, String vendedorId);

    default VentaResponse anular(Long id) {
        return anular(id, null);
    }
    VentaResponse anular(Long id, String vendedorId);

    default VentaResponse solicitarAnulacion(Long id, String motivo) {
        return solicitarAnulacion(id, motivo, null);
    }
    default VentaResponse solicitarAnulacion(Long id, String motivo, String vendedorId) {
        return solicitarAnulacion(id, motivo, vendedorId, vendedorId);
    }
    VentaResponse solicitarAnulacion(Long id, String motivo, String actor, String vendedorIdRestringido);

    default VentaResponse rechazarAnulacion(Long id) {
        return rechazarAnulacion(id, null);
    }
    VentaResponse rechazarAnulacion(Long id, String actor);

    VentaReporte reporte(EstadoVenta estado, MetodoPagoVenta metodoPago, LocalDateTime desde, LocalDateTime hasta, String vendedorId);

    default VentaReporte reporte(EstadoVenta estado, MetodoPagoVenta metodoPago, LocalDateTime desde, LocalDateTime hasta) {
        return reporte(estado, metodoPago, desde, hasta, null);
    }

    default VentaReporte reporte(EstadoVenta estado, LocalDateTime desde, LocalDateTime hasta) {
        return reporte(estado, null, desde, hasta);
    }

    ReporteOmnicanalResponse obtenerReporteOmnicanal();
}
