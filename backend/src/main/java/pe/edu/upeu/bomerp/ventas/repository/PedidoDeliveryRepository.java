package pe.edu.upeu.bomerp.ventas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.bomerp.ventas.entity.EstadoDelivery;
import pe.edu.upeu.bomerp.ventas.entity.PedidoDelivery;

import java.util.List;
import java.util.Optional;

public interface PedidoDeliveryRepository extends JpaRepository<PedidoDelivery, Long> {
    List<PedidoDelivery> findByEstadoDeliveryOrderByIdDesc(EstadoDelivery estadoDelivery);
    List<PedidoDelivery> findAllByOrderByIdDesc();
    Optional<PedidoDelivery> findByCodigoPedido(String codigoPedido);
}

