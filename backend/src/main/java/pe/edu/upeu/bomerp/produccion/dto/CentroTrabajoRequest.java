package pe.edu.upeu.bomerp.produccion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CentroTrabajoRequest {

    @NotBlank(message = "El código del centro de trabajo es obligatorio")
    private String codigo;

    @NotBlank(message = "El nombre del centro de trabajo es obligatorio")
    private String nombre;

    private String descripcion;

    @PositiveOrZero(message = "La capacidad no puede ser negativa")
    private BigDecimal capacidad;

    private String unidadCapacidad;

    @NotNull(message = "El costo operativo por hora es obligatorio")
    @PositiveOrZero(message = "El costo por hora no puede ser negativo")
    private BigDecimal costoHora;

    private Boolean activo;
}

