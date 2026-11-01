package pe.edu.upeu.bomerp.produccion.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ORDENES_PRODUCCION", schema = "BOM_PRODUCCION")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenProduccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODIGO_ORDEN", nullable = false, unique = true, length = 30)
    private String codigoOrden;

    @Column(name = "ID_PRODUCTO", nullable = false)
    private Long productoId;

    @Column(name = "NOMBRE_PRODUCTO", nullable = false, length = 120)
    private String nombreProducto;

    @Column(name = "CANTIDAD_PROGRAMADA", nullable = false)
    private Integer cantidadProgramada;

    @Column(name = "CANTIDAD_PRODUCIDA")
    private Integer cantidadProducida;

    @Column(name = "FECHA_INICIO", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "FECHA_FIN")
    private LocalDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EstadoOrdenProduccion estado;

    @Column(name = "OBSERVACIONES", length = 300)
    private String observaciones;

    @Column(name = "ID_CENTRO_TRABAJO")
    private Long centroTrabajoId;

    @Column(name = "NOMBRE_CENTRO_TRABAJO", length = 120)
    private String centroTrabajoNombre;

    @Version
    @Column(name = "VERSION", columnDefinition = "BIGINT DEFAULT 0")
    @Builder.Default
    private Long version = 0L;

    @Builder.Default
    @OneToMany(mappedBy = "ordenProduccion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleConsumoInsumo> insumosConsumidos = new ArrayList<>();
}
