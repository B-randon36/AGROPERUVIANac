export type EstadoOrden = 'PLANIFICADA' | 'PROGRAMADA' | 'EN_PROCESO' | 'COMPLETADA' | 'CANCELADA';

export interface DetalleConsumoInsumo {
  id?: number;
  insumoId: number;
  nombreInsumo: string;
  cantidadRequerida: number;
  cantidadConsumida?: number;
  unidadMedida: string;
}

export interface OrdenProduccion {
  id: number;
  codigoOrden: string;
  productoId: number;
  nombreProducto: string;
  cantidadProgramada: number;
  cantidadProducida?: number;
  fechaInicio?: string;
  fechaFin?: string;
  estado: EstadoOrden;
  centroTrabajoId?: number;
  centroTrabajoNombre?: string;
  observaciones?: string;
  version?: number;
  insumosConsumidos?: DetalleConsumoInsumo[];
  insumos?: DetalleConsumoInsumo[];
}

export interface DetalleConsumoRequest {
  insumoId: number;
  nombreInsumo: string;
  cantidadRequerida: number;
  cantidadConsumida: number;
  unidadMedida: string;
}

export interface OrdenProduccionRequest {
  productoId: number;
  nombreProducto: string;
  cantidadProgramada: number;
  centroTrabajoId?: number;
  centroTrabajoNombre?: string;
  observaciones?: string;
  insumos: DetalleConsumoRequest[];
}

export interface CompletarOrdenRequest {
  cantidadProducida: number;
  fechaVencimiento: string;
}

export interface ProduccionReporte {
  totalOrdenes: number;
  ordenesCompletadas: number;
  ordenesEnProceso: number;
  ordenesPlanificadas: number;
  totalCantidadProgramada: number;
  totalCantidadProducida: number;
  porcentajeEficiencia: number;
  ordenes?: OrdenProduccion[];
}

// Modelos para Centros de Trabajo (Isaí Armuto)
export interface CentroTrabajo {
  id: number;
  codigo: string;
  nombre: string;
  descripcion?: string;
  capacidad?: number;
  unidadCapacidad?: string;
  costoHora: number;
  activo: boolean;
  version?: number;
}

export interface CentroTrabajoRequest {
  codigo: string;
  nombre: string;
  descripcion?: string;
  capacidad?: number;
  unidadCapacidad?: string;
  costoHora: number;
  activo?: boolean;
}

// Modelos para Recetas Maestras BOM / Listas de Materiales (Isaí Armuto)
export interface DetalleListaMateriales {
  id?: number;
  insumoId: number;
  nombreInsumo: string;
  cantidadRequerida: number;
  unidadMedida: string;
}

export interface ListaMateriales {
  id: number;
  codigo: string;
  productoId: number;
  nombreProducto: string;
  cantidadBase: number;
  observaciones?: string;
  activo: boolean;
  version?: number;
  detalles: DetalleListaMateriales[];
}

export interface DetalleListaMaterialesRequest {
  insumoId: number;
  nombreInsumo: string;
  cantidadRequerida: number;
  unidadMedida: string;
}

export interface ListaMaterialesRequest {
  codigo: string;
  productoId: number;
  nombreProducto: string;
  cantidadBase: number;
  observaciones?: string;
  activo?: boolean;
  detalles: DetalleListaMaterialesRequest[];
}
