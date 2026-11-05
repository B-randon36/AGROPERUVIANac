package pe.edu.upeu.bomerp.ventas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PEDIDOS_DELIVERY", schema = "BOM_VENTAS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODIGO_PEDIDO", nullable = false, unique = true, length = 40)
    private String codigoPedido;

    @Column(name = "VENTA_ID")
    private Long ventaId;

    @Column(name = "CLIENTE_ID")
    private Long clienteId;

    @Column(name = "CLIENTE_NOMBRE", nullable = false, length = 150)
    private String clienteNombre;

    @Column(name = "CLIENTE_TELEFONO", length = 30)
    private String clienteTelefono;

    @Column(name = "DIRECCION_ENTREGA", nullable = false, length = 250)
    private String direccionEntrega;

    @Column(name = "REFERENCIA", length = 200)
    private String referencia;

    @Column(name = "COSTO_ENVIO", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoEnvio;

    @Column(name = "MONTO_TOTAL", precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "REPARTIDOR_NOMBRE", length = 120)
    private String repartidorNombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO_DELIVERY", nullable = false, length = 20)
    private EstadoDelivery estadoDelivery;

    @Column(name = "FECHA_PACTADA")
    private LocalDateTime fechaPactada;

    @Column(name = "FECHA_ENTREGA")
    private LocalDateTime fechaEntrega;

    @Column(name = "LATITUD")
    private Double latitud;

    @Column(name = "LONGITUD")
    private Double longitud;

    @Column(name = "OBSERVACIONES", length = 300)
    private String observaciones;
}

