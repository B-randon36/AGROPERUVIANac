package pe.edu.upeu.bomerp.ventas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearDeliveryRequest {

    private String codigoPedido;
    private Long ventaId;
    private Long clienteId;

    @NotBlank(message = "El nombre del cliente es obligatorio")
    private String clienteNombre;

    private String clienteTelefono;

    @NotBlank(message = "La dirección de entrega es obligatoria")
    private String direccionEntrega;

    private String referencia;

    @DecimalMin(value = "0.00", message = "El costo de envío no puede ser negativo")
    private BigDecimal costoEnvio;

    @DecimalMin(value = "0.00", message = "El monto total no puede ser negativo")
    private BigDecimal montoTotal;

    private String repartidorNombre;
    private Double latitud;
    private Double longitud;
    private LocalDateTime fechaPactada;
    private String observaciones;
}

