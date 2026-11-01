package pe.edu.upeu.bomerp.produccion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenProduccionRequest {

    @NotNull(message = "El ID del producto a elaborar es obligatorio")
    private Long productoId;

    @NotBlank(message = "El nombre del producto es obligatorio")
    private String nombreProducto;

    @NotNull(message = "La cantidad programada es obligatoria")
    @Positive(message = "La cantidad programada debe ser mayor a 0")
    private Integer cantidadProgramada;

    private Long centroTrabajoId;

    private String centroTrabajoNombre;

    private String observaciones;

    @NotEmpty(message = "La orden de producción debe listar los insumos de la receta BOM a consumir")
    @Valid
    private List<DetalleConsumoRequest> insumos;
}
