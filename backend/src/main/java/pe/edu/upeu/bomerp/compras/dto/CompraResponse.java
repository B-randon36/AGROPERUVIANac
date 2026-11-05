package pe.edu.upeu.bomerp.compras.dto;

import lombok.*;
import pe.edu.upeu.bomerp.compras.entity.EstadoCompra;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompraResponse {
    private Long id;
    private String serieComprobante;
    private String numeroComprobante;
    private ProveedorResponse proveedor;
    private Long proveedorId;
    private LocalDate fechaEmision;
    private String condicionPago;
    private LocalDate fechaVencimientoPago;
    private EstadoCompra estado;
    private BigDecimal subtotal;
    private BigDecimal igv;
    private BigDecimal costoFlete;
    private BigDecimal total;
    private BigDecimal saldoPendiente;
    private List<DetalleCompraResponse> detalles;
    private List<PagoCompraResponse> pagos;
}
