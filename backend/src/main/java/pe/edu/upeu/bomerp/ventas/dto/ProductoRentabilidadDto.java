package pe.edu.upeu.bomerp.ventas.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoRentabilidadDto {
    private Long productoId;
    private String productoNombre;
    private String categoria;
    private BigDecimal unidadesVendidas;
    private BigDecimal totalIngresos;
    private BigDecimal costoEstimado;
    private BigDecimal utilidadBruta;
    private BigDecimal margenContribucion;
    private String clasificacionAbc;
}
