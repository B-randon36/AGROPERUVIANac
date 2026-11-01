package pe.edu.upeu.bomerp.inventario.service;

import pe.edu.upeu.bomerp.inventario.dto.DescuentoFefoRequest;
import pe.edu.upeu.bomerp.inventario.dto.DescuentoFefoResponse;
import pe.edu.upeu.bomerp.inventario.dto.LoteCreateDto;
import pe.edu.upeu.bomerp.inventario.dto.LoteDto;
import pe.edu.upeu.bomerp.inventario.dto.MovimientoKardexDto;
import pe.edu.upeu.bomerp.inventario.dto.ReporteSemaforoInventarioResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface InventarioService {

    LoteDto registrarLote(LoteCreateDto dto);

    /**
     * Regla FEFO (First Expired, First Out):
     * Descuenta stock de un producto consumiendo automáticamente primero los lotes
     * cuya fecha de caducidad esté más próxima.
     */
    DescuentoFefoResponse descontarStockFEFO(DescuentoFefoRequest request);

    /**
     * Integración POS -> Inventario FEFO y Kardex:
     * Consume lotes vigentes bajo regla FEFO (si existen para el producto) y registra la SALIDA en Kardex.
     * Retorna el ID del lote principal consumido para trazabilidad en el detalle de venta.
     */
    Long registrarSalidaVentaFEFO(Long productoId, String productoNombre, int cantidad, String numeroTicket);

    /**
     * Reversión de salida en Lote y Kardex al anular una venta POS.
     */
    void revertirSalidaVenta(Long productoId, String productoNombre, Long loteId, int cantidad, String numeroTicket);

    /**
     * Integración Producción BOM -> Inventario FEFO y Kardex:
     * Registra el lote terminado producido en planta en la tabla unificada de lotes FEFO y asienta ENTRADA en Kardex.
     */
    LoteDto registrarIngresoProduccion(
            Long productoId,
            String productoNombre,
            String codigoLote,
            int cantidadProducida,
            LocalDate fechaVencimiento,
            String codigoOrden
    );

    /**
     * Integración Producción BOM -> Kardex (RN-PROD-01):
     * Descuenta stock de lotes de insumos (si existen) y asienta la SALIDA en Kardex por consumo de receta BOM.
     */
    void registrarConsumoInsumoBom(
            Long insumoId,
            String nombreInsumo,
            BigDecimal cantidadConsumida,
            String unidadMedida,
            String codigoOrden
    );

    /**
     * Integración Compras -> Inventario y Kardex (RN-COM-01):
     * Ingresa el lote de materia prima o envase adquirido y asienta la ENTRADA en Kardex.
     */
    LoteDto registrarIngresoCompraInsumo(
            Long insumoId,
            String nombreInsumo,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            String numeroComprobante
    );

    List<LoteDto> listarLotes(Long productoId);

    List<LoteDto> listarLotesProximosAVencer(int diasUmbral);

    List<MovimientoKardexDto> listarKardex(Long productoId);

    void evaluarEstadosVencimiento();

    ReporteSemaforoInventarioResponse obtenerReporteSemaforoInventario();
}
