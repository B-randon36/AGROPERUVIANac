package pe.edu.upeu.bomerp.produccion.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleListaMaterialesResponse {
    private Long id;
    private Long insumoId;
    private String nombreInsumo;
    private BigDecimal cantidadRequerida;
    private String unidadMedida;
}

