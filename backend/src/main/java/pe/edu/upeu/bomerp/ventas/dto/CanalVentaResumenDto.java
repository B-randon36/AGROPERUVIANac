package pe.edu.upeu.bomerp.ventas.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanalVentaResumenDto {
    private String canal;
    private String canalNombre;
    private BigDecimal totalVentas;
    private Long cantidadTransacciones;
    private BigDecimal ticketPromedio;
    private BigDecimal porcentaje;
}
