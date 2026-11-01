package pe.edu.upeu.bomerp.inventario.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCriticoDto {
    private Long productoId;
    private String nombreProducto;
    private String categoria;
    private BigDecimal precioUnitario;
    private Integer stockActual;
    private BigDecimal stockLotesVigentes;
    private Integer stockMinimo;
    private Integer deficit;
    private String nivelAlerta;
}

