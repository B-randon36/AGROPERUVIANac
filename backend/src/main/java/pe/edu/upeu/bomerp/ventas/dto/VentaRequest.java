package pe.edu.upeu.bomerp.ventas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import pe.edu.upeu.bomerp.ventas.entity.MetodoPagoVenta;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaRequest {

    @NotNull(message = "El método de pago es obligatorio")
    private MetodoPagoVenta metodoPago;

    private String clienteNombre;

    private String clienteDocumento;

    private Integer diasCredito;

    private Long sesionCajaId;

    private String operacionId;

    @NotEmpty(message = "La venta debe contener al menos un detalle de producto")
    @Valid
    private List<DetalleVentaRequest> detalles;
}
