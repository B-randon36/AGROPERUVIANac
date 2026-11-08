package pe.edu.upeu.bomerp.ventas.dto;

import lombok.*;
import pe.edu.upeu.bomerp.ventas.entity.EstadoVenta;
import pe.edu.upeu.bomerp.ventas.entity.MetodoPagoVenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaResponse {
    private Long id;
    private String numeroTicket;
    private LocalDateTime fecha;
    private EstadoVenta estado;
    private MetodoPagoVenta metodoPago;
    private BigDecimal subtotal;
    private BigDecimal igv;
    private BigDecimal total;
    private String clienteNombre;
    private String vendedorId;
    private Long sesionCajaId;
    private String operacionId;
    private List<DetalleVentaResponse> detalles;
}
