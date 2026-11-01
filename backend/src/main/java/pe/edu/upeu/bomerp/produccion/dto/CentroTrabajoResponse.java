package pe.edu.upeu.bomerp.produccion.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CentroTrabajoResponse {
    private Long id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private BigDecimal capacidad;
    private String unidadCapacidad;
    private BigDecimal costoHora;
    private Boolean activo;
    private Long version;
}

