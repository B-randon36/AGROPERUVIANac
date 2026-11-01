import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import {
  OrdenProduccion,
  OrdenProduccionRequest,
  CompletarOrdenRequest,
  ProduccionReporte,
  CentroTrabajo,
  CentroTrabajoRequest,
  ListaMateriales,
  ListaMaterialesRequest,
} from './produccion.model';

@Injectable({ providedIn: 'root' })
export class ProduccionService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  // --- Órdenes de Producción ---
  listarOrdenes(): Observable<OrdenProduccion[]> {
    return this.http.get<OrdenProduccion[]>(this.api.buildUrl('/api/v1/produccion/ordenes'));
  }

  obtenerOrden(id: number): Observable<OrdenProduccion> {
    return this.http.get<OrdenProduccion>(this.api.buildUrl(`/api/v1/produccion/ordenes/${id}`));
  }

  crearOrden(request: OrdenProduccionRequest): Observable<OrdenProduccion> {
    return this.http.post<OrdenProduccion>(this.api.buildUrl('/api/v1/produccion/ordenes'), request);
  }

  iniciarOrden(id: number): Observable<OrdenProduccion> {
    return this.http.put<OrdenProduccion>(this.api.buildUrl(`/api/v1/produccion/ordenes/${id}/iniciar`), {});
  }

  completarOrden(id: number, request: CompletarOrdenRequest): Observable<OrdenProduccion> {
    return this.http.put<OrdenProduccion>(
      this.api.buildUrl(`/api/v1/produccion/ordenes/${id}/completar`),
      request
    );
  }

  cancelarOrden(id: number): Observable<OrdenProduccion> {
    return this.http.post<OrdenProduccion>(this.api.buildUrl(`/api/v1/produccion/ordenes/${id}/cancelar`), {});
  }

  reporte(): Observable<ProduccionReporte> {
    return this.http.get<ProduccionReporte>(this.api.buildUrl('/api/v1/produccion/resumen'));
  }

  descargarReportePdf(id: number): Observable<Blob> {
    return this.http.get(this.api.buildUrl(`/api/v1/produccion/ordenes/${id}/reporte-pdf`), {
      responseType: 'blob',
    });
  }

  // --- Centros de Trabajo (Líneas de Planta) ---
  listarCentrosTrabajo(): Observable<CentroTrabajo[]> {
    return this.http.get<CentroTrabajo[]>(this.api.buildUrl('/api/v1/produccion/centros-trabajo'));
  }

  obtenerCentroTrabajo(id: number): Observable<CentroTrabajo> {
    return this.http.get<CentroTrabajo>(this.api.buildUrl(`/api/v1/produccion/centros-trabajo/${id}`));
  }

  crearCentroTrabajo(request: CentroTrabajoRequest): Observable<CentroTrabajo> {
    return this.http.post<CentroTrabajo>(this.api.buildUrl('/api/v1/produccion/centros-trabajo'), request);
  }

  actualizarCentroTrabajo(id: number, request: CentroTrabajoRequest): Observable<CentroTrabajo> {
    return this.http.put<CentroTrabajo>(this.api.buildUrl(`/api/v1/produccion/centros-trabajo/${id}`), request);
  }

  eliminarCentroTrabajo(id: number): Observable<void> {
    return this.http.delete<void>(this.api.buildUrl(`/api/v1/produccion/centros-trabajo/${id}`));
  }

  // --- Recetas Maestras BOM (Listas de Materiales) ---
  listarListasMateriales(): Observable<ListaMateriales[]> {
    return this.http.get<ListaMateriales[]>(this.api.buildUrl('/api/v1/produccion/listas-materiales'));
  }

  obtenerListaMateriales(id: number): Observable<ListaMateriales> {
    return this.http.get<ListaMateriales>(this.api.buildUrl(`/api/v1/produccion/listas-materiales/${id}`));
  }

  obtenerRecetaPorProducto(productoId: number): Observable<ListaMateriales> {
    return this.http.get<ListaMateriales>(
      this.api.buildUrl(`/api/v1/produccion/listas-materiales/por-producto/${productoId}`)
    );
  }

  crearListaMateriales(request: ListaMaterialesRequest): Observable<ListaMateriales> {
    return this.http.post<ListaMateriales>(this.api.buildUrl('/api/v1/produccion/listas-materiales'), request);
  }

  actualizarListaMateriales(id: number, request: ListaMaterialesRequest): Observable<ListaMateriales> {
    return this.http.put<ListaMateriales>(this.api.buildUrl(`/api/v1/produccion/listas-materiales/${id}`), request);
  }

  eliminarListaMateriales(id: number): Observable<void> {
    return this.http.delete<void>(this.api.buildUrl(`/api/v1/produccion/listas-materiales/${id}`));
  }
}
