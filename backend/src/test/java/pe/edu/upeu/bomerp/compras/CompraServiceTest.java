package pe.edu.upeu.bomerp.compras;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.bomerp.compras.dto.*;
import pe.edu.upeu.bomerp.compras.entity.Compra;
import pe.edu.upeu.bomerp.compras.entity.EstadoCompra;
import pe.edu.upeu.bomerp.compras.entity.PagoCompra;
import pe.edu.upeu.bomerp.compras.entity.Proveedor;
import pe.edu.upeu.bomerp.compras.repository.CompraRepository;
import pe.edu.upeu.bomerp.compras.repository.InsumoRepository;
import pe.edu.upeu.bomerp.compras.repository.PagoCompraRepository;
import pe.edu.upeu.bomerp.compras.repository.ProveedorRepository;
import pe.edu.upeu.bomerp.compras.service.CompraServiceImpl;
import pe.edu.upeu.bomerp.exception.BusinessConflictException;
import pe.edu.upeu.bomerp.finanzas.api.FinanzasApi;
import pe.edu.upeu.bomerp.inventario.service.InventarioService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompraServiceTest {

    @Mock
    private CompraRepository compraRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private PagoCompraRepository pagoCompraRepository;

    @Mock
    private InsumoRepository insumoRepository;

    @Mock
    private InventarioService inventarioService;

    @Mock
    private FinanzasApi finanzasApi;

    @InjectMocks
    private CompraServiceImpl compraService;

    private Proveedor proveedor;
    private Compra compraRegistrada;

    @BeforeEach
    void setUp() {
        proveedor = Proveedor.builder()
                .id(1L)
                .ruc("20601234567")
                .razonSocial("Cooperativa Agraria Productores de Ayacucho")
                .email("ventas@coopayacucho.pe")
                .build();

        compraRegistrada = Compra.builder()
                .id(10L)
                .numeroComprobante("F001-0001")
                .proveedor(proveedor)
                .fechaEmision(LocalDate.now())
                .estado(EstadoCompra.REGISTRADA)
                .subtotal(new BigDecimal("820.00"))
                .igv(new BigDecimal("180.00"))
                .total(new BigDecimal("1000.00"))
                .saldoPendiente(new BigDecimal("1000.00"))
                .detalles(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Debe crear proveedor correctamente o rechazar si RUC ya existe")
    void crearProveedor_conRucDuplicado_debeLanzarConflicto() {
        ProveedorRequest req = new ProveedorRequest();
        req.setRuc("20601234567");
        req.setRazonSocial("Otra Cooperativa");

        when(proveedorRepository.findByRuc("20601234567")).thenReturn(Optional.of(proveedor));

        assertThrows(BusinessConflictException.class, () -> compraService.crearProveedor(req));
        verify(proveedorRepository, never()).save(any(Proveedor.class));
    }

    @Test
    @DisplayName("Debe registrar compra cabecera-detalle calculando totales y saldo pendiente")
    void registrarCompra_debeCalcularTotalesYSaldoPendiente() {
        DetalleCompraRequest item = new DetalleCompraRequest();
        item.setInsumoId(9L);
        item.setNombreInsumo("Bolsa Bilaminada al Vacío");
        item.setCantidad(new BigDecimal("100.000"));
        item.setPrecioUnitario(new BigDecimal("10.00"));

        CompraRequest req = new CompraRequest();
        req.setNumeroComprobante("F001-0005");
        req.setProveedorId(1L);
        req.setFechaEmision(LocalDate.now());
        req.setDetalles(List.of(item));

        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(compraRepository.save(any(Compra.class))).thenAnswer(i -> {
            Compra c = i.getArgument(0);
            c.setId(11L);
            return c;
        });

        CompraResponse resp = compraService.registrarCompra(req);

        assertNotNull(resp);
        assertEquals(new BigDecimal("1180.00"), resp.getTotal());
        assertEquals(new BigDecimal("1180.00"), resp.getSaldoPendiente());
        assertEquals(new BigDecimal("1000.00"), resp.getSubtotal());
        assertEquals(new BigDecimal("180.00"), resp.getIgv());
        assertEquals(EstadoCompra.REGISTRADA, resp.getEstado());
        verify(compraRepository, times(1)).save(any(Compra.class));
    }

    @Test
    @DisplayName("Debe amortizar pago de compra y cambiar estado a PAGADA cuando el saldo llega a cero")
    void amortizarPago_pagoTotal_debeCambiarEstadoAPagada() {
        AmortizarPagoRequest amortizacion = new AmortizarPagoRequest();
        amortizacion.setMontoPago(new BigDecimal("1000.00"));

        when(compraRepository.findById(10L)).thenReturn(Optional.of(compraRegistrada));
        when(compraRepository.save(any(Compra.class))).thenAnswer(i -> i.getArgument(0));
        when(pagoCompraRepository.save(any(PagoCompra.class))).thenAnswer(i -> i.getArgument(0));

        CompraResponse resp = compraService.amortizarPago(10L, amortizacion);

        assertNotNull(resp);
        assertEquals(new BigDecimal("0.00"), resp.getSaldoPendiente());
        assertEquals(EstadoCompra.PAGADA, resp.getEstado());
        verify(compraRepository, times(1)).save(compraRegistrada);
        verify(pagoCompraRepository, times(1)).save(any(PagoCompra.class));
    }

    @Test
    @DisplayName("Debe anular compra y liquidar saldo pendiente")
    void anularCompra_debeCambiarEstadoAAnuladaYLiquidarSaldo() {
        when(compraRepository.findById(10L)).thenReturn(Optional.of(compraRegistrada));
        when(compraRepository.save(any(Compra.class))).thenAnswer(i -> i.getArgument(0));

        CompraResponse resp = compraService.anularCompra(10L);

        assertNotNull(resp);
        assertEquals(EstadoCompra.ANULADA, resp.getEstado());
        assertEquals(BigDecimal.ZERO, resp.getSaldoPendiente());
        verify(compraRepository, times(1)).save(compraRegistrada);
    }

    @Test
    @DisplayName("Debe generar reporte agregado de compras y cuentas por pagar")
    void reporte_debeCalcularMetricasDeCompras() {
        when(compraRepository.buscar(null, null, null)).thenReturn(List.of(compraRegistrada));

        CompraReporte rep = compraService.reporte(null, null, null);

        assertNotNull(rep);
        assertEquals(1L, rep.getTotalCompras());
        assertEquals(new BigDecimal("1000.00"), rep.getMontoTotalComprado());
        assertEquals(new BigDecimal("1000.00"), rep.getSaldoPendienteTotal());
        assertEquals(new BigDecimal("0.00"), rep.getMontoTotalPagado());
    }
}
