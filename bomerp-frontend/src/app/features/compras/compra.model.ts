export interface Proveedor {
  id: number;
  ruc: string;
  razonSocial: string;
  telefono?: string;
  email?: string;
  direccion?: string;
}

export interface ProveedorRequest {
  ruc: string;
  razonSocial: string;
  telefono?: string;
  email?: string;
  direccion?: string;
}

export interface RucConsultaResponse {
  success?: boolean;
  message?: string;
  ruc: string;
  razonSocial: string;
  nombreComercial?: string;
  estado?: string;
  condicion?: string;
  direccion?: string;
  departamento?: string;
  provincia?: string;
  distrito?: string;
  ubigeo?: string;
}

export interface DetalleCompra {
  id?: number;
  compraId?: number;
  insumoId: number;
  nombreInsumo: string;
  cantidad: number;
  precioUnitario: number;
  subtotal?: number;
}

export interface DetalleCompraRequest {
  insumoId: number;
  nombreInsumo: string;
  cantidad: number;
  precioUnitario: number;
}

export interface CompraRequest {
  proveedorId: number;
  numeroComprobante: string;
  fechaEmision: string;
  detalles: DetalleCompraRequest[];
}

export interface Compra {
  id: number;
  numeroComprobante: string;
  proveedor?: Proveedor;
  proveedorId?: number;
  fechaEmision: string;
  estado: 'REGISTRADA' | 'PAGADA' | 'ANULADA';
  subtotal: number;
  igv: number;
  total: number;
  saldoPendiente: number;
  detalles?: DetalleCompra[];
}

export interface AmortizarPagoRequest {
  montoPago: number;
  metodoPago?: string;
  referencia?: string;
}

export interface CompraReporte {
  totalCompras: number;
  montoTotalComprado: number;
  montoTotalPagado: number;
  saldoPendienteTotal: number;
  ticketPromedioCompra: number;
}
