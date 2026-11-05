package pe.edu.upeu.bomerp.compras.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "COMPRAS", schema = "BOM_COMPRAS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "SERIE_COMPROBANTE", length = 10)
    private String serieComprobante;

    @Column(name = "NUMERO_COMPROBANTE", nullable = false, length = 30)
    private String numeroComprobante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PROVEEDOR", nullable = false)
    private Proveedor proveedor;

    @Column(name = "FECHA_EMISION", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "CONDICION_PAGO", length = 30)
    private String condicionPago;

    @Column(name = "FECHA_VENCIMIENTO_PAGO")
    private LocalDate fechaVencimientoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EstadoCompra estado;

    @Column(name = "SUBTOTAL", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "IGV", nullable = false, precision = 12, scale = 2)
    private BigDecimal igv;

    @Column(name = "COSTO_FLETE", precision = 12, scale = 2)
    private BigDecimal costoFlete;

    @Column(name = "TOTAL", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "SALDO_PENDIENTE", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoPendiente;

    @Builder.Default
    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleCompra> detalles = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PagoCompra> pagos = new ArrayList<>();
}
