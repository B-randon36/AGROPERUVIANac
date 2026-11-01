package pe.edu.upeu.bomerp.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlmacenRequest {

    @NotBlank(message = "El código del almacén es obligatorio")
    private String codigo;

    @NotBlank(message = "El nombre del almacén es obligatorio")
    private String nombre;

    private String ubicacion;

    private String tipo;

    @NotNull(message = "La capacidad máxima es obligatoria")
    @Positive(message = "La capacidad máxima debe ser mayor a 0")
    private BigDecimal capacidadMaxima;

    @PositiveOrZero(message = "La ocupación actual no puede ser negativa")
    private BigDecimal ocupacionActual;

    private String responsable;

    private Boolean activo;
}

