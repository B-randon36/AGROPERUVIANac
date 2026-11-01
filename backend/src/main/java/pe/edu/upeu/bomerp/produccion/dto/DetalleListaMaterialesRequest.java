package pe.edu.upeu.bomerp.produccion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleListaMaterialesRequest {

    @NotNull(message = "El ID del insumo es obligatorio")
    private Long insumoId;

    @NotBlank(message = "El nombre del insumo es obligatorio")
    private String nombreInsumo;

    @NotNull(message = "La cantidad requerida es obligatoria")
    @Positive(message = "La cantidad requerida debe ser mayor a 0")
    private BigDecimal cantidadRequerida;

    @NotBlank(message = "La unidad de medida es obligatoria")
    private String unidadMedida;
}

