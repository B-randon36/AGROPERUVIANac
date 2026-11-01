package pe.edu.upeu.bomerp.inventario.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlmacenResponse {
    private Long id;
    private String codigo;
    private String nombre;
    private String ubicacion;
    private String tipo;
    private BigDecimal capacidadMaxima;
    private BigDecimal ocupacionActual;
    private Double porcentajeOcupacion;
    private String nivelOcupacion;
    private String responsable;
    private Boolean activo;
    private Long version;
}

