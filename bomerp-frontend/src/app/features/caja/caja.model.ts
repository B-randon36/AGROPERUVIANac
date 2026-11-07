export interface MovimientoCaja {
  id?: number;
  idSesionCaja?: number;
  tipo: 'INGRESO' | 'EGRESO';
  monto: number;
  concepto: string;
  metodoPago: 'EFECTIVO' | 'YAPE' | 'PLIN' | 'TRANSFERENCIA';
  referencia?: string;
  fecha?: string;
}

export interface SesionCaja {
  id: number;
  cajeroNombre: string;
  fechaApertura: string;
  fechaCierre?: string;
  montoApertura: number;
  montoCierre?: number;
  saldoTeorico: number;
  saldoReal?: number;
  diferencia?: number;
  estado: 'ABIERTA' | 'CERRADA';
  observaciones?: string;
}

export interface MovimientoCajaRequest {
  tipo: 'INGRESO' | 'EGRESO';
  monto: number;
  concepto: string;
  metodoPago: 'EFECTIVO' | 'YAPE' | 'PLIN' | 'TRANSFERENCIA';
  referencia?: string;
}

export interface AperturaCajaRequest {
  cajeroNombre: string;
  montoApertura: number;
  observaciones?: string;
  usuarioId?: number;
  cajeroRol?: string;
}

export interface CierreCajaRequest {
  saldoReal: number;
  observaciones?: string;
}

export interface CajaReporte {
  totalSesiones: number;
  totalIngresos: number;
  totalEgresos: number;
  saldoNeto: number;
}
