package pe.edu.upeu.bomerp.compras.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsumoDto {
    private Long id;
    private String codigo;
    private String nombre;
    private String unidadMedida;
    private BigDecimal precioSugerido;
    private String estado;
}
