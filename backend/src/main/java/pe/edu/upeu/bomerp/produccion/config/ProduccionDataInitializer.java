package pe.edu.upeu.bomerp.produccion.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.bomerp.produccion.dto.CentroTrabajoRequest;
import pe.edu.upeu.bomerp.produccion.dto.CentroTrabajoResponse;
import pe.edu.upeu.bomerp.produccion.dto.DetalleListaMaterialesRequest;
import pe.edu.upeu.bomerp.produccion.dto.ListaMaterialesRequest;
import pe.edu.upeu.bomerp.produccion.entity.OrdenProduccion;
import pe.edu.upeu.bomerp.produccion.repository.CentroTrabajoRepository;
import pe.edu.upeu.bomerp.produccion.repository.ListaMaterialesRepository;
import pe.edu.upeu.bomerp.produccion.repository.OrdenProduccionRepository;
import pe.edu.upeu.bomerp.produccion.service.CentroTrabajoService;
import pe.edu.upeu.bomerp.produccion.service.ListaMaterialesService;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Configuration
@Order(15)
@ConditionalOnProperty(name = "app.init.demo-data", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ProduccionDataInitializer implements CommandLineRunner {

    private final CentroTrabajoRepository centroTrabajoRepository;
    private final ListaMaterialesRepository listaMaterialesRepository;
    private final OrdenProduccionRepository ordenProduccionRepository;
    private final CentroTrabajoService centroTrabajoService;
    private final ListaMaterialesService listaMaterialesService;

    @Override
    @Transactional
    public void run(String... args) {
        if (centroTrabajoRepository.count() == 0) {
            log.info("Inicializando Centros de Trabajo de planta Agro Peruvian Foods...");
            CentroTrabajoResponse ctPrensado = centroTrabajoService.crear(CentroTrabajoRequest.builder()
                    .codigo("CT-PREN-01")
                    .nombre("Línea de Prensado en Frío Hidráulico")
                    .descripcion("Extracción mecánica a temperatura controlada (<38°C) para aceite de coco virgen")
                    .capacidad(new BigDecimal("250.0000"))
                    .unidadCapacidad("LITROS/HORA")
                    .costoHora(new BigDecimal("55.00"))
                    .activo(true)
                    .build());

            CentroTrabajoResponse ctCacao = centroTrabajoService.crear(CentroTrabajoRequest.builder()
                    .codigo("CT-CACAO-02")
                    .nombre("Línea de Selección y Clasificación de Cacao Chuncho")
                    .descripcion("Mesa densimétrica, zarandeo óptico y control de humedad para grano aromático")
                    .capacidad(new BigDecimal("400.0000"))
                    .unidadCapacidad("KG/HORA")
                    .costoHora(new BigDecimal("42.00"))
                    .activo(true)
                    .build());

            centroTrabajoService.crear(CentroTrabajoRequest.builder()
                    .codigo("CT-DESH-03")
                    .nombre("Túnel Deshidratador y Rallado de Coco Orgánico")
                    .descripcion("Rallado hebra larga y deshidratación sanitaria en acero inoxidable AISI 316")
                    .capacidad(new BigDecimal("180.0000"))
                    .unidadCapacidad("KG/HORA")
                    .costoHora(new BigDecimal("48.00"))
                    .activo(true)
                    .build());

            CentroTrabajoResponse ctEnvasado = centroTrabajoService.crear(CentroTrabajoRequest.builder()
                    .codigo("CT-ENV-04")
                    .nombre("Línea Automatizada de Envasado y Etiquetado Sanitario")
                    .descripcion("Dosificación volumétrica, tapado al vacío y codificación láser de lote FEFO")
                    .capacidad(new BigDecimal("600.0000"))
                    .unidadCapacidad("UNID/HORA")
                    .costoHora(new BigDecimal("38.00"))
                    .activo(true)
                    .build());

            // Vincular centros de trabajo a las órdenes semilla existentes si aún no tienen uno asignado
            for (OrdenProduccion orden : ordenProduccionRepository.findAll()) {
                if (orden.getCentroTrabajoId() == null) {
                    String prod = orden.getNombreProducto() != null ? orden.getNombreProducto().toLowerCase() : "";
                    if (prod.contains("cacao")) {
                        orden.setCentroTrabajoId(ctCacao.getId());
                        orden.setCentroTrabajoNombre(ctCacao.getNombre());
                    } else if (orden.getObservaciones() != null && orden.getObservaciones().toLowerCase().contains("embotellado")) {
                        orden.setCentroTrabajoId(ctEnvasado.getId());
                        orden.setCentroTrabajoNombre(ctEnvasado.getNombre());
                    } else {
                        orden.setCentroTrabajoId(ctPrensado.getId());
                        orden.setCentroTrabajoNombre(ctPrensado.getNombre());
                    }
                    ordenProduccionRepository.save(orden);
                }
            }
        }

        if (listaMaterialesRepository.count() == 0) {
            log.info("Inicializando Recetas Maestras BOM (Listas de Materiales) de planta...");

            listaMaterialesService.crear(ListaMaterialesRequest.builder()
                    .codigo("BOM-COCO-250ML")
                    .productoId(14L)
                    .nombreProducto("Aceite de coco Virgen 250 ml | Prensado en Frío")
                    .cantidadBase(new BigDecimal("100.0000"))
                    .observaciones("Receta estándar para lote base de 100 frascos de 250 ml")
                    .activo(true)
                    .detalles(List.of(
                            DetalleListaMaterialesRequest.builder()
                                    .insumoId(101L)
                                    .nombreInsumo("Copra Blanca Fresca Prensable Orgánica")
                                    .cantidadRequerida(new BigDecimal("45.0000"))
                                    .unidadMedida("KG")
                                    .build(),
                            DetalleListaMaterialesRequest.builder()
                                    .insumoId(102L)
                                    .nombreInsumo("Frasco de Vidrio Grado Alimentario 250ml")
                                    .cantidadRequerida(new BigDecimal("100.0000"))
                                    .unidadMedida("UNID")
                                    .build(),
                            DetalleListaMaterialesRequest.builder()
                                    .insumoId(103L)
                                    .nombreInsumo("Tapa Twist-Off + Precinto Termocontraíble")
                                    .cantidadRequerida(new BigDecimal("100.0000"))
                                    .unidadMedida("UNID")
                                    .build()
                    ))
                    .build());

            listaMaterialesService.crear(ListaMaterialesRequest.builder()
                    .codigo("BOM-COCO-1L")
                    .productoId(12L)
                    .nombreProducto("Aceite de Coco Virgen 1 L | Prensado en frío")
                    .cantidadBase(new BigDecimal("100.0000"))
                    .observaciones("Receta estándar para lote base de 100 envases de 1 litro")
                    .activo(true)
                    .detalles(List.of(
                            DetalleListaMaterialesRequest.builder()
                                    .insumoId(101L)
                                    .nombreInsumo("Copra Blanca Fresca Prensable Orgánica")
                                    .cantidadRequerida(new BigDecimal("180.0000"))
                                    .unidadMedida("KG")
                                    .build(),
                            DetalleListaMaterialesRequest.builder()
                                    .insumoId(104L)
                                    .nombreInsumo("Frasco de Vidrio Grado Alimentario 1L")
                                    .cantidadRequerida(new BigDecimal("100.0000"))
                                    .unidadMedida("UNID")
                                    .build(),
                            DetalleListaMaterialesRequest.builder()
                                    .insumoId(105L)
                                    .nombreInsumo("Etiqueta Metalizada Sanitaria + Sello Orgánico")
                                    .cantidadRequerida(new BigDecimal("100.0000"))
                                    .unidadMedida("UNID")
                                    .build()
                    ))
                    .build());

            listaMaterialesService.crear(ListaMaterialesRequest.builder()
                    .codigo("BOM-COCO-4L")
                    .productoId(1L)
                    .nombreProducto("Aceite de Coco Virgen | Balde de 4L")
                    .cantidadBase(new BigDecimal("50.0000"))
                    .observaciones("Formato institucional HORECA base 50 baldes de 4L")
                    .activo(true)
                    .detalles(List.of(
                            DetalleListaMaterialesRequest.builder()
                                    .insumoId(101L)
                                    .nombreInsumo("Copra Blanca Fresca Prensable Orgánica")
                                    .cantidadRequerida(new BigDecimal("360.0000"))
                                    .unidadMedida("KG")
                                    .build(),
                            DetalleListaMaterialesRequest.builder()
                                    .insumoId(106L)
                                    .nombreInsumo("Balde HDPE Grado Alimentario 4L con Asa y Precinto")
                                    .cantidadRequerida(new BigDecimal("50.0000"))
                                    .unidadMedida("UNID")
                                    .build()
                    ))
                    .build());

            listaMaterialesService.crear(ListaMaterialesRequest.builder()
                    .codigo("BOM-CACAO-1KG")
                    .productoId(6L)
                    .nombreProducto("Cacao Grano Entero | Sin Tostar | Aroma Intenso 1kg")
                    .cantidadBase(new BigDecimal("100.0000"))
                    .observaciones("Selección de grano entero aromático con 5% de merma de clasificación")
                    .activo(true)
                    .detalles(List.of(
                            DetalleListaMaterialesRequest.builder()
                                    .insumoId(107L)
                                    .nombreInsumo("Cacao Nativo Chuncho Fermentado Selección")
                                    .cantidadRequerida(new BigDecimal("105.0000"))
                                    .unidadMedida("KG")
                                    .build(),
                            DetalleListaMaterialesRequest.builder()
                                    .insumoId(108L)
                                    .nombreInsumo("Bolsa Bilaminada Kraft con Válvula Desgasificadora 1kg")
                                    .cantidadRequerida(new BigDecimal("100.0000"))
                                    .unidadMedida("UNID")
                                    .build()
                    ))
                    .build());

            listaMaterialesService.crear(ListaMaterialesRequest.builder()
                    .codigo("BOM-RALLADO-500G")
                    .productoId(4L)
                    .nombreProducto("Coco Rallado Doypack 500gr. | Hebra Larga")
                    .cantidadBase(new BigDecimal("100.0000"))
                    .observaciones("Empacado al nitrógeno de coco deshidratado hebra larga")
                    .activo(true)
                    .detalles(List.of(
                            DetalleListaMaterialesRequest.builder()
                                    .insumoId(109L)
                                    .nombreInsumo("Pulpa Fresca de Coco Deshidratada Hebra Larga")
                                    .cantidadRequerida(new BigDecimal("52.5000"))
                                    .unidadMedida("KG")
                                    .build(),
                            DetalleListaMaterialesRequest.builder()
                                    .insumoId(110L)
                                    .nombreInsumo("Bolsa Doypack Zipper Barrera UV 500g")
                                    .cantidadRequerida(new BigDecimal("100.0000"))
                                    .unidadMedida("UNID")
                                    .build()
                    ))
                    .build());
        }
    }
}

