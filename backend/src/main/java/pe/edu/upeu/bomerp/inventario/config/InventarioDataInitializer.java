package pe.edu.upeu.bomerp.inventario.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import pe.edu.upeu.bomerp.catalogo.producto.dto.ProductoResponse;
import pe.edu.upeu.bomerp.catalogo.producto.service.ProductoService;
import pe.edu.upeu.bomerp.inventario.dto.LoteCreateDto;
import pe.edu.upeu.bomerp.inventario.repository.LoteRepository;
import pe.edu.upeu.bomerp.inventario.service.InventarioService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Configuration
@Order(10)
@ConditionalOnProperty(name = "app.init.demo-data", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class InventarioDataInitializer implements CommandLineRunner {

    private final LoteRepository loteRepository;
    private final ProductoService productoService;
    private final InventarioService inventarioService;

    @Override
    public void run(String... args) {
        if (loteRepository.count() == 0) {
            log.info("Inicializando lotes de inventario FEFO para Agro Peruvian Foods...");

            List<ProductoResponse> productos = productoService.listar();
            if (productos.isEmpty()) {
                log.warn("No hay productos en catálogo para vincular lotes demo.");
                return;
            }

            LocalDate hoy = LocalDate.now();

            for (ProductoResponse p : productos) {
                String n = p.getNombre();
                if (n.contains("Aceite de Coco Virgen 460 ml")) {
                    // Lote 1: Próximo a vencer (18 días) -> Debe consumirse PRIMERO por FEFO
                    inventarioService.registrarLote(LoteCreateDto.builder()
                            .codigoLote("LOT-COCO460-2026-A1")
                            .productoId(p.getId())
                            .fechaFabricacion(hoy.minusMonths(5))
                            .fechaVencimiento(hoy.plusDays(18))
                            .stockInicial(new BigDecimal("20.00"))
                            .costoUnitario(new BigDecimal("21.00"))
                            .observaciones("Lote con vencimiento cercano. Prioridad 1 de despacho FEFO.")
                            .build());

                    // Lote 2: Fresco (vence en 8 meses) -> Debe consumirse DESPUÉS
                    inventarioService.registrarLote(LoteCreateDto.builder()
                            .codigoLote("LOT-COCO460-2026-B2")
                            .productoId(p.getId())
                            .fechaFabricacion(hoy.minusWeeks(2))
                            .fechaVencimiento(hoy.plusMonths(8))
                            .stockInicial(new BigDecimal("60.00"))
                            .costoUnitario(new BigDecimal("22.00"))
                            .observaciones("Lote fresco recién prensado en frío.")
                            .build());
                } else if (n.contains("Cacao Grano Entero") && n.contains("1kg")) {
                    // Lote 1: Preventivo (45 días)
                    inventarioService.registrarLote(LoteCreateDto.builder()
                            .codigoLote("LOT-CACAO1K-2026-01")
                            .productoId(p.getId())
                            .fechaFabricacion(hoy.minusMonths(4))
                            .fechaVencimiento(hoy.plusDays(45))
                            .stockInicial(new BigDecimal("30.00"))
                            .costoUnitario(new BigDecimal("16.50"))
                            .observaciones("Cacao grano entero 1kg lote 01 preventivo.")
                            .build());

                    // Lote 2: Óptimo (10 meses)
                    inventarioService.registrarLote(LoteCreateDto.builder()
                            .codigoLote("LOT-CACAO1K-2026-02")
                            .productoId(p.getId())
                            .fechaFabricacion(hoy.minusDays(5))
                            .fechaVencimiento(hoy.plusMonths(10))
                            .stockInicial(new BigDecimal("60.00"))
                            .costoUnitario(new BigDecimal("17.00"))
                            .observaciones("Cacao grano entero 1kg selección fresca La Convención.")
                            .build());
                } else if (n.contains("Coco Rallado Doypack 250g")) {
                    // Lote 1: Crítico (12 días)
                    inventarioService.registrarLote(LoteCreateDto.builder()
                            .codigoLote("LOT-COCORALL-2026-01")
                            .productoId(p.getId())
                            .fechaFabricacion(hoy.minusMonths(6))
                            .fechaVencimiento(hoy.plusDays(12))
                            .stockInicial(new BigDecimal("25.00"))
                            .costoUnitario(new BigDecimal("4.80"))
                            .observaciones("Coco rallado lote con menor vida útil. Prioridad FEFO.")
                            .build());

                    // Lote 2: Óptimo (7 meses)
                    inventarioService.registrarLote(LoteCreateDto.builder()
                            .codigoLote("LOT-COCORALL-2026-02")
                            .productoId(p.getId())
                            .fechaFabricacion(hoy.minusDays(10))
                            .fechaVencimiento(hoy.plusMonths(7))
                            .stockInicial(new BigDecimal("50.00"))
                            .costoUnitario(new BigDecimal("5.00"))
                            .observaciones("Coco rallado hebra larga lote estándar.")
                            .build());
                } else if (n.contains("Balde de 4L")) {
                    inventarioService.registrarLote(LoteCreateDto.builder()
                            .codigoLote("LOT-BALDE4L-2026-01")
                            .productoId(p.getId())
                            .fechaFabricacion(hoy.minusMonths(1))
                            .fechaVencimiento(hoy.plusMonths(11))
                            .stockInicial(new BigDecimal("40.00"))
                            .costoUnitario(new BigDecimal("120.00"))
                            .observaciones("Lote institucional HORECA 4L.")
                            .build());
                } else if (n.contains("Aceite de coco Virgen 250 ml")) {
                    inventarioService.registrarLote(LoteCreateDto.builder()
                            .codigoLote("LOT-COCO250-2026-01")
                            .productoId(p.getId())
                            .fechaFabricacion(hoy.minusMonths(2))
                            .fechaVencimiento(hoy.plusDays(50))
                            .stockInicial(new BigDecimal("45.00"))
                            .costoUnitario(new BigDecimal("11.50"))
                            .observaciones("Lote retail 250ml en ventana preventiva.")
                            .build());
                }
            }

            log.info("Lotes de inventario FEFO inicializados satisfactoriamente.");
        }
    }
}
