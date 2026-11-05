package pe.edu.upeu.bomerp.ventas.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class VentaAgregado {
    private final long totalVentas;
    private final BigDecimal montoTotal;
    private final BigDecimal ticketPromedio;
    private final long totalAnuladas;
    private final BigDecimal montoAnulado;

    public VentaAgregado(long totalVentas, BigDecimal montoTotal) {
        this(totalVentas, montoTotal, 0L, BigDecimal.ZERO);
    }

    public VentaAgregado(long totalVentas, BigDecimal montoTotal, long totalAnuladas, BigDecimal montoAnulado) {
        this.totalVentas = totalVentas;
        this.montoTotal = montoTotal != null ? montoTotal : BigDecimal.ZERO;
        this.totalAnuladas = totalAnuladas;
        this.montoAnulado = montoAnulado != null ? montoAnulado : BigDecimal.ZERO;
        long ventasEfectivas = Math.max(0, totalVentas - totalAnuladas);
        this.ticketPromedio = ventasEfectivas == 0
                ? BigDecimal.ZERO
                : this.montoTotal.divide(BigDecimal.valueOf(ventasEfectivas), 2, RoundingMode.HALF_UP);
    }
}
