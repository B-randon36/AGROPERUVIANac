package pe.edu.upeu.bomerp.compras.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "PAGOS_COMPRAS", schema = "BOM_COMPRAS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_COMPRA", nullable = false)
    private Compra compra;

    @Column(name = "FECHA_PAGO", nullable = false)
    private LocalDate fechaPago;

    @Column(name = "MONTO_PAGADO", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoPagado;

    @Column(name = "METODO_PAGO", nullable = false, length = 30)
    private String metodoPago;

    @Column(name = "NUMERO_OPERACION", length = 60)
    private String numeroOperacion;

    @Column(name = "ESTADO", nullable = false, length = 20)
    private String estado;

    @Column(name = "FECHA_CREACION", nullable = false)
    private LocalDateTime fechaCreacion;
}

