package pe.edu.upeu.bomerp.produccion.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "CENTROS_TRABAJO", schema = "BOM_PRODUCCION")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CentroTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODIGO", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "NOMBRE", nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(name = "DESCRIPCION", length = 250)
    private String descripcion;

    @Column(name = "CAPACIDAD", precision = 12, scale = 4)
    private BigDecimal capacidad;

    @Column(name = "UNIDAD_CAPACIDAD", length = 30)
    @Builder.Default
    private String unidadCapacidad = "UNID/HORA";

    @Column(name = "COSTO_HORA", nullable = false, precision = 12, scale = 4)
    private BigDecimal costoHora;

    @Column(name = "ACTIVO", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Version
    @Column(name = "VERSION", columnDefinition = "BIGINT DEFAULT 0")
    @Builder.Default
    private Long version = 0L;
}

