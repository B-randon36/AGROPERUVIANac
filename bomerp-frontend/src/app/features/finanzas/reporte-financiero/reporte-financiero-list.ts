import {
  Component,
  inject,
  signal,
  computed,
  OnInit,
  AfterViewInit,
  OnDestroy,
  ViewChild,
  ElementRef,
} from '@angular/core';
import { CommonModule, CurrencyPipe, DecimalPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Chart, registerables } from 'chart.js';
import { ReporteFinancieroService } from './reporte-financiero-service';
import {
  DashboardFinanciero,
  ReporteOmnicanal,
  ReporteSemaforoInventario,
} from './reporte-financiero.model';

Chart.register(...registerables);

export type TipoGraficoVista = 'OMNICANAL_BARRAS' | 'FINANZAS_PASTEL';
export type MetricaRanking = 'INGRESOS' | 'UNIDADES';
export type MetricaPastel = 'ESTRUCTURA' | 'CATEGORIAS' | 'METODOS';

@Component({
  selector: 'app-reporte-financiero-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, CurrencyPipe, DecimalPipe, DatePipe],
  templateUrl: './reporte-financiero-list.html',
  styleUrls: ['./reporte-financiero-list.css'],
})
export class ReporteFinancieroList implements OnInit, AfterViewInit, OnDestroy {
  private readonly reporteService = inject(ReporteFinancieroService);

  @ViewChild('omnicanalCanvas', { static: false }) omnicanalCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('productosCanvas', { static: false }) productosCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('pieCanvas', { static: false }) pieCanvas?: ElementRef<HTMLCanvasElement>;

  protected readonly dashboard = signal<DashboardFinanciero | null>(null);
  protected readonly omnicanal = signal<ReporteOmnicanal | null>(null);
  protected readonly semaforo = signal<ReporteSemaforoInventario | null>(null);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly exportando = signal(false);

  // Selector de Vistas: Barras o Pastel de Finanzas/Ganancias
  protected readonly vistaActiva = signal<TipoGraficoVista>('OMNICANAL_BARRAS');
  protected readonly metricaRanking = signal<MetricaRanking>('INGRESOS');
  protected readonly metricaPastel = signal<MetricaPastel>('ESTRUCTURA');

  // Filtros interactivos para las tablas inferiores
  protected readonly tabInferior = signal<'TODOS' | 'RANKING' | 'LOTES'>('TODOS');
  protected readonly busquedaRanking = signal<string>('');
  protected readonly busquedaLotes = signal<string>('');
  protected readonly filtroSemaforo = signal<'TODOS' | 'CRITICO' | 'PREVENTIVO' | 'OPTIMO'>('TODOS');

  // Computed: Ranking ABC Filtrado
  protected readonly rankingFiltrado = computed(() => {
    const oc = this.omnicanal();
    if (!oc || !oc.rankingProductos) return [];
    const q = this.busquedaRanking().toLowerCase().trim();
    if (!q) return oc.rankingProductos;
    return oc.rankingProductos.filter(
      (p) =>
        p.productoNombre.toLowerCase().includes(q) ||
        (p.categoria ?? '').toLowerCase().includes(q) ||
        (p.clasificacionAbc ?? '').toLowerCase().includes(q)
    );
  });

  // Computed: Lotes FEFO Filtrados
  protected readonly lotesFiltrados = computed(() => {
    const sf = this.semaforo();
    if (!sf || !sf.lotes) return [];
    const q = this.busquedaLotes().toLowerCase().trim();
    const filtro = this.filtroSemaforo();
    return sf.lotes.filter((l) => {
      const coincideTexto =
        !q ||
        l.codigoLote.toLowerCase().includes(q) ||
        l.productoNombre.toLowerCase().includes(q);
      const coincideEstado = filtro === 'TODOS' || l.estadoSemaforo === filtro;
      return coincideTexto && coincideEstado;
    });
  });

  // Computed: Margen Operativo %
  protected readonly margenOperativo = computed(() => {
    const d = this.dashboard();
    if (!d || d.totalIngresos <= 0) return 0;
    const margin = (d.balanceNeto / d.totalIngresos) * 100;
    return Number(margin.toFixed(1));
  });

  // Computed: Valorización en riesgo (lotes críticos + preventivos)
  protected readonly valorizacionEnRiesgo = computed(() => {
    const sf = this.semaforo();
    if (!sf || !sf.lotes) return 0;
    return sf.lotes
      .filter((l) => l.estadoSemaforo === 'CRITICO' || l.estadoSemaforo === 'PREVENTIVO')
      .reduce((acc, curr) => acc + (curr.valorizacionTotal || 0), 0);
  });

  private chartOmnicanal?: Chart;
  private chartProductos?: Chart;
  private chartPie?: Chart;

  ngOnInit(): void {
    this.cargarDatos();
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.actualizarGraficos(), 150);
  }

  ngOnDestroy(): void {
    this.destruirGraficos();
  }

  cargarDatos(): void {
    this.loading.set(true);
    this.error.set(null);
    this.omnicanal.set(null);
    this.destruirGraficos();

    // 1. Dashboard Financiero
    this.reporteService.obtenerDashboard().subscribe({
      next: (data) => {
        this.dashboard.set(data);
        this.loading.set(false);
        setTimeout(() => this.actualizarGraficos(), 100);
      },
      error: () => {
        this.error.set('No se pudo cargar el tablero analítico financiero.');
        this.loading.set(false);
      },
    });

    // 2. Ventas Omnicanal y Ranking ABC
    this.reporteService.obtenerReporteOmnicanal().subscribe({
      next: (data) => {
        this.omnicanal.set(data);
        setTimeout(() => this.actualizarGraficos(), 100);
      },
      error: (err) => {
        console.warn('Error al cargar métricas omnicanal:', err);
        this.omnicanal.set(null);
        this.error.set(err?.status === 403
          ? 'El reporte omnicanal requiere acceso de administrador o supervisor.'
          : 'No se pudo cargar el reporte omnicanal. Intenta nuevamente.');
      },
    });

    // 3. Semáforo FEFO de Lotes
    this.reporteService.obtenerReporteSemaforo().subscribe({
      next: (data) => {
        this.semaforo.set(data);
      },
      error: (err) => console.warn('Error semáforo:', err),
    });
  }

  cambiarVista(vista: TipoGraficoVista): void {
    this.vistaActiva.set(vista);
    setTimeout(() => this.actualizarGraficos(), 80);
  }

  cambiarMetrica(metrica: MetricaRanking): void {
    this.metricaRanking.set(metrica);
    setTimeout(() => this.renderizarGraficoProductos(), 50);
  }

  cambiarMetricaPastel(metrica: MetricaPastel): void {
    this.metricaPastel.set(metrica);
    setTimeout(() => this.renderizarGraficoPastelFinanzas(), 50);
  }

  private actualizarGraficos(): void {
    const vista = this.vistaActiva();
    if (vista === 'OMNICANAL_BARRAS') {
      this.renderizarGraficoOmnicanal();
      this.renderizarGraficoProductos();
    } else if (vista === 'FINANZAS_PASTEL') {
      this.renderizarGraficoPastelFinanzas();
    }
  }

  private renderizarGraficoOmnicanal(): void {
    if (!this.omnicanalCanvas?.nativeElement) return;
    const oc = this.omnicanal();
    if (!oc) return;

    if (this.chartOmnicanal) {
      this.chartOmnicanal.destroy();
    }

    const labels = oc.canales.map((c) => c.canalNombre);
    const montos = oc.canales.map((c) => c.totalVentas);
    const transacciones = oc.canales.map((c) => c.cantidadTransacciones);

    const ctx = this.omnicanalCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    this.chartOmnicanal = new Chart(ctx, {
      type: 'bar',
      data: {
        labels,
        datasets: [
          {
            label: 'Total Facturado (S/)',
            data: montos,
            backgroundColor: ['#7DD3FC', '#88B4CC', '#C8A0F0'],
            hoverBackgroundColor: ['#38BDF8', '#64748B', '#A855F7'],
            borderRadius: 8,
            borderSkipped: false,
            yAxisID: 'y',
          },
          {
            label: 'N° Pedidos / Ventas',
            data: transacciones,
            backgroundColor: '#1A2438',
            hoverBackgroundColor: '#0F172A',
            borderRadius: 6,
            borderSkipped: false,
            yAxisID: 'y1',
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'top',
            labels: {
              boxWidth: 12,
              usePointStyle: true,
              pointStyle: 'rectRounded',
              font: { family: "'Inter', sans-serif", weight: 600, size: 12 },
              color: '#1A2438',
            },
          },
          tooltip: {
            backgroundColor: '#1A2438',
            borderColor: '#334155',
            borderWidth: 1,
            titleColor: '#FFFFFF',
            bodyColor: '#E2E8F0',
            padding: 12,
            cornerRadius: 10,
            callbacks: {
              label: (context) => {
                if (context.datasetIndex === 0) {
                  return ` Ventas: S/ ${Number(context.raw).toFixed(2)}`;
                }
                return ` Transacciones: ${context.raw}`;
              },
            },
          },
        },
        scales: {
          y: {
            type: 'linear',
            position: 'left',
            ticks: {
              callback: (val) => `S/ ${val}`,
              font: { family: "'Inter', sans-serif", size: 11 },
              color: '#64748B',
            },
            grid: { color: '#F1F5F9' },
          },
          y1: {
            type: 'linear',
            position: 'right',
            grid: { drawOnChartArea: false },
            ticks: {
              precision: 0,
              font: { family: "'Inter', sans-serif", size: 11 },
              color: '#64748B',
            },
          },
          x: {
            grid: { display: false },
            ticks: {
              font: { family: "'Inter', sans-serif", weight: 500, size: 11 },
              color: '#1A2438',
            },
          },
        },
      },
    });
  }

  private renderizarGraficoProductos(): void {
    if (!this.productosCanvas?.nativeElement) return;
    const oc = this.omnicanal();
    if (!oc || !oc.rankingProductos || oc.rankingProductos.length === 0) return;

    if (this.chartProductos) {
      this.chartProductos.destroy();
    }

    const metrica = this.metricaRanking();
    const productos = oc.rankingProductos.slice(0, 5);
    const labels = productos.map((p) => p.productoNombre);

    let dataVals: number[] = [];
    let labelDs = '';
    let bgColor = '#7DD3FC';

    if (metrica === 'INGRESOS') {
      dataVals = productos.map((p) => p.totalIngresos);
      labelDs = 'Ingresos Totales (S/)';
      bgColor = '#7DD3FC';
    } else {
      dataVals = productos.map((p) => p.unidadesVendidas);
      labelDs = 'Unidades Vendidas';
      bgColor = '#88B4CC';
    }

    const ctx = this.productosCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    this.chartProductos = new Chart(ctx, {
      type: 'bar',
      data: {
        labels,
        datasets: [
          {
            label: labelDs,
            data: dataVals,
            backgroundColor: bgColor,
            borderRadius: 6,
          },
        ],
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#1A2438',
            borderColor: '#334155',
            borderWidth: 1,
            titleColor: '#FFFFFF',
            bodyColor: '#E2E8F0',
            padding: 10,
            cornerRadius: 8,
            callbacks: {
              label: (context) => {
                if (metrica === 'INGRESOS') return ` Ingresos: S/ ${Number(context.raw).toFixed(2)}`;
                return ` Unidades: ${context.raw}`;
              },
            },
          },
        },
        scales: {
          x: {
            ticks: {
              callback: (val) => {
                if (metrica === 'INGRESOS') return `S/ ${val}`;
                return `${val}`;
              },
              font: { family: "'Inter', sans-serif", size: 11 },
              color: '#64748B',
            },
            grid: { color: '#F1F5F9' },
          },
          y: {
            grid: { display: false },
            ticks: {
              font: { family: "'Inter', sans-serif", size: 11 },
              color: '#1A2438',
            },
          },
        },
      },
    });
  }

  private renderizarGraficoPastelFinanzas(): void {
    if (!this.pieCanvas?.nativeElement) return;
    const dash = this.dashboard();
    const oc = this.omnicanal();
    if (!dash) return;

    if (this.chartPie) {
      this.chartPie.destroy();
    }

    const tipo = this.metricaPastel();
    let labels: string[] = [];
    let dataVals: number[] = [];
    let bgColors: string[] = [];

    const paletaGlacier = [
      '#7DD3FC',
      '#88B4CC',
      '#C8A0F0',
      '#10B981',
      '#F59E0B',
      '#EF4444',
      '#38BDF8',
    ];

    if (tipo === 'ESTRUCTURA') {
      labels = ['Ingresos registrados', 'Egresos registrados'];
      dataVals = [dash.totalIngresos, dash.totalEgresos];
      bgColors = [paletaGlacier[0], paletaGlacier[5]];
    } else if (tipo === 'CATEGORIAS') {
      const porCategoria = new Map<string, number>();
      for (const producto of oc?.rankingProductos ?? []) {
        const categoria = producto.categoria ?? 'Sin categoría histórica';
        porCategoria.set(categoria, (porCategoria.get(categoria) ?? 0) + producto.totalIngresos);
      }
      labels = [...porCategoria.keys()];
      dataVals = [...porCategoria.values()];
      bgColors = labels.map((_, i) => paletaGlacier[i % paletaGlacier.length]);
    } else {
      labels = dash.resumenPorMetodo.map((m) => m.metodoPago);
      dataVals = dash.resumenPorMetodo.map((m) => m.totalIngresos);
      bgColors = labels.map((_, i) => paletaGlacier[i % paletaGlacier.length]);
    }

    const ctx = this.pieCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    const sliceOffsets = dataVals.map((_, i) => (i === 0 ? 12 : 0));

    this.chartPie = new Chart(ctx, {
      type: 'pie',
      data: {
        labels,
        datasets: [
          {
            data: dataVals,
            backgroundColor: bgColors,
            offset: sliceOffsets,
            hoverOffset: 16,
            borderWidth: 2,
            borderColor: '#ffffff',
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              usePointStyle: true,
              pointStyle: 'rectRounded',
              padding: 16,
              font: { family: "'Inter', sans-serif", size: 12, weight: 600 },
              color: '#1A2438',
            },
          },
          tooltip: {
            backgroundColor: '#1A2438',
            borderColor: '#334155',
            borderWidth: 1,
            titleColor: '#ffffff',
            bodyColor: '#ffffff',
            padding: 12,
            cornerRadius: 10,
            boxPadding: 6,
            usePointStyle: true,
            callbacks: {
              label: (context) => {
                const val = Number(context.raw);
                const total = context.dataset.data.reduce((a, b) => Number(a) + Number(b), 0);
                const pct = total > 0 ? ((val / total) * 100).toFixed(1) : '0.0';
                return ` ${context.label}: S/ ${val.toFixed(2)} (${pct}%)`;
              },
            },
          },
        },
      },
    });
  }

  private destruirGraficos(): void {
    this.chartOmnicanal?.destroy();
    this.chartProductos?.destroy();
    this.chartPie?.destroy();
    this.chartOmnicanal = undefined;
    this.chartProductos = undefined;
    this.chartPie = undefined;
  }

  calcularPorcentaje(monto: number): number {
    const dash = this.dashboard();
    if (!dash || dash.totalIngresos === 0) return 0;
    return Math.min(100, Math.round((monto / dash.totalIngresos) * 100));
  }

  async descargarReporteActiva(formato: 'PDF' | 'EXCEL'): Promise<void> {
    const dash = this.dashboard();
    if (!dash || !dash.sesionActivaId) {
      alert('No hay un turno de caja activo para exportar.');
      return;
    }

    this.exportando.set(true);

    this.reporteService.descargarCierreCaja(dash.sesionActivaId, formato).subscribe({
      next: async (blob) => {
        const ext = formato === 'PDF' ? 'pdf' : 'xlsx';
        const nombreArchivo = `corte-caja-sesion-${dash.sesionActivaId}.${ext}`;

        if ('showSaveFilePicker' in window) {
          try {
            const pickerOpts = {
              suggestedName: nombreArchivo,
              types: [
                formato === 'PDF'
                  ? {
                      description: 'Documento PDF (*.pdf)',
                      accept: { 'application/pdf': ['.pdf'] },
                    }
                  : {
                      description: 'Hoja de Cálculo Excel (*.xlsx)',
                      accept: {
                        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': ['.xlsx'],
                      },
                    },
              ],
            };

            const handle = await (window as any).showSaveFilePicker(pickerOpts);
            const writable = await handle.createWritable();
            await writable.write(blob);
            await writable.close();
            this.exportando.set(false);
            return;
          } catch (err: any) {
            if (err.name === 'AbortError') {
              this.exportando.set(false);
              return;
            }
          }
        }

        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = nombreArchivo;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        this.exportando.set(false);
      },
      error: () => {
        alert('Error al descargar el archivo de reporte.');
        this.exportando.set(false);
      },
    });
  }
}
