import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import {
  Almacen,
  AlmacenRequest,
  DescuentoFefoRequest,
  DescuentoFefoResponse,
  Lote,
  LoteCreateDto,
  MovimientoKardex,
  StockCritico,
} from './inventario.model';

@Injectable({
  providedIn: 'root',
})
export class InventarioService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  listarLotes(productoId?: number): Observable<Lote[]> {
    let params = new HttpParams();
    if (productoId) {
      params = params.set('productoId', productoId);
    }
    return this.http.get<Lote[]>(this.api.buildUrl('/api/v1/inventario/lotes'), { params });
  }

  listarAlertasVencimiento(dias = 30): Observable<Lote[]> {
    const params = new HttpParams().set('dias', dias);
    return this.http.get<Lote[]>(this.api.buildUrl('/api/v1/inventario/lotes/alertas-vencimiento'), { params });
  }

  registrarLote(dto: LoteCreateDto): Observable<Lote> {
    return this.http.post<Lote>(this.api.buildUrl('/api/v1/inventario/lotes'), dto);
  }

  despachoFefo(request: DescuentoFefoRequest): Observable<DescuentoFefoResponse> {
    return this.http.post<DescuentoFefoResponse>(this.api.buildUrl('/api/v1/inventario/despacho-fefo'), request);
  }

  listarKardex(productoId?: number): Observable<MovimientoKardex[]> {
    let params = new HttpParams();
    if (productoId) {
      params = params.set('productoId', productoId);
    }
    return this.http.get<MovimientoKardex[]>(this.api.buildUrl('/api/v1/inventario/kardex'), { params });
  }

  evaluarVencimientos(): Observable<void> {
    return this.http.post<void>(this.api.buildUrl('/api/v1/inventario/evaluar-vencimientos'), {});
  }

  // --- Almacenes Físicos ---
  listarAlmacenes(): Observable<Almacen[]> {
    return this.http.get<Almacen[]>(this.api.buildUrl('/api/v1/inventario/almacenes'));
  }

  obtenerAlmacen(id: number): Observable<Almacen> {
    return this.http.get<Almacen>(this.api.buildUrl(`/api/v1/inventario/almacenes/${id}`));
  }

  crearAlmacen(request: AlmacenRequest): Observable<Almacen> {
    return this.http.post<Almacen>(this.api.buildUrl('/api/v1/inventario/almacenes'), request);
  }

  actualizarAlmacen(id: number, request: AlmacenRequest): Observable<Almacen> {
    return this.http.put<Almacen>(this.api.buildUrl(`/api/v1/inventario/almacenes/${id}`), request);
  }

  eliminarAlmacen(id: number): Observable<void> {
    return this.http.delete<void>(this.api.buildUrl(`/api/v1/inventario/almacenes/${id}`));
  }

  // --- Stock Crítico ---
  obtenerStockCritico(stockMinimo = 100, soloAlertas = false): Observable<StockCritico[]> {
    const params = new HttpParams()
      .set('stockMinimo', stockMinimo)
      .set('soloAlertas', soloAlertas);
    return this.http.get<StockCritico[]>(this.api.buildUrl('/api/v1/inventario/stock-critico'), { params });
  }
}
