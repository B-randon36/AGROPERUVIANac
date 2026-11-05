package pe.edu.upeu.bomerp.compras.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "DETALLE_COMPRAS", schema = "BOM_COMPRAS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_COMPRA", nullable = false)
    private Compra compra;

    @Column(name = "ID_INSUMO", nullable = false)
    private Long insumoId;

    @Column(name = "NOMBRE_INSUMO", nullable = false, length = 120)
    private String nombreInsumo;

    @Column(name = "LOTE_PROVEEDOR", length = 50)
    private String loteProveedor;

    @Column(name = "FECHA_VENCIMIENTO")
    private LocalDate fechaVencimiento;

    @Column(name = "FACTOR_CONVERSION", precision = 8, scale = 2)
    private BigDecimal factorConversion;

    @Column(name = "CANTIDAD", nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "CANTIDAD_INGRESO", precision = 12, scale = 3)
    private BigDecimal cantidadIngreso;

    @Column(name = "PRECIO_UNITARIO", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "SUBTOTAL", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
}
