package pe.edu.upeu.bomerp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.bomerp.exception.BusinessConflictException;
import pe.edu.upeu.bomerp.inventario.dto.AlmacenRequest;
import pe.edu.upeu.bomerp.inventario.dto.AlmacenResponse;
import pe.edu.upeu.bomerp.inventario.dto.StockCriticoDto;
import pe.edu.upeu.bomerp.inventario.service.AlmacenService;
import pe.edu.upeu.bomerp.inventario.service.InventarioService;
import pe.edu.upeu.bomerp.produccion.dto.CentroTrabajoRequest;
import pe.edu.upeu.bomerp.produccion.dto.CentroTrabajoResponse;
import pe.edu.upeu.bomerp.produccion.dto.DetalleListaMaterialesRequest;
import pe.edu.upeu.bomerp.produccion.dto.ListaMaterialesRequest;
import pe.edu.upeu.bomerp.produccion.dto.ListaMaterialesResponse;
import pe.edu.upeu.bomerp.produccion.dto.OrdenProduccionResponse;
import pe.edu.upeu.bomerp.produccion.service.CentroTrabajoService;
import pe.edu.upeu.bomerp.produccion.service.ListaMaterialesService;
import pe.edu.upeu.bomerp.produccion.service.ProduccionService;
import pe.edu.upeu.bomerp.produccion.service.ReporteOrdenPdfService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProduccionEInventarioGdmerpIntegrationTest {

    @Autowired
    private ReporteOrdenPdfService reporteOrdenPdfService;

    @Autowired
    private ProduccionService produccionService;

    @Autowired
    private ListaMaterialesService listaMaterialesService;

    @Autowired
    private CentroTrabajoService centroTrabajoService;

    @Autowired
    private AlmacenService almacenService;

    @Autowired
    private InventarioService inventarioService;

    @Test
    @DisplayName("Debe generar un documento PDF A4 válido con OpenPDF para una Orden de Producción existente")
    void generarReportePdf_debeRetornarBytesPdfValidos() {
        List<OrdenProduccionResponse> ordenes = produccionService.listarOrdenes();
        assertFalse(ordenes.isEmpty(), "Deben existir órdenes semilla cargadas");

        Long ordenId = ordenes.get(0).getId();
        byte[] pdf = reporteOrdenPdfService.generarReportePdf(ordenId);

        assertNotNull(pdf);
        assertTrue(pdf.length > 500, "El PDF generado debe contener estructura binaria completa");
        String header = new String(pdf, 0, 4, StandardCharsets.US_ASCII);
        assertEquals("%PDF", header, "El archivo generado debe iniciar con cabecera estándar %PDF");
    }

    @Test
    @DisplayName("Debe gestionar Recetas Maestras BOM (Listas de Materiales) y consultar receta activa por producto")
    void listaMateriales_debeConsultarYRegistrarRecetasBom() {
        List<ListaMaterialesResponse> recetasIniciales = listaMaterialesService.listar();
        assertTrue(recetasIniciales.size() >= 5, "Deben inicializarse al menos 5 recetas maestras BOM");

        ListaMaterialesResponse recetaCoco250 = listaMaterialesService.obtenerActivaPorProducto(14L);
        assertNotNull(recetaCoco250);
        assertEquals("BOM-COCO-250ML", recetaCoco250.getCodigo());
        assertFalse(recetaCoco250.getDetalles().isEmpty());

        ListaMaterialesResponse creada = listaMaterialesService.crear(ListaMaterialesRequest.builder()
                .codigo("BOM-COCO-30ML-TEST")
                .productoId(16L)
                .nombreProducto("Aceite de coco Virgen 30 ml | Prensado en Frío")
                .cantidadBase(new BigDecimal("200.0000"))
                .observaciones("Receta de prueba unitaria integradora")
                .detalles(List.of(
                        DetalleListaMaterialesRequest.builder()
                                .insumoId(101L)
                                .nombreInsumo("Copra Blanca Orgánica")
                                .cantidadRequerida(new BigDecimal("14.5000"))
                                .unidadMedida("KG")
                                .build()
                ))
                .build());

        assertNotNull(creada.getId());
        assertEquals("BOM-COCO-30ML-TEST", creada.getCodigo());
        assertEquals(1, creada.getDetalles().size());
    }

    @Test
    @DisplayName("Debe gestionar Centros de Trabajo de planta con capacidad y costo por hora")
    void centroTrabajo_debeListarYCrearLineasDePlanta() {
        List<CentroTrabajoResponse> centros = centroTrabajoService.listar();
        assertTrue(centros.size() >= 4, "Deben existir los 4 centros de trabajo iniciales");

        CentroTrabajoResponse nuevo = centroTrabajoService.crear(CentroTrabajoRequest.builder()
                .codigo("CT-FILT-99")
                .nombre("Línea Microfiltración Tangencial")
                .descripcion("Filtración en frío de micras finas")
                .capacidad(new BigDecimal("320.0000"))
                .unidadCapacidad("LITROS/HORA")
                .costoHora(new BigDecimal("62.50"))
                .activo(true)
                .build());

        assertNotNull(nuevo.getId());
        assertEquals("CT-FILT-99", nuevo.getCodigo());
    }

    @Test
    @DisplayName("Debe gestionar Almacenes con control de capacidad máxima y calcular reporte de Stock Crítico")
    void almacenesYStockCritico_debeValidarOcupacionYAlertas() {
        List<AlmacenResponse> almacenes = almacenService.listar();
        assertTrue(almacenes.size() >= 4, "Deben existir los 4 almacenes físicos iniciales");

        assertThrows(BusinessConflictException.class, () -> almacenService.crear(AlmacenRequest.builder()
                .codigo("ALM-ERR-99")
                .nombre("Almacén Sobrecargado")
                .capacidadMaxima(new BigDecimal("100.00"))
                .ocupacionActual(new BigDecimal("150.00"))
                .build()));

        List<StockCriticoDto> alertas = inventarioService.obtenerReporteStockCritico(100, true);
        assertFalse(alertas.isEmpty(), "Con umbral de 100 unidades deben detectarse productos en nivel PREVENTIVO/CRITICO");
        assertTrue(alertas.stream().allMatch(a -> !"OPTIMO".equals(a.getNivelAlerta())));
    }
}

