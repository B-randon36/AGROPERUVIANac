import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ApiService } from '../services/api.service';
import { AuthService } from '../auth/auth-service';

export interface DashboardKpis {
  // Finanzas & Caja
  saldoCajaActiva: number;
  codigoSesionActiva: string | null;
  cajeroActivo: string | null;
  balanceNeto: number;
  totalIngresos: number;
  totalEgresos: number;

  // Ventas POS
  montoVentas: number;
  cantidadVentas: number;
  ticketPromedio: number;

  // Producción
  ordenesEnProceso: number;
  ordenesPlanificadas: number;
  ordenesCompletadas: number;

  // Inventario & Semáforo FEFO
  valorInventario: number;
  lotesOptimos: number;
  lotesPreventivos: number;
  lotesCriticos: number;
  lotesVencidos: number;

  // Compras & Proveedores
  montoCompras: number;
  cuentasPorPagar: number;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, CurrencyPipe],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);
  private readonly http = inject(HttpClient);

  protected readonly apiOnline = signal(false);
  protected readonly checkingStatus = signal(true);
  protected readonly loadingKpis = signal(false);

  protected readonly kpis = signal<DashboardKpis>({
    saldoCajaActiva: 0,
    codigoSesionActiva: null,
    cajeroActivo: null,
    balanceNeto: 0,
    totalIngresos: 0,
    totalEgresos: 0,
    montoVentas: 0,
    cantidadVentas: 0,
    ticketPromedio: 0,
    ordenesEnProceso: 0,
    ordenesPlanificadas: 0,
    ordenesCompletadas: 0,
    valorInventario: 0,
    lotesOptimos: 0,
    lotesPreventivos: 0,
    lotesCriticos: 0,
    lotesVencidos: 0,
    montoCompras: 0,
    cuentasPorPagar: 0,
  });

  ngOnInit(): void {
    this.verificarApiYCargarKpis();
  }

  verificarApiYCargarKpis(): void {
    this.checkingStatus.set(true);

    this.http.get(this.api.buildUrl('/api/v1/categorias')).subscribe({
      next: () => {
        this.apiOnline.set(true);
        this.checkingStatus.set(false);
        if (this.auth.autenticado()) {
          this.cargarMetricasEjecutivas();
        }
      },
      error: () => {
        this.apiOnline.set(false);
        this.checkingStatus.set(false);
      },
    });
  }

  cargarMetricasEjecutivas(): void {
    this.loadingKpis.set(true);

    const finanzas$ = this.http
      .get<any>(this.api.buildUrl('/api/v1/finanzas/reportes/dashboard'))
      .pipe(catchError(() => of(null)));

    const ventas$ = this.http
      .get<any>(this.api.buildUrl('/api/v1/ventas/resumen'))
      .pipe(catchError(() => of(null)));

    const produccion$ = this.http
      .get<any[]>(this.api.buildUrl('/api/v1/produccion/ordenes'))
      .pipe(catchError(() => of([])));

    const semaforo$ = this.http
      .get<any>(this.api.buildUrl('/api/v1/inventario/reporte-semaforo'))
      .pipe(catchError(() => of(null)));

    const compras$ = this.http
      .get<any>(this.api.buildUrl('/api/v1/compras/resumen'))
      .pipe(catchError(() => of(null)));

    forkJoin({
      finanzas: finanzas$,
      ventas: ventas$,
      produccion: produccion$,
      semaforo: semaforo$,
      compras: compras$,
    }).subscribe({
      next: ({ finanzas, ventas, produccion, semaforo, compras }) => {
        const prodList = Array.isArray(produccion) ? produccion : [];

        this.kpis.set({
          saldoCajaActiva: Number(finanzas?.saldoCajaDiariaActiva) || 0,
          codigoSesionActiva: finanzas?.codigoSesionActiva || null,
          cajeroActivo: finanzas?.cajeroActivo || null,
          balanceNeto: Number(finanzas?.balanceNeto) || 0,
          totalIngresos: Number(finanzas?.totalIngresos) || 0,
          totalEgresos: Number(finanzas?.totalEgresos) || 0,

          montoVentas: Number(ventas?.agregado?.montoTotal) || 0,
          cantidadVentas: Number(ventas?.agregado?.totalVentas) || 0,
          ticketPromedio: Number(ventas?.agregado?.ticketPromedio) || 0,

          ordenesEnProceso: prodList.filter((o) => o.estado === 'EN_PROCESO').length,
          ordenesPlanificadas: prodList.filter((o) => o.estado === 'PLANIFICADA').length,
          ordenesCompletadas: prodList.filter((o) => o.estado === 'COMPLETADA').length,

          valorInventario: Number(semaforo?.valorizacionTotalInventario) || 0,
          lotesOptimos: Number(semaforo?.lotesOptimos) || 0,
          lotesPreventivos: Number(semaforo?.lotesPreventivos) || 0,
          lotesCriticos: Number(semaforo?.lotesCriticos) || 0,
          lotesVencidos: Number(semaforo?.lotesVencidos) || 0,

          montoCompras: Number(compras?.montoTotalComprado) || 0,
          cuentasPorPagar: Number(compras?.saldoPendienteTotal) || 0,
        });

        this.loadingKpis.set(false);
      },
      error: () => {
        this.loadingKpis.set(false);
      },
    });
  }
}
