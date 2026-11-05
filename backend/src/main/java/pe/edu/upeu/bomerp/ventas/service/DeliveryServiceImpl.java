package pe.edu.upeu.bomerp.ventas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.bomerp.exception.BusinessConflictException;
import pe.edu.upeu.bomerp.exception.ResourceNotFoundException;
import pe.edu.upeu.bomerp.finanzas.api.FinanzasApi;
import pe.edu.upeu.bomerp.finanzas.entity.MetodoPago;
import pe.edu.upeu.bomerp.ventas.dto.ActualizarEstadoDeliveryRequest;
import pe.edu.upeu.bomerp.ventas.dto.AsignarRepartidorRequest;
import pe.edu.upeu.bomerp.ventas.dto.CrearDeliveryRequest;
import pe.edu.upeu.bomerp.ventas.dto.PedidoDeliveryResponse;
import pe.edu.upeu.bomerp.ventas.entity.EstadoDelivery;
import pe.edu.upeu.bomerp.ventas.entity.PedidoDelivery;
import pe.edu.upeu.bomerp.ventas.repository.PedidoDeliveryRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final PedidoDeliveryRepository deliveryRepository;
    private final FinanzasApi finanzasApi;

    @Override
    @Transactional(readOnly = true)
    public List<PedidoDeliveryResponse> listar(EstadoDelivery estado) {
        List<PedidoDelivery> pedidos = (estado != null)
                ? deliveryRepository.findByEstadoDeliveryOrderByIdDesc(estado)
                : deliveryRepository.findAllByOrderByIdDesc();
        return pedidos.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PedidoDeliveryResponse obtenerPorId(Long id) {
        PedidoDelivery pedido = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de delivery no encontrado con ID: " + id));
        return toResponse(pedido);
    }

    @Override
    @Transactional
    public PedidoDeliveryResponse crearPedido(CrearDeliveryRequest request) {
        String codigo = (request.getCodigoPedido() != null && !request.getCodigoPedido().isBlank())
                ? request.getCodigoPedido().trim().toUpperCase()
                : "DEL-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + (int) (Math.random() * 900 + 100);

        if (deliveryRepository.findByCodigoPedido(codigo).isPresent()) {
            throw new BusinessConflictException("Ya existe un pedido de delivery con código: " + codigo);
        }

        PedidoDelivery pedido = PedidoDelivery.builder()
                .codigoPedido(codigo)
                .ventaId(request.getVentaId())
                .clienteId(request.getClienteId())
                .clienteNombre(request.getClienteNombre().trim())
                .clienteTelefono(request.getClienteTelefono())
                .direccionEntrega(request.getDireccionEntrega().trim())
                .referencia(request.getReferencia())
                .costoEnvio(request.getCostoEnvio() != null ? request.getCostoEnvio() : BigDecimal.ZERO)
                .montoTotal(request.getMontoTotal() != null ? request.getMontoTotal() : BigDecimal.ZERO)
                .repartidorNombre(request.getRepartidorNombre())
                .latitud(request.getLatitud())
                .longitud(request.getLongitud())
                .estadoDelivery(EstadoDelivery.PENDIENTE)
                .fechaPactada(request.getFechaPactada() != null ? request.getFechaPactada() : LocalDateTime.now().plusHours(2))
                .observaciones(request.getObservaciones())
                .build();

        return toResponse(deliveryRepository.save(pedido));
    }

    @Override
    @Transactional
    public PedidoDeliveryResponse actualizarEstado(Long id, ActualizarEstadoDeliveryRequest request) {
        PedidoDelivery pedido = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de delivery no encontrado con ID: " + id));

        if (pedido.getEstadoDelivery() == EstadoDelivery.CANCELADO) {
            throw new BusinessConflictException("No se puede modificar el estado de un pedido cancelado.");
        }

        if (pedido.getEstadoDelivery() == EstadoDelivery.ENTREGADO && request.getEstado() != EstadoDelivery.ENTREGADO) {
            throw new BusinessConflictException("El pedido ya fue entregado y no puede revertirse.");
        }

        boolean pasaAEntregado = pedido.getEstadoDelivery() != EstadoDelivery.ENTREGADO
                && request.getEstado() == EstadoDelivery.ENTREGADO;

        pedido.setEstadoDelivery(request.getEstado());
        if (pasaAEntregado) {
            pedido.setFechaEntrega(LocalDateTime.now());
            // Si el despacho no provenía de una venta POS ya cobrada, liquidar el cobro contra entrega en Caja
            if (finanzasApi != null && pedido.getVentaId() == null
                    && pedido.getMontoTotal() != null
                    && pedido.getMontoTotal().compareTo(BigDecimal.ZERO) > 0) {
                finanzasApi.registrarIngresoVentaPos(
                        pedido.getMontoTotal(),
                        MetodoPago.EFECTIVO,
                        pedido.getCodigoPedido(),
                        pedido.getClienteNombre()
                );
            }
        }
        if (request.getObservaciones() != null && !request.getObservaciones().isBlank()) {
            pedido.setObservaciones(request.getObservaciones());
        }

        return toResponse(deliveryRepository.save(pedido));
    }

    @Override
    @Transactional
    public PedidoDeliveryResponse asignarRepartidor(Long id, AsignarRepartidorRequest request) {
        PedidoDelivery pedido = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de delivery no encontrado con ID: " + id));

        pedido.setRepartidorNombre(request.getRepartidorNombre().trim());
        return toResponse(deliveryRepository.save(pedido));
    }

    private PedidoDeliveryResponse toResponse(PedidoDelivery p) {
        return PedidoDeliveryResponse.builder()
                .id(p.getId())
                .codigoPedido(p.getCodigoPedido())
                .ventaId(p.getVentaId())
                .clienteId(p.getClienteId())
                .clienteNombre(p.getClienteNombre())
                .clienteTelefono(p.getClienteTelefono())
                .direccionEntrega(p.getDireccionEntrega())
                .referencia(p.getReferencia())
                .costoEnvio(p.getCostoEnvio())
                .montoTotal(p.getMontoTotal())
                .repartidorNombre(p.getRepartidorNombre())
                .estadoDelivery(p.getEstadoDelivery())
                .fechaPactada(p.getFechaPactada())
                .fechaEntrega(p.getFechaEntrega())
                .latitud(p.getLatitud())
                .longitud(p.getLongitud())
                .observaciones(p.getObservaciones())
                .build();
    }
}
