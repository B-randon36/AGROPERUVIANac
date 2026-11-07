import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { RendicionCajaChicaService } from './rendicion-caja-chica-service';
import {
  AsignacionCajaCentroCosto,
  AsignarPresupuestoRequest,
  CajaChica,
  CentroCosto,
  EstadoRendicion,
  RendicionCajaChica,
  ReporteRendicionesAgregado,
} from './rendicion-caja-chica.model';

@Component({
  selector: 'app-rendicion-caja-chica-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './rendicion-caja-chica-list.html',
  styleUrls: ['./rendicion-caja-chica-list.css'],
})
export class RendicionCajaChicaList implements OnInit {
  private readonly rendicionService = inject(RendicionCajaChicaService);

  // Señales de Estado Principal
  protected readonly rendiciones = signal<RendicionCajaChica[]>([]);
  protected readonly cajasChicas = signal<CajaChica[]>([]);
  protected readonly reporteAgregado = signal<ReporteRendicionesAgregado | null>(null);
  protected readonly loading = signal(false);
  protected readonly loadingAnular = signal(false);

  // Mensajes de Alerta
  protected readonly mensajeExito = signal<string | null>(null);
  protected readonly mensajeError = signal<string | null>(null);

  // Filtros de Búsqueda
  protected filtroEstado = signal<string>('');
  protected filtroCajaChicaId = signal<number | null>(null);
  protected filtroFechaInicio = signal<string>('');
  protected filtroFechaFin = signal<string>('');

  // Control de Visualización Detallada (Modal de Detalle Profundo)
  protected rendicionSeleccionada = signal<RendicionCajaChica | null>(null);
  protected mostrarModalDetalle = signal(false);

  // Control de Transición de Estado (Modal de Anulación)
  protected rendicionParaAnular = signal<RendicionCajaChica | null>(null);
  protected mostrarModalAnular = signal(false);

  // Gestión de Fondos Fijos (Cajas Chicas)
  protected mostrarModalCajaChica = signal(false);
  protected nuevaCajaCodigo = signal('');
  protected nuevaCajaResponsable = signal('');
  protected nuevaCajaLimite = signal(1000);
  protected nuevaCajaSaldo = signal(1000);
  protected loadingCrearCaja = signal(false);
  protected readonly presupuestosPorCaja = signal<Record<number, AsignacionCajaCentroCosto[]>>({});

  // Gestión de Presupuestos por Centro de Costo (M:N AsignacionCajaCentroCosto)
  protected readonly centrosCosto = signal<CentroCosto[]>([]);
  protected readonly asignaciones = signal<AsignacionCajaCentroCosto[]>([]);
  protected readonly cajaParaPresupuesto = signal<CajaChica | null>(null);
  protected readonly mostrarModalPresupuestos = signal(false);
  protected readonly formPresupuestoCentroCostoId = signal<number | null>(null);
  protected readonly formPresupuestoMonto = signal<number>(500);
  protected readonly loadingPresupuestos = signal(false);
  protected readonly mensajeExitoPresupuesto = signal<string | null>(null);

  // Modal de Creación de Nuevo Centro de Costo Maestro
  protected readonly mostrarModalNuevoCC = signal(false);
  protected readonly nuevoCcCodigo = signal('');
  protected readonly nuevoCcNombre = signal('');
  protected readonly loadingCrearCC = signal(false);
  protected readonly errorCrearCC = signal<string | null>(null);
  protected readonly exitoCrearCC = signal<string | null>(null);

  ngOnInit(): void {
    this.cargarCajasChicas();
    this.aplicarFiltros();
    this.cargarReporteAgregado();
  }

  cargarCajasChicas(): void {
    this.rendicionService.listarCajasChicas().subscribe({
      next: (data) => {
        this.cajasChicas.set(data);
        data.forEach((c) => {
          this.rendicionService.listarPresupuestosCaja(c.id).subscribe({
            next: (asigs) => {
              this.presupuestosPorCaja.update((map) => ({ ...map, [c.id]: asigs }));
            },
          });
        });
      },
      error: () => console.warn('No se pudieron cargar las cajas chicas'),
    });
  }

  cargarReporteAgregado(): void {
    this.rendicionService.obtenerReporteAgregado(this.filtroCajaChicaId()).subscribe({
      next: (data) => this.reporteAgregado.set(data),
      error: () => console.warn('No se pudo cargar el reporte agregado'),
    });
  }

  aplicarFiltros(): void {
    this.loading.set(true);
    this.mensajeError.set(null);

    this.rendicionService
      .buscarConFiltros({
        cajaChicaId: this.filtroCajaChicaId() || null,
        estado: this.filtroEstado() || null,
        fechaInicio: this.filtroFechaInicio() || null,
        fechaFin: this.filtroFechaFin() || null,
        size: 50,
      })
      .subscribe({
        next: (page) => {
          this.rendiciones.set(page.content);
          this.loading.set(false);
          this.cargarReporteAgregado();
        },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          this.mensajeError.set(
            err.error?.message || 'Error al filtrar las rendiciones de caja chica.'
          );
        },
      });
  }

  limpiarFiltros(): void {
    this.filtroEstado.set('');
    this.filtroCajaChicaId.set(null);
    this.filtroFechaInicio.set('');
    this.filtroFechaFin.set('');
    this.aplicarFiltros();
  }

  onCajaChicaChange(event: Event): void {
    const val = (event.target as HTMLSelectElement).value;
    this.filtroCajaChicaId.set(val ? Number(val) : null);
  }

  // --- Modal de Consulta y Desglose de Detalle Completo ---

  verDetalle(rendicion: RendicionCajaChica): void {
    // Si la rendición viene sin detalles o para asegurar el detalle completo, podemos consultar
    this.rendicionService.obtenerRendicion(rendicion.id).subscribe({
      next: (r) => {
        this.rendicionSeleccionada.set(r);
        this.mostrarModalDetalle.set(true);
      },
      error: () => {
        this.rendicionSeleccionada.set(rendicion);
        this.mostrarModalDetalle.set(true);
      },
    });
  }

  cerrarModalDetalle(): void {
    this.mostrarModalDetalle.set(false);
    this.rendicionSeleccionada.set(null);
  }

  // --- Transición de Estado con Regla de Negocio (Anular) ---

  abrirModalAnular(rendicion: RendicionCajaChica): void {
    if (rendicion.estado === 'ANULADO') {
      this.mensajeError.set('La rendición ya se encuentra en estado ANULADO.');
      return;
    }
    this.rendicionParaAnular.set(rendicion);
    this.mostrarModalAnular.set(true);
  }

  cerrarModalAnular(): void {
    this.mostrarModalAnular.set(false);
    this.rendicionParaAnular.set(null);
  }

  confirmarAnulacion(): void {
    const item = this.rendicionParaAnular();
    if (!item) return;

    this.loadingAnular.set(true);
    this.mensajeError.set(null);
    this.mensajeExito.set(null);

    this.rendicionService.anularRendicion(item.id).subscribe({
      next: (anulada) => {
        this.loadingAnular.set(false);
        this.cerrarModalAnular();
        this.mensajeExito.set(
          `¡Rendición ${anulada.codigo} ANULADA con éxito! Se restituyeron S/ ${anulada.montoTotal.toFixed(2)} al saldo de la Caja Chica.`
        );
        this.aplicarFiltros();
        this.cargarCajasChicas();
      },
      error: (err: HttpErrorResponse) => {
        this.loadingAnular.set(false);
        this.cerrarModalAnular();
        const status = err.status;
        const msg = err.error?.message || '';

        // Manejo específico del rechazo de reintento / idempotencia
        if (status === 409 || msg.includes('ANULADO') || err.error?.reglaNegocio === 'RN-FIN-02') {
          this.mensajeError.set(
            `REGLA DE NEGOCIO RN-FIN-02: ${msg || 'La transacción ya fue anulada previamente y no puede repetirse.'}`
          );
        } else {
          this.mensajeError.set(msg || 'No se pudo anular la rendición seleccionada.');
        }
      },
    });
  }

  getCajaNombre(cajaChicaId: number): string {
    const caja = this.cajasChicas().find((c) => c.id === cajaChicaId);
    return caja ? `${caja.codigo} (${caja.responsable})` : `Caja #${cajaChicaId}`;
  }

  getEstadoClass(estado: EstadoRendicion): string {
    switch (estado) {
      case 'APROBADO':
        return 'badge-success';
      case 'PENDIENTE':
        return 'badge-warning';
      case 'ANULADO':
        return 'badge-danger';
      case 'RECHAZADO':
        return 'badge-secondary';
      default:
        return 'badge-secondary';
    }
  }

  abrirModalCajaChica(): void {
    const nextNum = this.cajasChicas().length + 1;
    this.nuevaCajaCodigo.set(`CCH-2026-00${nextNum}`);
    this.nuevaCajaResponsable.set('');
    this.nuevaCajaLimite.set(1000);
    this.nuevaCajaSaldo.set(1000);
    this.mostrarModalCajaChica.set(true);
  }

  cerrarModalCajaChica(): void {
    this.mostrarModalCajaChica.set(false);
  }

  guardarCajaChica(): void {
    const codigo = this.nuevaCajaCodigo().trim();
    const responsable = this.nuevaCajaResponsable().trim();
    const limite = this.nuevaCajaLimite();
    const saldo = this.nuevaCajaSaldo();

    if (!codigo || !responsable || limite <= 0 || saldo < 0) {
      this.mensajeError.set('Por favor completa todos los campos válidos para la Caja Chica.');
      return;
    }

    this.loadingCrearCaja.set(true);
    this.mensajeError.set(null);

    this.rendicionService
      .crearCajaChica({
        codigo,
        responsable,
        limiteMaximo: limite,
        saldoInicial: saldo,
      })
      .subscribe({
        next: (caja) => {
          this.mensajeExito.set(`Caja Chica ${caja.codigo} (${caja.responsable}) creada con éxito.`);
          this.mostrarModalCajaChica.set(false);
          this.loadingCrearCaja.set(false);
          this.cargarCajasChicas();
        },
        error: (err) => {
          this.mensajeError.set(err?.error?.message || 'Error al crear la caja chica.');
          this.loadingCrearCaja.set(false);
        },
      });
  }

  // --- Gestión de Presupuestos por Centro de Costo (M:N) ---

  abrirModalCentroCosto(): void {
    this.nuevoCcCodigo.set('');
    this.nuevoCcNombre.set('');
    this.errorCrearCC.set(null);
    this.exitoCrearCC.set(null);
    this.mostrarModalNuevoCC.set(true);
    if (this.centrosCosto().length === 0) {
      this.cargarCentrosCosto();
    }
  }

  cerrarModalCentroCosto(): void {
    this.mostrarModalNuevoCC.set(false);
    this.errorCrearCC.set(null);
    this.exitoCrearCC.set(null);
  }

  cargarCentrosCosto(seleccionarId?: number): void {
    this.rendicionService.listarCentrosCosto().subscribe({
      next: (ccs) => {
        this.centrosCosto.set(ccs);
        if (seleccionarId) {
          this.formPresupuestoCentroCostoId.set(seleccionarId);
        } else if (ccs.length > 0 && !this.formPresupuestoCentroCostoId()) {
          this.formPresupuestoCentroCostoId.set(ccs[0].id);
        }
      },
      error: () => console.warn('Error al cargar centros de costo'),
    });
  }

  guardarNuevoCentroCosto(): void {
    const codigo = this.nuevoCcCodigo().trim().toUpperCase();
    const nombre = this.nuevoCcNombre().trim();

    if (!codigo || !nombre) {
      this.errorCrearCC.set('Por favor completa el código y el nombre del Centro de Costo.');
      return;
    }

    this.loadingCrearCC.set(true);
    this.errorCrearCC.set(null);
    this.exitoCrearCC.set(null);

    this.rendicionService.crearCentroCosto({ codigoCc: codigo, nombreArea: nombre }).subscribe({
      next: (nuevoCC) => {
        this.loadingCrearCC.set(false);
        this.exitoCrearCC.set(`¡Centro de Costo "${nuevoCC.nombreArea}" creado con éxito!`);
        this.nuevoCcCodigo.set('');
        this.nuevoCcNombre.set('');
        this.cargarCentrosCosto(nuevoCC.id);
        setTimeout(() => {
          this.cerrarModalCentroCosto();
        }, 1200);
      },
      error: (err: HttpErrorResponse) => {
        this.loadingCrearCC.set(false);
        this.errorCrearCC.set(err?.error?.message || 'Error al registrar el Centro de Costo.');
      },
    });
  }

  abrirGestionPresupuestos(caja: CajaChica): void {
    this.cajaParaPresupuesto.set(caja);
    this.mensajeExitoPresupuesto.set(null);
    this.mostrarModalPresupuestos.set(true);
    this.cargarPresupuestosCaja(caja.id);

    if (this.centrosCosto().length === 0) {
      this.cargarCentrosCosto();
    }
  }

  cargarPresupuestosCaja(cajaId: number): void {
    this.loadingPresupuestos.set(true);
    this.rendicionService.listarPresupuestosCaja(cajaId).subscribe({
      next: (asigs) => {
        this.asignaciones.set(asigs);
        this.loadingPresupuestos.set(false);
      },
      error: () => {
        this.loadingPresupuestos.set(false);
      },
    });
  }

  cerrarModalPresupuestos(): void {
    this.mostrarModalPresupuestos.set(false);
    this.cajaParaPresupuesto.set(null);
    this.mensajeExitoPresupuesto.set(null);
  }

  guardarPresupuesto(): void {
    const caja = this.cajaParaPresupuesto();
    const ccId = this.formPresupuestoCentroCostoId();
    const monto = this.formPresupuestoMonto();

    if (!caja || !ccId || monto <= 0) return;

    this.loadingPresupuestos.set(true);
    this.mensajeExitoPresupuesto.set(null);

    this.rendicionService
      .asignarPresupuesto(caja.id, { centroCostoId: Number(ccId), montoPresupuesto: Number(monto) })
      .subscribe({
        next: (asig) => {
          this.mensajeExitoPresupuesto.set(
            `Presupuesto asignado a ${asig.codigoCc} (${asig.nombreArea}): S/ ${asig.presupuestoAsignado.toFixed(2)}`
          );
          this.cargarPresupuestosCaja(caja.id);
          this.cargarCajasChicas();
        },
        error: (err) => {
          this.mensajeError.set(err?.error?.message || 'Error al asignar presupuesto.');
          this.loadingPresupuestos.set(false);
        },
      });
  }

  // --- Helpers Visuales Semafóricos para Usabilidad ---
  getConsumoPorcentaje(asig: AsignacionCajaCentroCosto): number {
    if (!asig.presupuestoAsignado || asig.presupuestoAsignado <= 0) return 0;
    const consumido = asig.presupuestoAsignado - asig.saldoDisponible;
    return Math.max(0, Math.min(100, (consumido / asig.presupuestoAsignado) * 100));
  }

  getSemaforoColorClass(asig: AsignacionCajaCentroCosto): string {
    const pct = this.getConsumoPorcentaje(asig);
    if (pct >= 90 || asig.saldoDisponible <= 0) return 'semaforo-rojo';
    if (pct >= 75) return 'semaforo-ambar';
    return 'semaforo-celeste';
  }
}
