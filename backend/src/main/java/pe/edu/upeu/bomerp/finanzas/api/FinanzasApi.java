package pe.edu.upeu.bomerp.finanzas.api;

import pe.edu.upeu.bomerp.finanzas.entity.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Puerto público de Spring Modulith para el módulo de Finanzas.
 * Permite a otros módulos del ERP (Ventas POS, Delivery y Compras) impactar Sesiones de Caja,
 * Cuentas por Cobrar y Transacciones Financieras sin acoplarse a repositorios ni entidades JPA internas.
 */
public interface FinanzasApi {

    /**
     * Registra un cobro transaccional originado en una venta sobre la sesión activa del usuario.
     */
    Long registrarCobroVenta(Long usuarioId, BigDecimal monto, MetodoPago metodoPago, String referenciaVenta, Long categoriaId);

    /**
     * Registra automáticamente el ingreso en el turno de caja abierto actual y la transacción financiera PAGADA
     * al emitir una venta POS al contado o liquidar un cobro contra entrega (Delivery).
     */
    Long registrarIngresoVentaPos(BigDecimal monto, MetodoPago metodoPago, String numeroTicket, String clienteNombre);

    /**
     * Registra automáticamente una Cuenta por Cobrar (PENDIENTE) y su transacción financiera devengada
     * cuando una venta POS se emite bajo modalidad CREDITO.
     */
    Long registrarVentaACredito(
            String numeroTicket,
            String clienteNombre,
            String clienteDocumento,
            BigDecimal montoTotal,
            Integer diasCredito
    );

    /**
     * Extorna el impacto financiero (Transacción Financiera, Sesión de Caja o Cuenta por Cobrar) al anular una venta POS.
     */
    void revertirVentaPos(String numeroTicket, BigDecimal monto, MetodoPago metodoPago, boolean eraCredito);

    /**
     * Registra la obligación financiera (EGRESO PENDIENTE) al ingresar una factura/comprobante de Compra de insumos.
     */
    void registrarFacturaCompraPendiente(
            String numeroComprobante,
            String razonSocialProveedor,
            BigDecimal montoTotal,
            LocalDate fechaEmision
    );

    /**
     * Registra el egreso en la Sesión de Caja activa (aplicando RN-FIN-01 de solvencia en efectivo) y en
     * Transacciones Financieras al amortizar o cancelar el pago a un proveedor.
     */
    void registrarPagoProveedorCompra(
            String numeroComprobante,
            String razonSocialProveedor,
            BigDecimal montoPagado,
            MetodoPago metodoPago,
            boolean liquidadaTotalmente
    );

    /**
     * Anula la obligación financiera pendiente cuando una Orden de Compra se anula.
     */
    void anularFacturaCompra(String numeroComprobante);

    /**
     * Verifica si el usuario dispone de un turno de caja en estado ABIERTO.
     */
    boolean tieneSesionAbierta(Long usuarioId);

    /**
     * Retorna el ID de la sesión activa del usuario, o lanza excepción si no existe.
     */
    Long obtenerIdSesionActiva(Long usuarioId);
}
