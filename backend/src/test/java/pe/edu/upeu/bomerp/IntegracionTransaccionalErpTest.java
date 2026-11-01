package pe.edu.upeu.bomerp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.bomerp.compras.dto.AmortizarPagoRequest;
import pe.edu.upeu.bomerp.compras.dto.CompraRequest;
import pe.edu.upeu.bomerp.compras.dto.CompraResponse;
import pe.edu.upeu.bomerp.compras.dto.DetalleCompraRequest;
import pe.edu.upeu.bomerp.compras.service.CompraService;
import pe.edu.upeu.bomerp.exception.SolvenciaInsuficienteException;
import pe.edu.upeu.bomerp.finanzas.dto.CuentaPorCobrarResponse;
import pe.edu.upeu.bomerp.finanzas.dto.SesionCajaResponse;
import pe.edu.upeu.bomerp.finanzas.service.CuentaPorCobrarService;
import pe.edu.upeu.bomerp.finanzas.service.SesionCajaService;
import pe.edu.upeu.bomerp.inventario.dto.LoteDto;
import pe.edu.upeu.bomerp.inventario.dto.MovimientoKardexDto;
import pe.edu.upeu.bomerp.inventario.service.InventarioService;
import pe.edu.upeu.bomerp.produccion.dto.CompletarOrdenRequest;
import pe.edu.upeu.bomerp.produccion.dto.DetalleConsumoRequest;
import pe.edu.upeu.bomerp.produccion.dto.OrdenProduccionRequest;
import pe.edu.upeu.bomerp.produccion.dto.OrdenProduccionResponse;
import pe.edu.upeu.bomerp.produccion.service.ProduccionService;
import pe.edu.upeu.bomerp.ventas.dto.DetalleVentaRequest;
import pe.edu.upeu.bomerp.ventas.dto.VentaRequest;
import pe.edu.upeu.bomerp.ventas.dto.VentaResponse;
import pe.edu.upeu.bomerp.ventas.entity.MetodoPagoVenta;
import pe.edu.upeu.bomerp.ventas.service.VentaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class IntegracionTransaccionalErpTest {

    @Autowired
    private VentaService ventaService;

    @Autowired
    private InventarioService inventarioService;

    @Autowired
    private SesionCajaService sesionCajaService;

    @Autowired
    private CuentaPorCobrarService cuentaPorCobrarService;

    @Autowired
    private ProduccionService produccionService;

    @Autowired
    private CompraService compraService;

    @Test
    @DisplayName("E2E 1: Venta POS en EFECTIVO descuenta primero el lote FEFO más próximo a vencer y aumenta la Sesión de Caja activa")
    void ventaPosEfectivo_debeDescontarLoteFefoEImpactarSesionCaja() {
        // Producto 13: Aceite de Coco Virgen 460 ml (tiene LOT-COCO460-2026-A1 con 20 unid próximo a vencer)
        List<LoteDto> lotesAntes = inventarioService.listarLotes(13L);
        assertFalse(lotesAntes.isEmpty(), "Debe existir lote semilla para producto 13");
        LoteDto lotePrioritario = lotesAntes.get(0);
        BigDecimal stockLoteInicial = lotePrioritario.getStockActual();

        SesionCajaResponse sesionAntes = sesionCajaService.obtenerUltimaSesionActiva();
        BigDecimal saldoEsperadoAntes = sesionAntes.getSaldoEsperadoEfectivo();

        DetalleVentaRequest det = DetalleVentaRequest.builder().productoId(13L).cantidad(3).build();
        VentaRequest req = VentaRequest.builder()
                .clienteNombre("Cliente Mostrador Miraflores")
                .metodoPago(MetodoPagoVenta.EFECTIVO)
                .detalles(List.of(det))
                .build();

        VentaResponse venta = ventaService.crear(req, "vendedor-demo");

        // 1. Verificar trazabilidad FEFO en el detalle y descuento en el lote más antiguo
        assertEquals(lotePrioritario.getId(), venta.getDetalles().get(0).getLoteId());
        LoteDto loteDespues = inventarioService.listarLotes(13L).get(0);
        assertEquals(0, stockLoteInicial.subtract(new BigDecimal("3")).compareTo(loteDespues.getStockActual()));

        // 2. Verificar movimiento en Kardex
        List<MovimientoKardexDto> kardex = inventarioService.listarKardex(13L);
        assertTrue(kardex.stream().anyMatch(k -> venta.getNumeroTicket().equals(k.getDocumentoReferencia())));

        // 3. Verificar aumento automático del saldo en efectivo esperado en la Sesión de Caja abierta
        SesionCajaResponse sesionDespues = sesionCajaService.obtenerUltimaSesionActiva();
        assertEquals(0, saldoEsperadoAntes.add(venta.getTotal()).compareTo(sesionDespues.getSaldoEsperadoEfectivo()));
    }

    @Test
    @DisplayName("E2E 2: Venta POS a CREDITO genera automáticamente una Cuenta por Cobrar sin tocar el efectivo en gaveta")
    void ventaPosCredito_debeCrearCuentaPorCobrarSinAlterarEfectivoCaja() {
        SesionCajaResponse sesionAntes = sesionCajaService.obtenerUltimaSesionActiva();
        int totalCreditosAntes = cuentaPorCobrarService.listarTodos(null).size();

        DetalleVentaRequest det = DetalleVentaRequest.builder().productoId(1L).cantidad(2).build(); // 2 baldes de 4L = S/ 400.00
        VentaRequest req = VentaRequest.builder()
                .clienteNombre("Panadería Artesanal Barranco SAC")
                .clienteDocumento("20609988776")
                .metodoPago(MetodoPagoVenta.CREDITO)
                .diasCredito(30)
                .detalles(List.of(det))
                .build();

        VentaResponse venta = ventaService.crear(req, "vendedor-demo");

        List<CuentaPorCobrarResponse> creditosDespues = cuentaPorCobrarService.listarTodos(null);
        assertEquals(totalCreditosAntes + 1, creditosDespues.size());
        assertTrue(creditosDespues.stream().anyMatch(c ->
                c.getCodigo().equals("CRE-" + venta.getNumeroTicket())
                        && c.getSaldoPendiente().compareTo(new BigDecimal("400.00")) == 0));

        SesionCajaResponse sesionDespues = sesionCajaService.obtenerUltimaSesionActiva();
        assertEquals(0, sesionAntes.getSaldoEsperadoEfectivo().compareTo(sesionDespues.getSaldoEsperadoEfectivo()));
    }

    @Test
    @DisplayName("E2E 3: Completar Orden de Producción BOM unifica el nuevo Lote FEFO en Inventario y descuenta insumos en Kardex")
    void completarOrdenProduccion_debeUnificarLoteFefoYRegistrarKardexInsumos() {
        int lotesProd12Antes = inventarioService.listarLotes(12L).size();

        DetalleConsumoRequest insumoReq = DetalleConsumoRequest.builder()
                .insumoId(103L)
                .nombreInsumo("Copra Blanca Deshidratada")
                .cantidadRequerida(new BigDecimal("185.00"))
                .cantidadConsumida(new BigDecimal("185.00"))
                .unidadMedida("KG")
                .build();

        OrdenProduccionResponse creada = produccionService.crearOrden(OrdenProduccionRequest.builder()
                .productoId(12L)
                .nombreProducto("Aceite de Coco Virgen 1 L | Prensado en frío")
                .cantidadProgramada(100)
                .observaciones("Prueba E2E Planta")
                .insumos(List.of(insumoReq))
                .build());

        produccionService.iniciarOrden(creada.getId());
        produccionService.completarOrden(creada.getId(), CompletarOrdenRequest.builder()
                .cantidadProducida(98)
                .fechaVencimiento(LocalDate.now().plusMonths(12))
                .build());

        List<LoteDto> lotesProd12Despues = inventarioService.listarLotes(12L);
        assertEquals(lotesProd12Antes + 1, lotesProd12Despues.size());

        List<MovimientoKardexDto> kardexInsumo = inventarioService.listarKardex(103L);
        assertTrue(kardexInsumo.stream().anyMatch(k -> creada.getCodigoOrden().equals(k.getDocumentoReferencia())));
    }

    @Test
    @DisplayName("E2E 4: Compra ingresa lote de insumo a Kardex y amortización en EFECTIVO aplica control de liquidez RN-FIN-01")
    void compraYAmortizacion_debeIngresarStockYAplicarReglaSolvenciaEfectivo() {
        DetalleCompraRequest det = DetalleCompraRequest.builder()
                .insumoId(108L)
                .nombreInsumo("Frasco de Vidrio 1L Boca Ancha")
                .cantidad(new BigDecimal("500"))
                .precioUnitario(new BigDecimal("4.00"))
                .build();

        CompraResponse compra = compraService.registrarCompra(CompraRequest.builder()
                .proveedorId(2L)
                .numeroComprobante("F009-998877")
                .fechaEmision(LocalDate.now())
                .detalles(List.of(det))
                .build());

        // 1. Verificar que el lote del insumo ingresó a Inventario / Kardex
        assertFalse(inventarioService.listarLotes(108L).isEmpty());

        // 2. Intentar pagar S/ 2000 en EFECTIVO cuando la gaveta tiene S/ 435 debe disparar RN-FIN-01
        AmortizarPagoRequest pagoEfectivoExcesivo = new AmortizarPagoRequest(
                new BigDecimal("2000.00"),
                "EFECTIVO",
                "Intento sin liquidez en gaveta"
        );
        assertThrows(SolvenciaInsuficienteException.class, () ->
                compraService.amortizarPago(compra.getId(), pagoEfectivoExcesivo));

        // 3. Pagar por TRANSFERENCIA bancaria liquida la deuda sin exceder efectivo físico de gaveta
        CompraResponse pagada = compraService.amortizarPago(compra.getId(), new AmortizarPagoRequest(
                new BigDecimal("2000.00"),
                "TRANSFERENCIA",
                "OP-BCP-778899"
        ));
        assertEquals(0, BigDecimal.ZERO.compareTo(pagada.getSaldoPendiente()));
    }
}
