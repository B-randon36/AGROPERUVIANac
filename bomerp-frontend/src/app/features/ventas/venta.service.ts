import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiService } from '../../core/services/api.service';
import { VentaRequest, VentaResponse, VentaReporte, PageResponse } from './venta.model';

@Injectable({ providedIn: 'root' })
export class VentaService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);
  private readonly resource = '/api/v1/ventas';

  consultarDni(dni: string): Observable<{ dni: string; nombres: string; apellidoPaterno: string; apellidoMaterno: string; nombreCompleto: string }> {
    return this.http.get<{ dni: string; nombres: string; apellidoPaterno: string; apellidoMaterno: string; nombreCompleto: string }>(
      this.api.buildUrl(`/api/v1/personas/dni/${dni}`),
    );
  }

  crear(venta: VentaRequest): Observable<VentaResponse> {
    return this.http.post<VentaResponse>(this.api.buildUrl(this.resource), venta);
  }

  buscar(
    estado?: string,
    desde?: string,
    hasta?: string,
    page = 0,
    size = 50,
    sort = 'fecha,desc',
  ): Observable<VentaResponse[]> {
    let params = new HttpParams().set('page', page).set('size', size).set('sort', sort);
    if (estado) params = params.set('estado', estado);
    if (desde) params = params.set('desde', desde);
    if (hasta) params = params.set('hasta', hasta);
    return this.http.get<any>(this.api.buildUrl(this.resource), { params }).pipe(
      map(res => Array.isArray(res) ? res : (res?.content || []))
    );
  }

  buscarPaginado(
    estado?: string,
    desde?: string,
    hasta?: string,
    page = 0,
    size = 20,
    sort = 'fecha,desc',
  ): Observable<PageResponse<VentaResponse>> {
    let params = new HttpParams().set('page', page).set('size', size).set('sort', sort);
    if (estado) params = params.set('estado', estado);
    if (desde) params = params.set('desde', desde);
    if (hasta) params = params.set('hasta', hasta);
    return this.http.get<PageResponse<VentaResponse>>(this.api.buildUrl(this.resource), { params });
  }

  obtener(id: number): Observable<VentaResponse> {
    return this.http.get<VentaResponse>(this.api.buildUrl(`${this.resource}/${id}`));
  }

  anular(id: number): Observable<VentaResponse> {
    return this.http.patch<VentaResponse>(this.api.buildUrl(`${this.resource}/${id}/anular`), {});
  }

  reporte(estado?: string, desde?: string, hasta?: string): Observable<VentaReporte> {
    let params = new HttpParams();
    if (estado) params = params.set('estado', estado);
    if (desde) params = params.set('desde', desde);
    if (hasta) params = params.set('hasta', hasta);
    return this.http.get<VentaReporte>(this.api.buildUrl(`${this.resource}/resumen`), { params });
  }

  eliminarTodas(): Observable<void> {
    return this.http.delete<void>(this.api.buildUrl(this.resource));
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(this.api.buildUrl(`${this.resource}/${id}`));
  }
}
