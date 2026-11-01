package pe.edu.upeu.bomerp.produccion.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListaMaterialesResponse {
    private Long id;
    private String codigo;
    private Long productoId;
    private String nombreProducto;
    private BigDecimal cantidadBase;
    private String observaciones;
    private Boolean activo;
    private Long version;
    private List<DetalleListaMaterialesResponse> detalles;
}

