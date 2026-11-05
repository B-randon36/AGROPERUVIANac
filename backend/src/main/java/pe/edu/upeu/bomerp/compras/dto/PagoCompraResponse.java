package pe.edu.upeu.bomerp.compras.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoCompraResponse {
    private Long id;
    private Long compraId;
    private LocalDate fechaPago;
    private BigDecimal montoPagado;
    private String metodoPago;
    private String numeroOperacion;
    private String estado;
    private LocalDateTime fechaCreacion;
}
