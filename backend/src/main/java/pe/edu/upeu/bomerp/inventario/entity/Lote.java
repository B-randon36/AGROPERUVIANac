package pe.edu.upeu.bomerp.inventario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "TBL_LOTES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODIGO_LOTE", nullable = false, unique = true, length = 50)
    private String codigoLote;

    @Column(name = "PRODUCTO_ID", nullable = false)
    private Long productoId;

    @Column(name = "PRODUCTO_NOMBRE", nullable = false, length = 150)
    private String productoNombre;

    @Column(name = "FECHA_FABRICACION", nullable = false)
    private LocalDate fechaFabricacion;

    @Column(name = "FECHA_VENCIMIENTO", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "STOCK_INICIAL", nullable = false, precision = 12, scale = 2)
    private BigDecimal stockInicial;

    @Column(name = "STOCK_ACTUAL", nullable = false, precision = 12, scale = 2)
    private BigDecimal stockActual;

    @Column(name = "COSTO_UNITARIO", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoUnitario;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 25)
    private EstadoLote estado;

    @Column(name = "OBSERVACIONES", length = 255)
    private String observaciones;

    @Version
    @Column(name = "VERSION", columnDefinition = "BIGINT DEFAULT 0")
    @Builder.Default
    private Long version = 0L;
}
