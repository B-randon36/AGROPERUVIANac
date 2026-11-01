package pe.edu.upeu.bomerp.produccion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.bomerp.catalogo.producto.service.ProductoService;
import pe.edu.upeu.bomerp.inventario.service.InventarioService;
import pe.edu.upeu.bomerp.produccion.dto.*;
import pe.edu.upeu.bomerp.produccion.entity.DetalleConsumoInsumo;
import pe.edu.upeu.bomerp.produccion.entity.EstadoOrdenProduccion;
import pe.edu.upeu.bomerp.produccion.entity.LoteProducto;
import pe.edu.upeu.bomerp.produccion.entity.OrdenProduccion;
import pe.edu.upeu.bomerp.produccion.repository.LoteProductoRepository;
import pe.edu.upeu.bomerp.produccion.repository.OrdenProduccionRepository;
import pe.edu.upeu.bomerp.produccion.service.ProduccionServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProduccionServiceTest {

    @Mock
    private OrdenProduccionRepository ordenProduccionRepository;

    @Mock
    private LoteProductoRepository loteProductoRepository;

    @Mock
    private ProductoService productoService;

    @Mock
    private InventarioService inventarioService;

    @InjectMocks
    private ProduccionServiceImpl produccionService;

    private OrdenProduccion ordenPlanificada;

    @BeforeEach
    void setUp() {
        DetalleConsumoInsumo insumoBom = DetalleConsumoInsumo.builder()
                .id(10L)
                .insumoId(103L)
                .nombreInsumo("Copra Blanca Deshidratada Grado Alimentario")
                .cantidadRequerida(new BigDecimal("220.00"))
                .cantidadConsumida(new BigDecimal("225.00"))
                .unidadMedida("KG")
                .build();

        ordenPlanificada = OrdenProduccion.builder()
                .id(1L)
                .codigoOrden("OP-20261031-001")
                .productoId(14L)
                .nombreProducto("Aceite de coco Virgen 250 ml | Prensado en Frío")
                .cantidadProgramada(500)
                .fechaInicio(LocalDateTime.now())
                .estado(EstadoOrdenProduccion.PLANIFICADA)
                .insumosConsumidos(new ArrayList<>(List.of(insumoBom)))
                .build();
    }

    @Test
    @DisplayName("Debe planificar una orden de producción con sus insumos BOM")
    void crearOrden_debeCrearEnEstadoPlanificada() {
        DetalleConsumoRequest insumo = new DetalleConsumoRequest();
        insumo.setInsumoId(106L);
        insumo.setNombreInsumo("Frasco de Vidrio 250ml");
        insumo.setCantidadRequerida(new BigDecimal("500.000"));
        insumo.setCantidadConsumida(new BigDecimal("500.000"));
        insumo.setUnidadMedida("UNID");

        OrdenProduccionRequest req = new OrdenProduccionRequest();
        req.setProductoId(14L);
        req.setNombreProducto("Aceite de coco Virgen 250 ml | Prensado en Frío");
        req.setCantidadProgramada(500);
        req.setInsumos(List.of(insumo));

        when(ordenProduccionRepository.save(any(OrdenProduccion.class))).thenAnswer(i -> {
            OrdenProduccion o = i.getArgument(0);
            o.setId(2L);
            return o;
        });

        OrdenProduccionResponse resp = produccionService.crearOrden(req);

        assertNotNull(resp);
        assertEquals(2L, resp.getId());
        assertEquals(EstadoOrdenProduccion.PLANIFICADA, resp.getEstado());
        assertEquals(500, resp.getCantidadProgramada());
        verify(ordenProduccionRepository, times(1)).save(any(OrdenProduccion.class));
    }

    @Test
    @DisplayName("Debe iniciar una orden planificada cambiando su estado a EN_PROCESO")
    void iniciarOrden_debeCambiarEstadoAEnProceso() {
        when(ordenProduccionRepository.findById(1L)).thenReturn(Optional.of(ordenPlanificada));
        when(ordenProduccionRepository.save(any(OrdenProduccion.class))).thenAnswer(i -> i.getArgument(0));

        OrdenProduccionResponse resp = produccionService.iniciarOrden(1L);

        assertNotNull(resp);
        assertEquals(EstadoOrdenProduccion.EN_PROCESO, resp.getEstado());
        verify(ordenProduccionRepository, times(1)).save(ordenPlanificada);
    }

    @Test
    @DisplayName("Debe completar la orden descontando insumos BOM en Kardex y unificando Lote FEFO en Inventario")
    void completarOrden_debeActualizarStockDescontarInsumosYUnificarLoteFEFO() {
        ordenPlanificada.setEstado(EstadoOrdenProduccion.EN_PROCESO);
        LocalDate vencimiento = LocalDate.now().plusYears(1);

        CompletarOrdenRequest req = new CompletarOrdenRequest();
        req.setCantidadProducida(480);
        req.setFechaVencimiento(vencimiento);

        when(ordenProduccionRepository.findById(1L)).thenReturn(Optional.of(ordenPlanificada));
        doNothing().when(productoService).aumentarStock(14L, 480);
        when(loteProductoRepository.save(any(LoteProducto.class))).thenAnswer(i -> i.getArgument(0));
        when(ordenProduccionRepository.save(any(OrdenProduccion.class))).thenAnswer(i -> i.getArgument(0));

        OrdenProduccionResponse resp = produccionService.completarOrden(1L, req);

        assertNotNull(resp);
        assertEquals(EstadoOrdenProduccion.COMPLETADA, resp.getEstado());
        assertEquals(480, resp.getCantidadProducida());

        verify(inventarioService, times(1)).registrarConsumoInsumoBom(
                eq(103L),
                eq("Copra Blanca Deshidratada Grado Alimentario"),
                eq(new BigDecimal("225.00")),
                eq("KG"),
                eq("OP-20261031-001")
        );
        verify(productoService, times(1)).aumentarStock(14L, 480);
        verify(inventarioService, times(1)).registrarIngresoProduccion(
                eq(14L),
                eq("Aceite de coco Virgen 250 ml | Prensado en Frío"),
                any(),
                eq(480),
                eq(vencimiento),
                eq("OP-20261031-001")
        );
        verify(loteProductoRepository, times(1)).save(any(LoteProducto.class));
        verify(ordenProduccionRepository, times(1)).save(ordenPlanificada);
    }

    @Test
    @DisplayName("Debe cancelar una orden de producción planificada")
    void cancelarOrden_ordenPlanificada_debeCambiarEstadoACancelada() {
        when(ordenProduccionRepository.findById(1L)).thenReturn(Optional.of(ordenPlanificada));
        when(ordenProduccionRepository.save(any(OrdenProduccion.class))).thenAnswer(i -> i.getArgument(0));

        OrdenProduccionResponse resp = produccionService.cancelarOrden(1L);

        assertNotNull(resp);
        assertEquals(EstadoOrdenProduccion.CANCELADA, resp.getEstado());
        verify(ordenProduccionRepository, times(1)).save(ordenPlanificada);
    }

    @Test
    @DisplayName("Debe generar reporte agregado de eficiencia de producción")
    void reporte_debeCalcularMetricasYEficiencia() {
        ordenPlanificada.setCantidadProducida(450);
        when(ordenProduccionRepository.buscar(null, null, null)).thenReturn(List.of(ordenPlanificada));

        ProduccionReporte rep = produccionService.reporte(null, null, null);

        assertNotNull(rep);
        assertEquals(1L, rep.getTotalOrdenes());
        assertEquals(500L, rep.getTotalCantidadProgramada());
        assertEquals(450L, rep.getTotalCantidadProducida());
        assertEquals(90.0, rep.getPorcentajeEficiencia());
    }
}
