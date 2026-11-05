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

export interface Insumo {
  id: number;
  codigo: string;
  nombre: string;
  unidadMedida: string;
  precioSugerido: number;
  estado?: string;
}

export interface PagoCompra {
  id: number;
  compraId?: number;
  fechaPago: string;
  montoPagado: number;
  metodoPago: string;
  numeroOperacion?: string;
  estado: string;
  fechaCreacion?: string;
}

export interface DetalleCompra {
  id?: number;
  compraId?: number;
  insumoId: number;
  nombreInsumo: string;
  loteProveedor?: string;
  fechaVencimiento?: string;
  factorConversion?: number;
  cantidad: number;
  cantidadIngreso?: number;
  precioUnitario: number;
  subtotal?: number;
}

export interface DetalleCompraRequest {
  insumoId: number;
  nombreInsumo: string;
  loteProveedor?: string;
  fechaVencimiento?: string;
  factorConversion?: number;
  cantidad: number;
  precioUnitario: number;
}

export interface CompraRequest {
  proveedorId: number;
  serieComprobante?: string;
  numeroComprobante: string;
  fechaEmision: string;
  condicionPago?: string;
  fechaVencimientoPago?: string;
  costoFlete?: number;
  detalles: DetalleCompraRequest[];
}

export interface Compra {
  id: number;
  serieComprobante?: string;
  numeroComprobante: string;
  proveedor?: Proveedor;
  proveedorId?: number;
  fechaEmision: string;
  condicionPago?: string;
  fechaVencimientoPago?: string;
  estado: 'REGISTRADA' | 'PAGADA' | 'ANULADA';
  subtotal: number;
  igv: number;
  costoFlete?: number;
  total: number;
  saldoPendiente: number;
  detalles?: DetalleCompra[];
  pagos?: PagoCompra[];
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
