package pe.edu.upeu.bomerp.produccion.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "DETALLES_LISTA_MATERIALES", schema = "BOM_PRODUCCION")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleListaMateriales {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_LISTA_MATERIALES", nullable = false)
    private ListaMateriales listaMateriales;

    @Column(name = "ID_INSUMO", nullable = false)
    private Long insumoId;

    @Column(name = "NOMBRE_INSUMO", nullable = false, length = 150)
    private String nombreInsumo;

    @Column(name = "CANTIDAD_REQUERIDA", nullable = false, precision = 12, scale = 4)
    private BigDecimal cantidadRequerida;

    @Column(name = "UNIDAD_MEDIDA", nullable = false, length = 25)
    private String unidadMedida;
}

