import { MetodoPago } from '../sesion-caja/sesion-caja.model';

export interface MetodoPagoResumen {
  metodoPago: MetodoPago;
  totalIngresos: number;
  totalEgresos: number;
  neto: number;
}

export interface FlujoCajaResumen {
  totalIngresos: number;
  totalEgresos: number;
  saldoNeto: number;
  cantidadTransacciones: number;
}

export interface ReporteRendicionesAgregado {
  totalMonto: number;
  promedioMonto: number;
  montoMaximo: number;
  montoMinimo: number;
  cantidadRendiciones: number;
}

export interface DashboardFinanciero {
  totalIngresos: number;
  totalEgresos: number;
  balanceNeto: number;
  saldoCajaDiariaActiva: number;
  sesionActivaId?: number | null;
  codigoSesionActiva?: string | null;
  cajeroActivo?: string | null;
  resumenPorMetodo: MetodoPagoResumen[];
  flujoCaja: FlujoCajaResumen;
  reporteRendiciones: ReporteRendicionesAgregado;
}

export interface CanalVentaResumen {
  canal: string;
  canalNombre: string;
  totalVentas: number;
  cantidadTransacciones: number;
  ticketPromedio: number;
  porcentaje: number;
}

export interface ProductoRentabilidad {
  productoId: number;
  productoNombre: string;
  categoria: string | null;
  unidadesVendidas: number;
  totalIngresos: number;
  costoEstimado: number | null;
  utilidadBruta: number | null;
  margenContribucion: number | null;
  clasificacionAbc: 'A' | 'B' | 'C' | null;
}

export interface ReporteOmnicanal {
  totalVentasOmnicanal: number;
  totalTransacciones: number;
  canales: CanalVentaResumen[];
  rankingProductos: ProductoRentabilidad[];
}

export interface LoteSemaforo {
  loteId: number;
  codigoLote: string;
  productoId: number;
  productoNombre: string;
  fechaFabricacion: string;
  fechaVencimiento: string;
  diasRestantes: number;
  estadoSemaforo: 'CRITICO' | 'PREVENTIVO' | 'OPTIMO' | 'VENCIDO';
  stockActual: number;
  costoUnitario: number;
  valorizacionTotal: number;
}

export interface ReporteSemaforoInventario {
  valorizacionTotalInventario: number;
  totalLotes: number;
  lotesCriticos: number;
  lotesPreventivos: number;
  lotesOptimos: number;
  lotesVencidos: number;
  lotes: LoteSemaforo[];
}

