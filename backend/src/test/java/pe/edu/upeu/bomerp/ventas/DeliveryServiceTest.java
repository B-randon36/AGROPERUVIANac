package pe.edu.upeu.bomerp.ventas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.bomerp.exception.BusinessConflictException;
import pe.edu.upeu.bomerp.finanzas.api.FinanzasApi;
import pe.edu.upeu.bomerp.finanzas.entity.MetodoPago;
import pe.edu.upeu.bomerp.ventas.dto.ActualizarEstadoDeliveryRequest;
import pe.edu.upeu.bomerp.ventas.dto.AsignarRepartidorRequest;
import pe.edu.upeu.bomerp.ventas.dto.CrearDeliveryRequest;
import pe.edu.upeu.bomerp.ventas.dto.PedidoDeliveryResponse;
import pe.edu.upeu.bomerp.ventas.entity.EstadoDelivery;
import pe.edu.upeu.bomerp.ventas.entity.PedidoDelivery;
import pe.edu.upeu.bomerp.ventas.repository.PedidoDeliveryRepository;
import pe.edu.upeu.bomerp.ventas.service.DeliveryServiceImpl;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private PedidoDeliveryRepository deliveryRepository;

    @Mock
    private FinanzasApi finanzasApi;

    @InjectMocks
    private DeliveryServiceImpl deliveryService;

    @Test
    @DisplayName("Debe crear pedido de delivery en estado PENDIENTE y autogenerar código")
    void crearPedido_exito() {
        CrearDeliveryRequest req = CrearDeliveryRequest.builder()
                .clienteNombre("EcoMarket Miraflores")
                .clienteTelefono("987654321")
                .direccionEntrega("Av. Larco 880, Miraflores")
                .costoEnvio(new BigDecimal("12.00"))
                .montoTotal(new BigDecimal("250.00"))
                .repartidorNombre("Carlos Mendoza (Moto 01)")
                .build();

        when(deliveryRepository.findByCodigoPedido(any())).thenReturn(Optional.empty());
        when(deliveryRepository.save(any(PedidoDelivery.class))).thenAnswer(inv -> {
            PedidoDelivery p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        PedidoDeliveryResponse res = deliveryService.crearPedido(req);

        assertNotNull(res.getId());
        assertTrue(res.getCodigoPedido().startsWith("DEL-"));
        assertEquals(EstadoDelivery.PENDIENTE, res.getEstadoDelivery());
        assertEquals("EcoMarket Miraflores", res.getClienteNombre());
    }

    @Test
    @DisplayName("Debe transicionar pedido a ENTREGADO y liquidar cobro contra entrega en Finanzas")
    void actualizarEstado_entregado_exito() {
        PedidoDelivery existente = PedidoDelivery.builder()
                .id(5L)
                .codigoPedido("DEL-2026-005")
                .clienteNombre("BioVida San Isidro")
                .direccionEntrega("San Isidro")
                .costoEnvio(new BigDecimal("10.00"))
                .montoTotal(new BigDecimal("180.00"))
                .estadoDelivery(EstadoDelivery.EN_RUTA)
                .build();

        when(deliveryRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(deliveryRepository.save(any(PedidoDelivery.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoDeliveryResponse res = deliveryService.actualizarEstado(5L,
                ActualizarEstadoDeliveryRequest.builder()
                        .estado(EstadoDelivery.ENTREGADO)
                        .observaciones("Entregado con conformidad")
                        .build());

        assertEquals(EstadoDelivery.ENTREGADO, res.getEstadoDelivery());
        assertNotNull(res.getFechaEntrega());
        verify(finanzasApi, times(1)).registrarIngresoVentaPos(
                eq(new BigDecimal("180.00")),
                eq(MetodoPago.EFECTIVO),
                eq("DEL-2026-005"),
                eq("BioVida San Isidro")
        );
    }

    @Test
    @DisplayName("Debe impedir revertir un pedido que ya fue ENTREGADO")
    void actualizarEstado_yaEntregado_lanzaExcepcion() {
        PedidoDelivery entregado = PedidoDelivery.builder()
                .id(6L)
                .codigoPedido("DEL-2026-006")
                .estadoDelivery(EstadoDelivery.ENTREGADO)
                .build();

        when(deliveryRepository.findById(6L)).thenReturn(Optional.of(entregado));

        assertThrows(BusinessConflictException.class, () ->
                deliveryService.actualizarEstado(6L,
                        ActualizarEstadoDeliveryRequest.builder()
                                .estado(EstadoDelivery.PENDIENTE)
                                .build()));
    }

    @Test
    @DisplayName("Debe asignar nuevo repartidor al pedido")
    void asignarRepartidor_exito() {
        PedidoDelivery existente = PedidoDelivery.builder()
                .id(7L)
                .codigoPedido("DEL-2026-007")
                .estadoDelivery(EstadoDelivery.PENDIENTE)
                .build();

        when(deliveryRepository.findById(7L)).thenReturn(Optional.of(existente));
        when(deliveryRepository.save(any(PedidoDelivery.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoDeliveryResponse res = deliveryService.asignarRepartidor(7L,
                AsignarRepartidorRequest.builder().repartidorNombre("Luis Quispe (Furgón 02)").build());

        assertEquals("Luis Quispe (Furgón 02)", res.getRepartidorNombre());
    }
}
