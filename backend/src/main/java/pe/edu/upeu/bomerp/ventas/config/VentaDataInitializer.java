package pe.edu.upeu.bomerp.ventas.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.bomerp.catalogo.producto.entity.Producto;
import pe.edu.upeu.bomerp.catalogo.producto.repository.ProductoRepository;
import pe.edu.upeu.bomerp.finanzas.entity.EstadoCaja;
import pe.edu.upeu.bomerp.finanzas.entity.SesionCaja;
import pe.edu.upeu.bomerp.finanzas.repository.SesionCajaRepository;
import pe.edu.upeu.bomerp.inventario.entity.Lote;
import pe.edu.upeu.bomerp.inventario.entity.MovimientoKardex;
import pe.edu.upeu.bomerp.inventario.entity.TipoMovimientoKardex;
import pe.edu.upeu.bomerp.inventario.repository.LoteRepository;
import pe.edu.upeu.bomerp.inventario.repository.MovimientoKardexRepository;
import pe.edu.upeu.bomerp.ventas.entity.DetalleVenta;
import pe.edu.upeu.bomerp.ventas.entity.EstadoVenta;
import pe.edu.upeu.bomerp.ventas.entity.MetodoPagoVenta;
import pe.edu.upeu.bomerp.ventas.entity.PedidoDelivery;
import pe.edu.upeu.bomerp.ventas.entity.Venta;
import pe.edu.upeu.bomerp.ventas.repository.PedidoDeliveryRepository;
import pe.edu.upeu.bomerp.ventas.repository.VentaRepository;
import pe.edu.upeu.bomerp.ventas.service.AuditoriaService;
import pe.edu.upeu.bomerp.ventas.service.NumeroTicketService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Inicializador de datos semilla de prueba para el módulo de Ventas POS y Omnicanalidad.
 * Provee ventas realistas con variedad de métodos de pago, estados, fechas, clientes y enlaces
 * a sesiones de caja, pedidos de delivery y auditoría para que el usuario pueda probar el ERP completamente.
 */
@Slf4j
@Configuration
@Order(30)
@ConditionalOnProperty(name = "app.init.demo-data", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class VentaDataInitializer implements CommandLineRunner {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final LoteRepository loteRepository;
    private final MovimientoKardexRepository kardexRepository;
    private final SesionCajaRepository sesionCajaRepository;
    private final PedidoDeliveryRepository deliveryRepository;
    private final AuditoriaService auditoriaService;
    private final NumeroTicketService numeroTicketService;

    // UUIDs nominales de Keycloak del realm 'bomerp' para pruebas realistas con ABAC
    private static final String ID_VENDEDOR1 = "5f624b7a-e931-4923-842d-74623e510bba"; // vendedor1
    private static final String ID_ADMIN = "383b8059-6a43-4c78-a9c1-b7594539b98d";     // admin
    private static final String ID_SUPERVISOR = "c568d3f9-7e54-4683-b5c1-6245709e090e"; // supervisor

    @Override
    @Transactional
    public void run(String... args) {
        if (ventaRepository.count() > 0) {
            return;
        }

        log.info("Inicializando datos de prueba de Ventas POS y Omnicanalidad para Agro Peruvian Foods...");

        Map<Long, Producto> productosPorId = productoRepository.findAll().stream()
                .collect(Collectors.toMap(Producto::getId, Function.identity()));

        if (productosPorId.isEmpty()) {
            log.warn("No se encontraron productos en catálogo para generar ventas semilla.");
            return;
        }

        List<Lote> lotes = loteRepository.findAll();
        Map<Long, List<Lote>> lotesPorProducto = lotes.stream()
                .collect(Collectors.groupingBy(Lote::getProductoId));

        Long sesionCajaActivaId = sesionCajaRepository.findFirstByEstadoOrderByFechaAperturaDesc(EstadoCaja.ABIERTA)
                .map(SesionCaja::getId)
                .orElse(null);

        Long sesionCajaCerradaId = sesionCajaRepository.findFirstByEstadoOrderByFechaAperturaDesc(EstadoCaja.CERRADA)
                .map(SesionCaja::getId)
                .orElse(null);

        LocalDateTime ahora = LocalDateTime.now();

        // 1. Venta POS Hoy - Efectivo (Mostrador en sesión abierta)
        Venta v1 = crearVenta(
                ahora.minusHours(2),
                EstadoVenta.REGISTRADA,
                MetodoPagoVenta.EFECTIVO,
                "María Elena Flores Ramos",
                ID_VENDEDOR1,
                sesionCajaActivaId,
                "DEMO-OP-001",
                List.of(
                        new ItemSeed(13L, 2), // Aceite de Coco 460ml (2 x S/ 35 = S/ 70)
                        new ItemSeed(6L, 1)   // Cacao Grano Entero 1kg (1 x S/ 28 = S/ 28)
                ),
                productosPorId,
                lotesPorProducto
        );

        // 2. Venta POS Hoy - Yape
        Venta v2 = crearVenta(
                ahora.minusHours(3),
                EstadoVenta.REGISTRADA,
                MetodoPagoVenta.YAPE,
                "Juan Carlos Quispe Huamán",
                ID_VENDEDOR1,
                sesionCajaActivaId,
                "DEMO-OP-002",
                List.of(
                        new ItemSeed(1L, 1), // Balde 4L Aceite de Coco (1 x S/ 200 = S/ 200)
                        new ItemSeed(5L, 2)  // Coco Rallado 250gr (2 x S/ 8 = S/ 16)
                ),
                productosPorId,
                lotesPorProducto
        );

        // 3. Venta Ayer - Transferencia (Vinculada a Delivery DEL-2026-001)
        Venta v3 = crearVenta(
                ahora.minusDays(1).withHour(11).withMinute(30),
                EstadoVenta.REGISTRADA,
                MetodoPagoVenta.TRANSFERENCIA,
                "EcoMarket Orgánico Miraflores",
                ID_ADMIN,
                sesionCajaCerradaId,
                "DEMO-OP-003",
                List.of(
                        new ItemSeed(13L, 4), // 4 x S/ 35 = S/ 140
                        new ItemSeed(6L, 3),  // 3 x S/ 28 = S/ 84
                        new ItemSeed(4L, 2),  // 2 x S/ 16 = S/ 32
                        new ItemSeed(14L, 3)  // 3 x S/ 20 = S/ 60
                ),
                productosPorId,
                lotesPorProducto
        );
        deliveryRepository.findByCodigoPedido("DEL-2026-001").ifPresent(del -> {
            del.setVentaId(v3.getId());
            deliveryRepository.save(del);
        });

        // 4. Venta Ayer - Crédito Comercial a 30 días
        Venta v4 = crearVenta(
                ahora.minusDays(1).withHour(15).withMinute(45),
                EstadoVenta.REGISTRADA,
                MetodoPagoVenta.CREDITO,
                "Panadería Artesanal Barranco SAC",
                ID_ADMIN,
                null,
                "DEMO-OP-004",
                List.of(
                        new ItemSeed(2L, 1), // Balde 20L Aceite de Coco (1 x S/ 730 = S/ 730)
                        new ItemSeed(1L, 1)  // Balde 4L Aceite de Coco (1 x S/ 200 = S/ 200)
                ),
                productosPorId,
                lotesPorProducto
        );

        // 5. Venta Hace 2 días - Tarjeta con Solicitud de Anulación pendiente (para probar aprobación/rechazo)
        Venta v5 = crearVenta(
                ahora.minusDays(2).withHour(10).withMinute(15),
                EstadoVenta.SOLICITADA_ANULACION,
                MetodoPagoVenta.TARJETA,
                "BioTienda Saludable San Isidro",
                ID_VENDEDOR1,
                sesionCajaCerradaId,
                "DEMO-OP-005",
                List.of(
                        new ItemSeed(14L, 3), // 3 x S/ 20 = S/ 60
                        new ItemSeed(5L, 2)   // 2 x S/ 8 = S/ 16
                ),
                productosPorId,
                lotesPorProducto
        );
        auditoriaService.registrar(ID_VENDEDOR1, "SOLICITAR_ANULACION", "VENTA", v5.getId(),
                "ticket=" + v5.getNumeroTicket() + "; motivo=Cliente solicitó cambio de comprobante a factura con RUC");

        // 6. Venta Hace 3 días - Efectivo Anulada (para probar filtros y badges de anulada)
        Venta v6 = crearVenta(
                ahora.minusDays(3).withHour(16).withMinute(0),
                EstadoVenta.ANULADA,
                MetodoPagoVenta.EFECTIVO,
                "Carlos Mendoza Alarcón",
                ID_VENDEDOR1,
                sesionCajaCerradaId,
                "DEMO-OP-006",
                List.of(
                        new ItemSeed(6L, 1) // 1 x S/ 28 = S/ 28
                ),
                productosPorId,
                lotesPorProducto
        );
        auditoriaService.registrar(ID_ADMIN, "ANULAR", "VENTA", v6.getId(),
                "ticket=" + v6.getNumeroTicket() + "; motivo=Error en medio de pago seleccionado");

        // 7. Venta Hace 4 días - Plin
        Venta v7 = crearVenta(
                ahora.minusDays(4).withHour(12).withMinute(20),
                EstadoVenta.REGISTRADA,
                MetodoPagoVenta.PLIN,
                "Lucía Paredes Benítez",
                ID_VENDEDOR1,
                sesionCajaCerradaId,
                "DEMO-OP-007",
                List.of(
                        new ItemSeed(15L, 4), // 4 x S/ 12 = S/ 48
                        new ItemSeed(13L, 1)  // 1 x S/ 35 = S/ 35
                ),
                productosPorId,
                lotesPorProducto
        );

        // 8. Venta Hace 5 días - Efectivo
        Venta v8 = crearVenta(
                ahora.minusDays(5).withHour(9).withMinute(40),
                EstadoVenta.REGISTRADA,
                MetodoPagoVenta.EFECTIVO,
                "Restaurante Vegano Verde Vital",
                ID_SUPERVISOR,
                sesionCajaCerradaId,
                "DEMO-OP-008",
                List.of(
                        new ItemSeed(1L, 1), // 1 x S/ 200 = S/ 200
                        new ItemSeed(6L, 4)  // 4 x S/ 28 = S/ 112
                ),
                productosPorId,
                lotesPorProducto
        );

        // 9. Venta Hace 6 días - Transferencia (Vinculada a Delivery DEL-2026-004)
        Venta v9 = crearVenta(
                ahora.minusDays(6).withHour(14).withMinute(10),
                EstadoVenta.REGISTRADA,
                MetodoPagoVenta.TRANSFERENCIA,
                "Cafetería Cacao Nativo San Borja",
                ID_ADMIN,
                sesionCajaCerradaId,
                "DEMO-OP-009",
                List.of(
                        new ItemSeed(6L, 6),  // 6 x S/ 28 = S/ 168
                        new ItemSeed(15L, 1)  // 1 x S/ 12 = S/ 12
                ),
                productosPorId,
                lotesPorProducto
        );
        deliveryRepository.findByCodigoPedido("DEL-2026-004").ifPresent(del -> {
            del.setVentaId(v9.getId());
            deliveryRepository.save(del);
        });

        // 10. Venta Hoy mañana - Crédito Mayorista
        Venta v10 = crearVenta(
                ahora.minusHours(4),
                EstadoVenta.REGISTRADA,
                MetodoPagoVenta.CREDITO,
                "Distribuidora Orgánica Los Olivos EIRL",
                ID_VENDEDOR1,
                null,
                "DEMO-OP-010",
                List.of(
                        new ItemSeed(12L, 10) // 10 x S/ 65 = S/ 650
                ),
                productosPorId,
                lotesPorProducto
        );

        log.info("Ventas de prueba cargadas exitosamente (10 ventas registradas con tickets TKT-S-0000000001 a TKT-S-0000000010).");
    }

    private Venta crearVenta(
            LocalDateTime fecha,
            EstadoVenta estado,
            MetodoPagoVenta metodoPago,
            String cliente,
            String vendedorId,
            Long sesionCajaId,
            String operacionId,
            List<ItemSeed> items,
            Map<Long, Producto> productos,
            Map<Long, List<Lote>> lotesPorProducto) {

        // Obtener ticket correlativo oficial avanzando la secuencia atómica nativa
        String ticket = numeroTicketService.siguiente();

        Venta venta = Venta.builder()
                .numeroTicket(ticket)
                .fecha(fecha)
                .estado(estado)
                .metodoPago(metodoPago)
                .clienteNombre(cliente)
                .vendedorId(vendedorId)
                .sesionCajaId(sesionCajaId)
                .operacionId(operacionId)
                .detalles(new ArrayList<>())
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (ItemSeed item : items) {
            Producto prod = productos.get(item.productoId);
            if (prod == null) {
                continue;
            }

            BigDecimal precio = prod.getPrecio() != null ? prod.getPrecio() : BigDecimal.TEN;
            BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(item.cantidad));

            Long loteId = null;
            Lote loteEncontrado = null;
            List<Lote> lotesProd = lotesPorProducto.get(prod.getId());
            if (lotesProd != null && !lotesProd.isEmpty()) {
                loteEncontrado = lotesProd.get(0);
                loteId = loteEncontrado.getId();
            }

            DetalleVenta det = DetalleVenta.builder()
                    .venta(venta)
                    .productoId(prod.getId())
                    .loteId(loteId)
                    .nombreProducto(prod.getNombre())
                    .precioUnitario(precio)
                    .cantidad(item.cantidad)
                    .subtotal(subtotal)
                    .build();

            venta.getDetalles().add(det);
            total = total.add(subtotal);

            // Asentar movimiento en Kardex Físico
            TipoMovimientoKardex tipoKardex = (estado == EstadoVenta.ANULADA)
                    ? TipoMovimientoKardex.ENTRADA
                    : TipoMovimientoKardex.SALIDA;
            String motivo = (estado == EstadoVenta.ANULADA) ? "ANULACION_VENTA_POS" : "VENTA_POS_FEFO";

            kardexRepository.save(MovimientoKardex.builder()
                    .tipo(tipoKardex)
                    .lote(loteEncontrado)
                    .productoId(prod.getId())
                    .productoNombre(prod.getNombre())
                    .cantidad(BigDecimal.valueOf(item.cantidad))
                    .saldoStockResultante(BigDecimal.valueOf(prod.getStock() != null ? prod.getStock() : 100))
                    .motivo(motivo)
                    .documentoReferencia(ticket)
                    .fechaHora(fecha)
                    .build());
        }

        total = total.setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = total.divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(subtotal);

        venta.setSubtotal(subtotal);
        venta.setIgv(igv);
        venta.setTotal(total);

        Venta guardada = ventaRepository.save(venta);
        auditoriaService.registrar(vendedorId, "CREAR", "VENTA", guardada.getId(), "total=" + total);
        return guardada;
    }

    private record ItemSeed(Long productoId, int cantidad) {}
}

