package pe.edu.upeu.bomerp.compras.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompraRequest {

    @NotNull(message = "El ID del proveedor es obligatorio")
    private Long proveedorId;

    private String serieComprobante;

    @NotBlank(message = "El número de comprobante es obligatorio")
    private String numeroComprobante;

    @NotNull(message = "La fecha de emisión es obligatoria")
    private LocalDate fechaEmision;

    private String condicionPago;

    private LocalDate fechaVencimientoPago;

    @DecimalMin(value = "0.00", message = "El costo de flete no puede ser negativo")
    private BigDecimal costoFlete;

    @NotEmpty(message = "La compra debe incluir al menos un insumo en el detalle")
    @Valid
    private List<DetalleCompraRequest> detalles;
}
