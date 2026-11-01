package pe.edu.upeu.bomerp.produccion.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "LISTAS_MATERIALES", schema = "BOM_PRODUCCION")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListaMateriales {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODIGO", nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(name = "ID_PRODUCTO", nullable = false)
    private Long productoId;

    @Column(name = "NOMBRE_PRODUCTO", nullable = false, length = 150)
    private String nombreProducto;

    @Column(name = "CANTIDAD_BASE", nullable = false, precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal cantidadBase = BigDecimal.ONE;

    @Column(name = "OBSERVACIONES", length = 255)
    private String observaciones;

    @Column(name = "ACTIVO", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Version
    @Column(name = "VERSION", columnDefinition = "BIGINT DEFAULT 0")
    @Builder.Default
    private Long version = 0L;

    @Builder.Default
    @OneToMany(mappedBy = "listaMateriales", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleListaMateriales> detalles = new ArrayList<>();
}

