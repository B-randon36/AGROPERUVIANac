import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import {
  Proveedor,
  ProveedorRequest,
  RucConsultaResponse,
  Compra,
  CompraRequest,
  AmortizarPagoRequest,
  CompraReporte,
  Insumo,
  PagoCompra,
} from './compra.model';

@Injectable({ providedIn: 'root' })
export class CompraService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  listarInsumos(): Observable<Insumo[]> {
    return this.http.get<Insumo[]>(this.api.buildUrl('/api/v1/compras/insumos'));
  }

  listarProveedores(): Observable<Proveedor[]> {
    return this.http.get<Proveedor[]>(this.api.buildUrl('/api/v1/compras/proveedores'));
  }

  crearProveedor(request: ProveedorRequest): Observable<Proveedor> {
    return this.http.post<Proveedor>(this.api.buildUrl('/api/v1/compras/proveedores'), request);
  }

  consultarRuc(ruc: string): Observable<RucConsultaResponse> {
    return this.http.get<RucConsultaResponse>(this.api.buildUrl(`/api/v1/personas/ruc/${ruc}`));
  }

  listarCompras(estado?: string, desde?: string, hasta?: string): Observable<Compra[]> {
    let params = new HttpParams();
    if (estado && estado !== 'TODAS') {
      params = params.set('estado', estado);
    }
    if (desde) {
      params = params.set('desde', desde);
    }
    if (hasta) {
      params = params.set('hasta', hasta);
    }
    return this.http.get<Compra[]>(this.api.buildUrl('/api/v1/compras'), { params });
  }

  crearCompra(request: CompraRequest): Observable<Compra> {
    return this.http.post<Compra>(this.api.buildUrl('/api/v1/compras'), request);
  }

  obtenerCompra(id: number): Observable<Compra> {
    return this.http.get<Compra>(this.api.buildUrl(`/api/v1/compras/${id}`));
  }

  amortizarPago(id: number, request: AmortizarPagoRequest): Observable<Compra> {
    return this.http.put<Compra>(this.api.buildUrl(`/api/v1/compras/${id}/amortizar`), request);
  }

  listarPagos(id: number): Observable<PagoCompra[]> {
    return this.http.get<PagoCompra[]>(this.api.buildUrl(`/api/v1/compras/${id}/pagos`));
  }

  anularCompra(id: number): Observable<Compra> {
    return this.http.post<Compra>(this.api.buildUrl(`/api/v1/compras/${id}/anular`), {});
  }

  reporte(estado?: string, desde?: string, hasta?: string): Observable<CompraReporte> {
    let params = new HttpParams();
    if (estado && estado !== 'TODAS') {
      params = params.set('estado', estado);
    }
    if (desde) {
      params = params.set('desde', desde);
    }
    if (hasta) {
      params = params.set('hasta', hasta);
    }
    return this.http.get<CompraReporte>(this.api.buildUrl('/api/v1/compras/resumen'), { params });
  }
}
