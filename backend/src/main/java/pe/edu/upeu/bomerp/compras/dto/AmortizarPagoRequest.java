package pe.edu.upeu.bomerp.compras.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmortizarPagoRequest {

    @NotNull(message = "El monto a pagar/amortizar es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto a amortizar debe ser mayor a 0")
    private BigDecimal montoPago;

    private String metodoPago;
    private String referencia;
}
