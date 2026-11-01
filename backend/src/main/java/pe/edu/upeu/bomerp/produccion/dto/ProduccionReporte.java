package pe.edu.upeu.bomerp.produccion.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProduccionReporte {
    private Long totalOrdenes;
    private Long ordenesCompletadas;
    private Long ordenesEnProceso;
    private Long ordenesPlanificadas;
    private Long totalCantidadProgramada;
    private Long totalCantidadProducida;
    private Double porcentajeEficiencia;
    private List<OrdenProduccionResponse> ordenes;
}
