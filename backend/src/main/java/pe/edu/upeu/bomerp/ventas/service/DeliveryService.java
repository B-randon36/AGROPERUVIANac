package pe.edu.upeu.bomerp.ventas.service;

import pe.edu.upeu.bomerp.ventas.dto.ActualizarEstadoDeliveryRequest;
import pe.edu.upeu.bomerp.ventas.dto.AsignarRepartidorRequest;
import pe.edu.upeu.bomerp.ventas.dto.CrearDeliveryRequest;
import pe.edu.upeu.bomerp.ventas.dto.PedidoDeliveryResponse;
import pe.edu.upeu.bomerp.ventas.entity.EstadoDelivery;

import java.util.List;

public interface DeliveryService {
    List<PedidoDeliveryResponse> listar(EstadoDelivery estado);
    PedidoDeliveryResponse obtenerPorId(Long id);
    PedidoDeliveryResponse crearPedido(CrearDeliveryRequest request);
    PedidoDeliveryResponse actualizarEstado(Long id, ActualizarEstadoDeliveryRequest request);
    PedidoDeliveryResponse asignarRepartidor(Long id, AsignarRepartidorRequest request);
}

