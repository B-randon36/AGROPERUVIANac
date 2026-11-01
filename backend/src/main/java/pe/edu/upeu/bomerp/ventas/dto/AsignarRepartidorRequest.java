package pe.edu.upeu.bomerp.ventas.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsignarRepartidorRequest {

    @NotBlank(message = "El nombre del repartidor es obligatorio")
    private String repartidorNombre;
}

