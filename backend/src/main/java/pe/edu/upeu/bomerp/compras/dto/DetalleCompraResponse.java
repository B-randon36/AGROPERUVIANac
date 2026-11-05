package pe.edu.upeu.bomerp.compras.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleCompraResponse {
    private Long id;
    private Long insumoId;
    private String nombreInsumo;
    private String loteProveedor;
    private LocalDate fechaVencimiento;
    private BigDecimal factorConversion;
    private BigDecimal cantidad;
    private BigDecimal cantidadIngreso;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}
