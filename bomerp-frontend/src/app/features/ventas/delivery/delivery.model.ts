export type EstadoDelivery = 'PENDIENTE' | 'EN_RUTA' | 'ENTREGADO' | 'CANCELADO';

export interface PedidoDelivery {
  id: number;
  codigoPedido: string;
  ventaId?: number;
  clienteId?: number;
  clienteNombre: string;
  clienteTelefono?: string;
  direccionEntrega: string;
  referencia?: string;
  costoEnvio: number;
  montoTotal?: number;
  repartidorNombre?: string;
  estadoDelivery: EstadoDelivery;
  fechaPactada?: string;
  fechaEntrega?: string;
  latitud?: number;
  longitud?: number;
  observaciones?: string;
}

export interface CrearDeliveryRequest {
  codigoPedido?: string;
  ventaId?: number;
  clienteId?: number;
  clienteNombre: string;
  clienteTelefono?: string;
  direccionEntrega: string;
  referencia?: string;
  costoEnvio: number;
  montoTotal?: number;
  repartidorNombre?: string;
  latitud?: number;
  longitud?: number;
  fechaPactada?: string;
  observaciones?: string;
}

export interface ActualizarEstadoDeliveryRequest {
  estado: EstadoDelivery;
  observaciones?: string;
}

export interface AsignarRepartidorRequest {
  repartidorNombre: string;
}
