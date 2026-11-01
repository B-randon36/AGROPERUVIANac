package pe.edu.upeu.bomerp.ventas.dto;

import lombok.*;
import pe.edu.upeu.bomerp.ventas.entity.EstadoDelivery;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDeliveryResponse {
    private Long id;
    private String codigoPedido;
    private Long ventaId;
    private Long clienteId;
    private String clienteNombre;
    private String clienteTelefono;
    private String direccionEntrega;
    private String referencia;
    private BigDecimal costoEnvio;
    private BigDecimal montoTotal;
    private String repartidorNombre;
    private EstadoDelivery estadoDelivery;
    private LocalDateTime fechaPactada;
    private LocalDateTime fechaEntrega;
    private String observaciones;
}

