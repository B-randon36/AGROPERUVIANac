package pe.edu.upeu.bomerp.ventas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "VENTAS", schema = "BOM_VENTAS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NUMERO_TICKET", nullable = false, unique = true, length = 30)
    private String numeroTicket;

    @Column(name = "FECHA", nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EstadoVenta estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "METODO_PAGO", nullable = false, length = 30)
    private MetodoPagoVenta metodoPago;

    @Column(name = "SUBTOTAL", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "IGV", nullable = false, precision = 12, scale = 2)
    private BigDecimal igv;

    @Column(name = "TOTAL", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "CLIENTE_NOMBRE", length = 120)
    private String clienteNombre;

    @Column(name = "VENDEDOR_ID", length = 36)
    private String vendedorId;

    @Column(name = "SESION_CAJA_ID")
    private Long sesionCajaId;

    @Column(name = "OPERACION_ID", length = 64, unique = true)
    private String operacionId;

    @Builder.Default
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();
}
