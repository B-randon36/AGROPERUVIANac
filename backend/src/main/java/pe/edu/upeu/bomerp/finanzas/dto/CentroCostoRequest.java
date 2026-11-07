package pe.edu.upeu.bomerp.finanzas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CentroCostoRequest {

    @NotBlank(message = "El código del centro de costo es obligatorio")
    @Size(max = 30, message = "El código no debe superar 30 caracteres")
    private String codigoCc;

    @NotBlank(message = "El nombre del área o línea es obligatorio")
    @Size(max = 100, message = "El nombre no debe superar 100 caracteres")
    private String nombreArea;
}
