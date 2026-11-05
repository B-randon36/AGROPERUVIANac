package pe.edu.upeu.bomerp.compras.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleCompraRequest {

    @NotNull(message = "El ID del insumo es obligatorio")
    private Long insumoId;

    @NotBlank(message = "El nombre del insumo es obligatorio")
    private String nombreInsumo;

    private String loteProveedor;

    private LocalDate fechaVencimiento;

    @DecimalMin(value = "0.01", message = "El factor de conversión debe ser mayor a 0")
    private BigDecimal factorConversion;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor a 0")
    private BigDecimal cantidad;

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio unitario debe ser mayor a 0")
    private BigDecimal precioUnitario;
}
