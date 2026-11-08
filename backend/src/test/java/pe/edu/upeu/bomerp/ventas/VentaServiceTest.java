package pe.edu.upeu.bomerp.ventas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.bomerp.catalogo.categoria.dto.CategoriaResumen;
import pe.edu.upeu.bomerp.catalogo.producto.dto.ProductoResponse;
import pe.edu.upeu.bomerp.catalogo.producto.service.ProductoService;
import pe.edu.upeu.bomerp.exception.BusinessConflictException;
import pe.edu.upeu.bomerp.finanzas.api.FinanzasApi;
import pe.edu.upeu.bomerp.finanzas.entity.MetodoPago;
import pe.edu.upeu.bomerp.inventario.service.InventarioService;
import pe.edu.upeu.bomerp.ventas.dto.DetalleVentaRequest;
import pe.edu.upeu.bomerp.ventas.dto.VentaRequest;
import pe.edu.upeu.bomerp.ventas.dto.VentaResponse;
import pe.edu.upeu.bomerp.ventas.entity.EstadoVenta;
import pe.edu.upeu.bomerp.ventas.entity.MetodoPagoVenta;
import pe.edu.upeu.bomerp.ventas.entity.Venta;
import pe.edu.upeu.bomerp.ventas.repository.VentaRepository;
import pe.edu.upeu.bomerp.ventas.service.AuditoriaService;
import pe.edu.upeu.bomerp.ventas.service.VentaServiceImpl;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private ProductoService productoService;

    @Mock
    private InventarioService inventarioService;

    @Mock
    private FinanzasApi finanzasApi;

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private pe.edu.upeu.bomerp.ventas.service.NumeroTicketService numeroTicketService;

    @Mock
    private pe.edu.upeu.bomerp.ventas.repository.PedidoDeliveryRepository deliveryRepository;

    @InjectMocks
    private VentaServiceImpl ventaService;

    private ProductoResponse mockProducto;

    @BeforeEach
    void setUp() {
        lenient().when(numeroTicketService.siguiente()).thenReturn("TKT-S-0000000001");
        mockProducto = ProductoResponse.builder()
                .id(13L)
                .nombre("Aceite de Coco Virgen 460 ml | Prensado en Frío")
                .precio(new BigDecimal("10.00"))
                .stock(100)
                .categoria(new CategoriaResumen(6L, "Aceites y Derivados de Coco"))
                .build();
    }

    @Test
    @DisplayName("Debe registrar venta POS descontando lote FEFO e impactando Sesión de Caja activa")
    void crear_ventaValida_debeCalcularTotalesDespacharFefoERegistrarEnCaja() {
        DetalleVentaRequest item = new DetalleVentaRequest();
        item.setProductoId(13L);
        item.setCantidad(2);

        VentaRequest request = new VentaRequest();
        request.setClienteNombre("Supermercados Wong");
        request.setMetodoPago(MetodoPagoVenta.EFECTIVO);
        request.setDetalles(List.of(item));

        when(productoService.obtener(13L)).thenReturn(mockProducto);
        doNothing().when(productoService).descontarStock(13L, 2);
        when(inventarioService.registrarSalidaVentaFEFO(eq(13L), any(), eq(2), any())).thenReturn(77L);

        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> {
            Venta v = invocation.getArgument(0);
            v.setId(100L);
            return v;
        });

        VentaResponse response = ventaService.crear(request);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Supermercados Wong", response.getClienteNombre());
        assertEquals(EstadoVenta.REGISTRADA, response.getEstado());
        assertEquals(MetodoPagoVenta.EFECTIVO, response.getMetodoPago());
        assertEquals(77L, response.getDetalles().get(0).getLoteId());

        assertEquals(new BigDecimal("20.00"), response.getTotal());
        assertEquals(new BigDecimal("3.05"), response.getIgv());
        assertEquals(new BigDecimal("16.95"), response.getSubtotal());

        verify(productoService, times(1)).descontarStock(13L, 2);
        verify(inventarioService, times(1)).registrarSalidaVentaFEFO(eq(13L), any(), eq(2), any());
        verify(finanzasApi, times(1)).registrarIngresoVentaPos(
                eq(new BigDecimal("20.00")),
                eq(MetodoPago.EFECTIVO),
                any(),
                eq("Supermercados Wong"),
                any(),
                any()
        );
        verify(ventaRepository, times(1)).save(any(Venta.class));
    }

    @Test
    @DisplayName("Debe generar automáticamente una Cuenta por Cobrar cuando la venta POS es a CREDITO")
    void crear_ventaACredito_debeGenerarCuentaPorCobrarEnFinanzas() {
        DetalleVentaRequest item = new DetalleVentaRequest();
        item.setProductoId(13L);
        item.setCantidad(5);

        VentaRequest request = VentaRequest.builder()
                .clienteNombre("BioMercado Chacarilla S.A.C.")
                .clienteDocumento("20601234567")
                .metodoPago(MetodoPagoVenta.CREDITO)
                .diasCredito(45)
                .detalles(List.of(item))
                .build();

        when(productoService.obtener(13L)).thenReturn(mockProducto);
        when(ventaRepository.save(any(Venta.class))).thenAnswer(i -> {
            Venta v = i.getArgument(0);
            v.setId(105L);
            return v;
        });

        VentaResponse response = ventaService.crear(request, "vendedor-1");

        assertNotNull(response);
        assertEquals(MetodoPagoVenta.CREDITO, response.getMetodoPago());
        verify(finanzasApi, times(1)).registrarVentaACredito(
                any(),
                eq("BioMercado Chacarilla S.A.C."),
                eq("20601234567"),
                eq(new BigDecimal("50.00")),
                eq(45)
        );
        verify(finanzasApi, never()).registrarIngresoVentaPos(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Debe fallar y abortar la venta si el stock es insuficiente (rollback)")
    void crear_stockInsuficiente_debeLanzarExcepcion() {
        DetalleVentaRequest item = new DetalleVentaRequest();
        item.setProductoId(13L);
        item.setCantidad(500);

        VentaRequest request = new VentaRequest();
        request.setClienteNombre("Cliente Mayorista");
        request.setMetodoPago(MetodoPagoVenta.TRANSFERENCIA);
        request.setDetalles(List.of(item));

        when(productoService.obtener(13L)).thenReturn(mockProducto);
        doThrow(new BusinessConflictException("Stock insuficiente"))
                .when(productoService).descontarStock(13L, 500);

        assertThrows(BusinessConflictException.class, () -> ventaService.crear(request));
        verify(ventaRepository, never()).save(any(Venta.class));
        verify(finanzasApi, never()).registrarIngresoVentaPos(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Debe anular una venta registrada restaurando catálogo, lote FEFO y extornando Finanzas")
    void anular_ventaRegistrada_debeRestaurarStockLoteYExtornarFinanzas() {
        pe.edu.upeu.bomerp.ventas.entity.DetalleVenta detalle = pe.edu.upeu.bomerp.ventas.entity.DetalleVenta.builder()
                .id(1L)
                .productoId(13L)
                .loteId(77L)
                .nombreProducto("Aceite de Coco Virgen 460 ml")
                .precioUnitario(new BigDecimal("10.00"))
                .cantidad(5)
                .subtotal(new BigDecimal("50.00"))
                .build();

        Venta venta = Venta.builder()
                .id(200L)
                .numeroTicket("TKT-20261021-001")
                .estado(EstadoVenta.REGISTRADA)
                .metodoPago(MetodoPagoVenta.EFECTIVO)
                .total(new BigDecimal("50.00"))
                .detalles(new java.util.ArrayList<>(List.of(detalle)))
                .build();

        when(ventaRepository.findById(200L)).thenReturn(java.util.Optional.of(venta));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(i -> i.getArgument(0));

        VentaResponse response = ventaService.anular(200L);

        assertNotNull(response);
        assertEquals(EstadoVenta.ANULADA, response.getEstado());
        verify(productoService, times(1)).restaurarStock(13L, 5);
        verify(inventarioService, times(1)).revertirSalidaVentaPorTicket("TKT-20261021-001");
        verify(finanzasApi, times(1)).revertirVentaPos(eq("TKT-20261021-001"), eq(new BigDecimal("50.00")), eq(MetodoPago.EFECTIVO), eq(false), any());
        verify(ventaRepository, times(1)).save(venta);
    }

    @Test
    @DisplayName("Debe rechazar con VentaYaAnuladaException si la venta ya está en estado ANULADA")
    void anular_ventaYaAnulada_debeLanzarExcepcion() {
        Venta venta = Venta.builder()
                .id(201L)
                .estado(EstadoVenta.ANULADA)
                .build();

        when(ventaRepository.findById(201L)).thenReturn(java.util.Optional.of(venta));

        assertThrows(pe.edu.upeu.bomerp.exception.VentaYaAnuladaException.class, () -> ventaService.anular(201L));
        verify(productoService, never()).restaurarStock(any(), any());
        verify(ventaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe permitir al vendedor solicitar la anulación de una venta")
    void solicitarAnulacion_debeCambiarEstadoASolicitada() {
        Venta venta = Venta.builder()
                .id(205L)
                .vendedorId("vendedor-1")
                .numeroTicket("TKT-20261021-005")
                .estado(EstadoVenta.REGISTRADA)
                .total(new BigDecimal("80.00"))
                .build();

        when(ventaRepository.findById(205L)).thenReturn(java.util.Optional.of(venta));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(i -> i.getArgument(0));

        VentaResponse response = ventaService.solicitarAnulacion(205L, "Error en producto", "vendedor-1");

        assertNotNull(response);
        assertEquals(EstadoVenta.SOLICITADA_ANULACION, response.getEstado());
        verify(ventaRepository, times(1)).save(venta);
    }

    @Test
    @DisplayName("Debe permitir al supervisor rechazar la solicitud de anulación y volver a REGISTRADA")
    void rechazarAnulacion_debeRestaurarEstadoRegistrada() {
        Venta venta = Venta.builder()
                .id(206L)
                .numeroTicket("TKT-20261021-006")
                .estado(EstadoVenta.SOLICITADA_ANULACION)
                .total(new BigDecimal("80.00"))
                .build();

        when(ventaRepository.findById(206L)).thenReturn(java.util.Optional.of(venta));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(i -> i.getArgument(0));

        VentaResponse response = ventaService.rechazarAnulacion(206L, "admin-1");

        assertNotNull(response);
        assertEquals(EstadoVenta.REGISTRADA, response.getEstado());
        verify(ventaRepository, times(1)).save(venta);
    }

    @Test
    @DisplayName("RN5: Debe persistir el vendedorId tomado del JWT en la cabecera de la venta")
    void crear_conVendedorId_debeAsignarVendedorIdEnCabecera() {
        DetalleVentaRequest item = new DetalleVentaRequest();
        item.setProductoId(13L);
        item.setCantidad(1);

        VentaRequest request = new VentaRequest();
        request.setClienteNombre("Cliente Prueba");
        request.setDetalles(List.of(item));

        when(productoService.obtener(13L)).thenReturn(mockProducto);
        when(ventaRepository.save(any(Venta.class))).thenAnswer(i -> {
            Venta v = i.getArgument(0);
            v.setId(301L);
            return v;
        });

        VentaResponse response = ventaService.crear(request, "vendedor-uuid-123");

        assertNotNull(response);
        assertEquals("vendedor-uuid-123", response.getVendedorId());
    }

    @Test
    @DisplayName("RN8: Vendedor solo puede consultar su propia venta (mismo vendedorId)")
    void obtener_propiaVenta_debePermitirAcceso() {
        Venta venta = Venta.builder()
                .id(401L)
                .vendedorId("1")
                .estado(EstadoVenta.REGISTRADA)
                .total(new BigDecimal("100.00"))
                .build();

        when(ventaRepository.findById(401L)).thenReturn(java.util.Optional.of(venta));

        VentaResponse response = ventaService.obtener(401L, "1");
        assertNotNull(response);
        assertEquals(401L, response.getId());
        assertEquals("1", response.getVendedorId());
    }

    @Test
    @DisplayName("RN8: Vendedor no puede consultar venta ajena y lanza AccesoDenegadoException (HTTP 403)")
    void obtener_ventaAjena_debeLanzarAccesoDenegadoException() {
        Venta venta = Venta.builder()
                .id(402L)
                .vendedorId("1")
                .estado(EstadoVenta.REGISTRADA)
                .total(new BigDecimal("100.00"))
                .build();

        when(ventaRepository.findById(402L)).thenReturn(java.util.Optional.of(venta));

        assertThrows(pe.edu.upeu.bomerp.exception.AccesoDenegadoException.class,
                () -> ventaService.obtener(402L, "2"));
    }

    @Test
    @DisplayName("RN8: Supervisor/Admin (vendedorId null) puede consultar cualquier venta sin importar de quién sea")
    void obtener_comoSupervisor_debePermitirCualquierVenta() {
        Venta venta = Venta.builder()
                .id(403L)
                .vendedorId("1")
                .estado(EstadoVenta.REGISTRADA)
                .total(new BigDecimal("100.00"))
                .build();

        when(ventaRepository.findById(403L)).thenReturn(java.util.Optional.of(venta));

        VentaResponse response = ventaService.obtener(403L, null);
        assertNotNull(response);
        assertEquals(403L, response.getId());
    }

    @Test
    @DisplayName("Reporte: Debe calcular métricas reflejando ventas anuladas y monto neto descontado")
    void reporte_debeRetornarMetricasConAnuladasYMontoNeto() {
        pe.edu.upeu.bomerp.ventas.dto.VentaAgregado mockAgregado =
                new pe.edu.upeu.bomerp.ventas.dto.VentaAgregado(10L, new BigDecimal("1600.00"), 2L, new BigDecimal("400.00"));

        when(ventaRepository.agregados(isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(mockAgregado);
        when(ventaRepository.buscarResumen(isNull(), isNull(), isNull(), isNull(), isNull(), any())).thenReturn(List.of());

        pe.edu.upeu.bomerp.ventas.dto.VentaReporte reporte = ventaService.reporte(null, null, null);

        assertNotNull(reporte);
        assertNotNull(reporte.getAgregado());
        assertEquals(10L, reporte.getAgregado().getTotalVentas());
        assertEquals(2L, reporte.getAgregado().getTotalAnuladas());
        assertEquals(new BigDecimal("1600.00"), reporte.getAgregado().getMontoTotal());
        assertEquals(new BigDecimal("400.00"), reporte.getAgregado().getMontoAnulado());
        // 1600 / 8 ventas efectivas = 200.00 de ticket promedio
        assertEquals(new BigDecimal("200.00"), reporte.getAgregado().getTicketPromedio());
    }

    @Test
    @DisplayName("Reporte con Método de Pago: Debe filtrar agregados por método de pago específico")
    void reporte_conMetodoPago_debeFiltrarPorMetodo() {
        pe.edu.upeu.bomerp.ventas.dto.VentaAgregado mockAgregado =
                new pe.edu.upeu.bomerp.ventas.dto.VentaAgregado(5L, new BigDecimal("800.00"), 0L, BigDecimal.ZERO);

        when(ventaRepository.agregados(isNull(), org.mockito.ArgumentMatchers.eq(pe.edu.upeu.bomerp.ventas.entity.MetodoPagoVenta.YAPE), isNull(), isNull(), isNull()))
                .thenReturn(mockAgregado);
        when(ventaRepository.buscarResumen(isNull(), org.mockito.ArgumentMatchers.eq(pe.edu.upeu.bomerp.ventas.entity.MetodoPagoVenta.YAPE), isNull(), isNull(), isNull(), any()))
                .thenReturn(List.of());

        pe.edu.upeu.bomerp.ventas.dto.VentaReporte reporte =
                ventaService.reporte(null, pe.edu.upeu.bomerp.ventas.entity.MetodoPagoVenta.YAPE, null, null);

        assertNotNull(reporte);
        assertNotNull(reporte.getAgregado());
        assertEquals(5L, reporte.getAgregado().getTotalVentas());
        assertEquals(0L, reporte.getAgregado().getTotalAnuladas());
        assertEquals(new BigDecimal("800.00"), reporte.getAgregado().getMontoTotal());
    }
    @Test
    void solicitarAnulacionAjena_noModificaVentaNiAuditoria() {
        Venta venta = Venta.builder().id(10L).vendedorId("otro").estado(EstadoVenta.REGISTRADA).build();
        when(ventaRepository.findById(10L)).thenReturn(java.util.Optional.of(venta));
        assertThrows(pe.edu.upeu.bomerp.exception.AccesoDenegadoException.class,
                () -> ventaService.solicitarAnulacion(10L, "Error", "vendedor-1", "vendedor-1"));
        assertEquals(EstadoVenta.REGISTRADA, venta.getEstado());
        verify(ventaRepository, never()).save(any());
        verifyNoInteractions(auditoriaService, productoService, finanzasApi);
    }

    @Test
    void reporteFiltrado_transmiteVendedorAListaYTotales() {
        when(ventaRepository.agregados(null, null, null, null, "vendedor-1"))
                .thenReturn(new pe.edu.upeu.bomerp.ventas.dto.VentaAgregado(0L, BigDecimal.ZERO, 0L, BigDecimal.ZERO));
        when(ventaRepository.buscarResumen(isNull(), isNull(), isNull(), isNull(), eq("vendedor-1"), any()))
                .thenReturn(List.of());
        ventaService.reporte(null, null, null, null, "vendedor-1");
        verify(ventaRepository).agregados(null, null, null, null, "vendedor-1");
        verify(ventaRepository).buscarResumen(isNull(), isNull(), isNull(), isNull(), eq("vendedor-1"), any());
    }

    @Test
    void precioFinal118_desglosa100Mas18YUsaTicketDeSecuencia() {
        mockProducto.setPrecio(new BigDecimal("118.00"));
        when(productoService.obtener(13L)).thenReturn(mockProducto);
        when(ventaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        VentaRequest request = VentaRequest.builder().metodoPago(MetodoPagoVenta.EFECTIVO)
                .detalles(List.of(DetalleVentaRequest.builder().productoId(13L).cantidad(1).build())).build();
        VentaResponse respuesta = ventaService.crear(request, "vendedor-1");
        assertEquals(new BigDecimal("118.00"), respuesta.getTotal());
        assertEquals(new BigDecimal("100.00"), respuesta.getSubtotal());
        assertEquals(new BigDecimal("18.00"), respuesta.getIgv());
        assertEquals("TKT-S-0000000001", respuesta.getNumeroTicket());
    }

    @Test
    void omnicanalVacio_noInventaVentasNiProductos() {
        when(ventaRepository.findAll()).thenReturn(List.of());
        when(deliveryRepository.findAll()).thenReturn(List.of());
        var reporte = ventaService.obtenerReporteOmnicanal();
        assertEquals(0, reporte.getTotalVentasOmnicanal().signum());
        assertEquals(0L, reporte.getTotalTransacciones());
        assertTrue(reporte.getRankingProductos().isEmpty());
        assertTrue(reporte.getCanales().stream().allMatch(c -> c.getCantidadTransacciones() == 0));
    }

    @Test
    void omnicanal_noDuplicaVentasVinculadasNiCuentaPedidosPendientes() {
        var detalle = pe.edu.upeu.bomerp.ventas.entity.DetalleVenta.builder()
                .productoId(13L).nombreProducto("Producto real").cantidad(2).subtotal(new BigDecimal("118.00")).build();
        Venta venta = Venta.builder().id(10L).estado(EstadoVenta.REGISTRADA)
                .total(new BigDecimal("118.00")).detalles(List.of(detalle)).build();
        Venta anulada = Venta.builder().id(11L).estado(EstadoVenta.ANULADA).total(new BigDecimal("900.00")).build();
        var vinculado = pedido(10L, "ENTREGADO", "118.00");
        var duplicado = pedido(10L, "PENDIENTE", "118.00");
        var pendiente = pedido(null, "PENDIENTE", "70.00");
        var entregado = pedido(null, "ENTREGADO", "25.00");
        var cancelado = pedido(null, "CANCELADO", "80.00");
        when(ventaRepository.findAll()).thenReturn(List.of(venta, anulada));
        when(deliveryRepository.findAll()).thenReturn(List.of(vinculado, duplicado, pendiente, entregado, cancelado));
        var reporte = ventaService.obtenerReporteOmnicanal();
        assertEquals(new BigDecimal("143.00"), reporte.getTotalVentasOmnicanal());
        assertEquals(2L, reporte.getTotalTransacciones());
        assertEquals(1, reporte.getRankingProductos().size());
        var fila = reporte.getRankingProductos().get(0);
        assertEquals(new BigDecimal("118.00"), fila.getTotalIngresos());
        assertEquals(new BigDecimal("2"), fila.getUnidadesVendidas());
        assertNull(fila.getCostoEstimado());
        assertNull(fila.getUtilidadBruta());
        assertNull(fila.getMargenContribucion());
    }

    private pe.edu.upeu.bomerp.ventas.entity.PedidoDelivery pedido(Long ventaId, String estado, String monto) {
        return pe.edu.upeu.bomerp.ventas.entity.PedidoDelivery.builder().ventaId(ventaId)
                .estadoDelivery(pe.edu.upeu.bomerp.ventas.entity.EstadoDelivery.valueOf(estado))
                .montoTotal(new BigDecimal(monto)).build();
    }

}
