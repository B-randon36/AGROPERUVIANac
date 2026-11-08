import '@angular/compiler';
import { Injector } from '@angular/core';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { Observable, of, throwError } from 'rxjs';
import { ReporteFinancieroList } from './reporte-financiero-list';
import { ReporteFinancieroService } from './reporte-financiero-service';

describe('Reporte financiero con datos reales', () => {
  afterEach(() => vi.useRealTimers());

  function crear(omnicanal: Observable<unknown>) {
    vi.useFakeTimers();
    const service = {
      obtenerDashboard: () => of({ totalIngresos: 0, totalEgresos: 0, balanceNeto: 0 }),
      obtenerReporteOmnicanal: () => omnicanal,
      obtenerReporteSemaforo: () => of({ lotes: [] }),
    };
    const injector = Injector.create({ providers: [
      { provide: ReporteFinancieroService, useValue: service }, ReporteFinancieroList,
    ] });
    return injector.get(ReporteFinancieroList) as any;
  }

  it('un fallo de API no produce cifras de ejemplo y elimina datos anteriores', () => {
    const componente = crear(throwError(() => new Error('403')));
    componente.omnicanal.set({ totalVentasOmnicanal: 100 });
    componente.cargarDatos();
    expect(componente.omnicanal()).toBeNull();
    expect(componente.error()).toContain('No se pudo cargar');
    expect(componente.rankingFiltrado()).toEqual([]);
  });

  it('un reporte vacío conserva cero ventas y cero balance relativo', () => {
    const componente = crear(of({ totalVentasOmnicanal: 0, totalTransacciones: 0, canales: [], rankingProductos: [] }));
    componente.cargarDatos();
    expect(componente.omnicanal().totalVentasOmnicanal).toBe(0);
    expect(componente.omnicanal().totalTransacciones).toBe(0);
    expect(componente.margenOperativo()).toBe(0);
  });

  it('permite buscar productos sin inventar categoría, costo ni margen', () => {
    const producto = { productoId: 1, productoNombre: 'Cacao', categoria: null, clasificacionAbc: null,
      totalIngresos: 118, unidadesVendidas: 1, costoEstimado: null, margenContribucion: null };
    const componente = crear(of({ totalVentasOmnicanal: 118, totalTransacciones: 1, canales: [], rankingProductos: [producto] }));
    componente.cargarDatos();
    componente.busquedaRanking.set('cacao');
    expect(componente.rankingFiltrado()).toEqual([producto]);
    componente.busquedaRanking.set('inexistente');
    expect(componente.rankingFiltrado()).toEqual([]);
  });
});
