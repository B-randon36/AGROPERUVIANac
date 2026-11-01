package pe.edu.upeu.bomerp.produccion.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "LOTES_PRODUCTO", schema = "BOM_PRODUCCION")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoteProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODIGO_LOTE", nullable = false, unique = true, length = 30)
    private String codigoLote;

    @Column(name = "ID_PRODUCTO", nullable = false)
    private Long productoId;

    @Column(name = "NOMBRE_PRODUCTO", nullable = false, length = 120)
    private String nombreProducto;

    @Column(name = "STOCK_DISPONIBLE", nullable = false)
    private Integer stockDisponible;

    @Column(name = "FECHA_PRODUCCION", nullable = false)
    private LocalDate fechaProduccion;

    @Column(name = "FECHA_VENCIMIENTO", nullable = false)
    private LocalDate fechaVencimiento;

    @Version
    @Column(name = "VERSION", columnDefinition = "BIGINT DEFAULT 0")
    @Builder.Default
    private Long version = 0L;
}
