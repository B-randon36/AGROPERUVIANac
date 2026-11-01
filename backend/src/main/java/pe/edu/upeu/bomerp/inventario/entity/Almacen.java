package pe.edu.upeu.bomerp.inventario.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "TBL_ALMACENES")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Almacen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODIGO", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "NOMBRE", nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(name = "UBICACION", length = 200)
    private String ubicacion;

    @Column(name = "TIPO", length = 40)
    @Builder.Default
    private String tipo = "PRODUCTO_TERMINADO";

    @Column(name = "CAPACIDAD_MAXIMA", nullable = false, precision = 12, scale = 2)
    private BigDecimal capacidadMaxima;

    @Column(name = "OCUPACION_ACTUAL", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal ocupacionActual = BigDecimal.ZERO;

    @Column(name = "RESPONSABLE", length = 100)
    private String responsable;

    @Column(name = "ACTIVO", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Version
    @Column(name = "VERSION", columnDefinition = "BIGINT DEFAULT 0")
    @Builder.Default
    private Long version = 0L;
}

