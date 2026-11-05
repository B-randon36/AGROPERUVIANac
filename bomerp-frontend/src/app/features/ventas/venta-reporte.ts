import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { VentaService } from './venta.service';
import { VentaAgregado, VentaResponse } from './venta.model';
import { AuthService } from '../../core/auth/auth-service';

@Component({
  selector: 'app-venta-reporte',
  imports: [CommonModule, CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './venta-reporte.html',
  styleUrl: './venta-reporte.css',
})
export class VentaReporteComponent implements OnInit {
  private readonly ventaService = inject(VentaService);
  protected readonly auth = inject(AuthService);

  // Filas paginadas de la página actual
  protected readonly ventas = signal<VentaResponse[]>([]);

  // Agregados reales calculados en el servidor (SQL SUM/COUNT en BD Oracle/H2)
  protected readonly agregado = signal<VentaAgregado>({
    totalVentas: 0,
    montoTotal: 0,
    ticketPromedio: 0,
  });

  // Totales provenientes directamente del servidor (S14: separación de responsabilidades)
  protected readonly totalVentas = computed(() => this.agregado().totalVentas ?? 0);
  protected readonly montoTotal = computed(() => this.agregado().montoTotal ?? 0);
  protected readonly ticketPromedio = computed(() => this.agregado().ticketPromedio ?? 0);

  // Filtros
  protected readonly estadoFiltro = signal('');
  protected readonly desdeFiltro = signal('');
  protected readonly hastaFiltro = signal('');

  // Paginación
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(10);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);

  // Estado UI
  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set(null);
    const desde = this.desdeFiltro() ? `${this.desdeFiltro()}T00:00:00` : undefined;
    const hasta = this.hastaFiltro() ? `${this.hastaFiltro()}T23:59:59` : undefined;

    // 1. Obtener agregados reales del servidor (/api/v1/ventas/resumen)
    this.ventaService
      .reporte(this.estadoFiltro() || undefined, desde, hasta)
      .subscribe({
        next: (rep) => {
          if (rep.agregado) {
            this.agregado.set(rep.agregado);
          } else {
            this.agregado.set({
              totalVentas: rep.totalVentas ?? 0,
              montoTotal: rep.montoTotal ?? rep.totalRecaudado ?? 0,
              ticketPromedio: rep.ticketPromedio ?? 0,
            });
          }
        },
        error: () => {},
      });

    // 2. Obtener solo las filas correspondientes a la página solicitada (S14 Paginación)
    this.ventaService
      .buscarPaginado(
        this.estadoFiltro() || undefined,
        desde,
        hasta,
        this.pageIndex(),
        this.pageSize(),
      )
      .subscribe({
        next: (page) => {
          this.ventas.set(page.content);
          this.totalElements.set(page.totalElements);
          this.totalPages.set(page.totalPages);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('No se pudo cargar el reporte de ventas del servidor.');
          this.loading.set(false);
        },
      });
  }

  filtrarPorEstado(estado: string): void {
    this.estadoFiltro.set(estado);
    this.pageIndex.set(0);
    this.cargar();
  }

  filtrarPorFecha(desde: string, hasta: string): void {
    this.desdeFiltro.set(desde);
    this.hastaFiltro.set(hasta);
    this.pageIndex.set(0);
    this.cargar();
  }

  limpiarFiltros(): void {
    this.estadoFiltro.set('');
    this.desdeFiltro.set('');
    this.hastaFiltro.set('');
    this.pageIndex.set(0);
    this.cargar();
  }

  cambiarPagina(nuevaPagina: number): void {
    if (nuevaPagina >= 0 && nuevaPagina < this.totalPages()) {
      this.pageIndex.set(nuevaPagina);
      this.cargar();
    }
  }

  cambiarPageSize(size: number): void {
    this.pageSize.set(size);
    this.pageIndex.set(0);
    this.cargar();
  }

  anular(id: number): void {
    if (!confirm('¿Anular esta venta? El stock de sus productos se restaurará.')) {
      return;
    }
    this.ventaService.anular(id).subscribe({
      next: () => this.cargar(),
      error: () => this.error.set('No se pudo anular la venta seleccionada.'),
    });
  }

  eliminarTodas(): void {
    if (!confirm('¿Está seguro de eliminar todas las ventas del reporte? Esta acción vaciará el historial transaccional.')) {
      return;
    }
    this.loading.set(true);
    this.ventaService.eliminarTodas().subscribe({
      next: () => {
        this.pageIndex.set(0);
        this.cargar();
      },
      error: () => {
        this.error.set('No se pudieron eliminar las ventas del reporte.');
        this.loading.set(false);
      },
    });
  }

  eliminar(id: number): void {
    if (!confirm('¿Está seguro de eliminar permanentemente este registro de venta?')) {
      return;
    }
    this.loading.set(true);
    this.ventaService.eliminar(id).subscribe({
      next: () => this.cargar(),
      error: () => {
        this.error.set('No se pudo eliminar la venta seleccionada.');
        this.loading.set(false);
      },
    });
  }
}
