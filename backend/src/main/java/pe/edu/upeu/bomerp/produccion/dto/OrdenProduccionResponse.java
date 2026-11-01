package pe.edu.upeu.bomerp.produccion.dto;

import lombok.*;
import pe.edu.upeu.bomerp.produccion.entity.EstadoOrdenProduccion;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenProduccionResponse {
    private Long id;
    private String codigoOrden;
    private Long productoId;
    private String nombreProducto;
    private Integer cantidadProgramada;
    private Integer cantidadProducida;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private EstadoOrdenProduccion estado;
    private Long centroTrabajoId;
    private String centroTrabajoNombre;
    private String observaciones;
    private Long version;
    private List<DetalleConsumoResponse> insumosConsumidos;
}
