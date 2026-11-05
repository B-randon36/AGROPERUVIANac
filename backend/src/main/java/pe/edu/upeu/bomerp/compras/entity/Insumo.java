package pe.edu.upeu.bomerp.compras.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "INSUMOS", schema = "BOM_COMPRAS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Insumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODIGO", length = 30, unique = true)
    private String codigo;

    @Column(name = "NOMBRE", nullable = false, length = 150)
    private String nombre;

    @Column(name = "UNIDAD_MEDIDA", nullable = false, length = 20)
    private String unidadMedida;

    @Column(name = "PRECIO_SUGERIDO", precision = 10, scale = 2)
    private BigDecimal precioSugerido;

    @Column(name = "ESTADO", length = 20)
    private String estado;
}

