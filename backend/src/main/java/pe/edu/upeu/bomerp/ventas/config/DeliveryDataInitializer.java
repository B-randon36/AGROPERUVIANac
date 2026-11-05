package pe.edu.upeu.bomerp.ventas.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import pe.edu.upeu.bomerp.ventas.entity.EstadoDelivery;
import pe.edu.upeu.bomerp.ventas.entity.PedidoDelivery;
import pe.edu.upeu.bomerp.ventas.repository.PedidoDeliveryRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.init.demo-data", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DeliveryDataInitializer implements CommandLineRunner {

    private final PedidoDeliveryRepository deliveryRepository;

    @Override
    public void run(String... args) {
        if (deliveryRepository.count() == 0) {
            log.info("Inicializando pedidos semilla de Delivery para Agro Peruvian Foods...");
            LocalDateTime ahora = LocalDateTime.now();

            List<PedidoDelivery> demos = List.of(
                    PedidoDelivery.builder()
                            .codigoPedido("DEL-2026-001")
                            .clienteNombre("EcoMarket Orgánico Miraflores")
                            .clienteTelefono("987654321")
                            .direccionEntrega("Av. Larco 880, Miraflores")
                            .referencia("Frente al Parque Kennedy, local esquinero")
                            .costoEnvio(new BigDecimal("12.00"))
                            .montoTotal(new BigDecimal("340.00"))
                            .repartidorNombre("Carlos Mendoza (Moto 01)")
                            .latitud(-12.1215)
                            .longitud(-77.0298)
                            .estadoDelivery(EstadoDelivery.EN_RUTA)
                            .fechaPactada(ahora.plusMinutes(45))
                            .observaciones("1 bandeja Aceite de Coco Virgen 120ml + 2 bolsas Coco Rallado 500g")
                            .build(),
                    PedidoDelivery.builder()
                            .codigoPedido("DEL-2026-002")
                            .clienteNombre("Pastelería Fina ChocoArte Surco")
                            .clienteTelefono("994512368")
                            .direccionEntrega("Av. Primavera 1420, Santiago de Surco")
                            .referencia("Edificio Empresarial Monterrico Piso 1")
                            .costoEnvio(new BigDecimal("15.00"))
                            .montoTotal(new BigDecimal("745.00"))
                            .repartidorNombre("Luis Quispe (Furgón 02)")
                            .latitud(-12.1120)
                            .longitud(-76.9785)
                            .estadoDelivery(EstadoDelivery.EN_RUTA)
                            .fechaPactada(ahora.plusHours(1))
                            .observaciones("1 Balde 20L Aceite de Coco Virgen prensado en frío")
                            .build(),
                    PedidoDelivery.builder()
                            .codigoPedido("DEL-2026-003")
                            .clienteNombre("Tienda Saludable BioVida San Isidro")
                            .clienteTelefono("955412890")
                            .direccionEntrega("Calle Los Libertadores 315, San Isidro")
                            .referencia("A media cuadra del Bosque El Olivar")
                            .costoEnvio(new BigDecimal("10.00"))
                            .montoTotal(new BigDecimal("210.00"))
                            .repartidorNombre("Carlos Mendoza (Moto 01)")
                            .latitud(-12.0970)
                            .longitud(-77.0360)
                            .estadoDelivery(EstadoDelivery.PENDIENTE)
                            .fechaPactada(ahora.plusHours(2))
                            .observaciones("1 Balde 4L Aceite de Coco Virgen - cobrar con Yape al entregar")
                            .build(),
                    PedidoDelivery.builder()
                            .codigoPedido("DEL-2026-004")
                            .clienteNombre("Cafetería Cacao Nativo San Borja")
                            .clienteTelefono("971234985")
                            .direccionEntrega("Av. Aviación 2740, San Borja")
                            .referencia("Altura estación San Borja Sur")
                            .costoEnvio(new BigDecimal("12.00"))
                            .montoTotal(new BigDecimal("180.00"))
                            .repartidorNombre("Pedro Rojas (Moto 03)")
                            .latitud(-12.0880)
                            .longitud(-77.0010)
                            .estadoDelivery(EstadoDelivery.ENTREGADO)
                            .fechaPactada(ahora.minusHours(2))
                            .fechaEntrega(ahora.minusHours(1))
                            .observaciones("6 bolsas Cacao Grano Entero 1kg entregado conforme")
                            .build(),
                    PedidoDelivery.builder()
                            .codigoPedido("DEL-2026-005")
                            .clienteNombre("Restaurante Vegano Verde Vital La Molina")
                            .clienteTelefono("963258741")
                            .direccionEntrega("Av. Raúl Ferrero 1250, La Molina")
                            .referencia("Centro Comercial Molina Plaza")
                            .costoEnvio(new BigDecimal("18.00"))
                            .montoTotal(new BigDecimal("278.00"))
                            .repartidorNombre("Luis Quispe (Furgón 02)")
                            .latitud(-12.0830)
                            .longitud(-76.9450)
                            .estadoDelivery(EstadoDelivery.PENDIENTE)
                            .fechaPactada(ahora.plusHours(3))
                            .observaciones("4 frascos Aceite de Coco Virgen 1L")
                            .build()
            );

            deliveryRepository.saveAll(demos);
            log.info("Pedidos semilla de Delivery creados: {}", demos.size());
        }
    }
}

