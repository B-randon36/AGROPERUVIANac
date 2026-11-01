package pe.edu.upeu.bomerp.produccion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListaMaterialesRequest {

    @NotBlank(message = "El código de la receta BOM es obligatorio")
    private String codigo;

    @NotNull(message = "El ID del producto terminado es obligatorio")
    private Long productoId;

    @NotBlank(message = "El nombre del producto terminado es obligatorio")
    private String nombreProducto;

    @NotNull(message = "La cantidad base de la receta es obligatoria")
    @Positive(message = "La cantidad base debe ser mayor a 0")
    private BigDecimal cantidadBase;

    private String observaciones;

    private Boolean activo;

    @NotEmpty(message = "La receta BOM debe incluir al menos un insumo o material")
    @Valid
    private List<DetalleListaMaterialesRequest> detalles;
}

