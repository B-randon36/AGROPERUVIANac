package pe.edu.upeu.bomerp.compras.service;

import pe.edu.upeu.bomerp.compras.dto.*;
import pe.edu.upeu.bomerp.compras.entity.EstadoCompra;

import java.time.LocalDate;
import java.util.List;

public interface CompraService {
    ProveedorResponse crearProveedor(ProveedorRequest request);
    List<ProveedorResponse> listarProveedores();
    CompraResponse registrarCompra(CompraRequest request);
    CompraResponse amortizarPago(Long compraId, AmortizarPagoRequest request);
    CompraResponse anularCompra(Long id);
    CompraResponse obtener(Long id);
    List<CompraResponse> listarCompras();
    List<CompraResponse> listarCompras(EstadoCompra estado, LocalDate desde, LocalDate hasta);
    CompraReporte reporte(EstadoCompra estado, LocalDate desde, LocalDate hasta);
    List<InsumoDto> listarInsumos();
    List<PagoCompraResponse> listarPagosPorCompra(Long compraId);
}
