import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProduccionService } from './produccion.service';
import { CentroTrabajo, CentroTrabajoRequest } from './produccion.model';

@Component({
  selector: 'app-centro-trabajo-list',
  imports: [CommonModule, RouterLink, ReactiveFormsModule, DecimalPipe],
  template: `
    <div class="feature-container">
      <!-- NAVEGACIÓN ESTANDARIZADA & BREADCRUMB (Pilar 1) -->
      <div class="view-back-nav">
        <a routerLink="/produccion" class="btn-back-nav">
          <span class="material-symbols-outlined" style="font-size: 1.1rem;">arrow_back</span>
          Volver a Órdenes
        </a>
        <div class="breadcrumb-trail">
          <a routerLink="/produccion">Producción & Planta</a>
          <span class="separator">/</span>
          <span class="current">Centros de Trabajo</span>
        </div>
      </div>

      <div class="page-header">
        <div>
          <div class="badge badge-warning">PRODUCCIÓN & PLANTA</div>
          <h1>Centros de Trabajo & Líneas Operativas</h1>
          <p class="subtitle">Capacidades nominales, tarifas operativas por hora y monitoreo de líneas de procesamiento</p>
        </div>
        <div class="header-actions">
          <button class="btn btn-glacier-primary" (click)="abrirModalNuevo()">
            <span class="material-symbols-outlined icon-inline">add_circle</span> + Nuevo Centro de Trabajo
          </button>
          <a routerLink="/produccion" class="btn btn-neutral-secondary">← Órdenes de Planta</a>
          <button class="btn btn-neutral-secondary" (click)="cargarDatos()" title="Actualizar">
            <span class="material-symbols-outlined" style="vertical-align: middle;">refresh</span>
          </button>
        </div>
      </div>

      @if (error()) {
        <div class="alert alert-danger" style="display: flex; align-items: center; gap: 0.5rem;">
          <span class="material-symbols-outlined">warning</span>
          <span>{{ error() }}</span>
        </div>
      }
      @if (successMsg()) {
        <div class="alert alert-success" style="display: flex; align-items: center; gap: 0.5rem;">
          <span class="material-symbols-outlined">check_circle</span>
          <span>{{ successMsg() }}</span>
        </div>
      }

      <div class="card">
        <div class="table-container">
          <table>
            <thead>
              <tr>
                <th>Código</th>
                <th>Nombre de la Línea</th>
                <th>Capacidad Nominal</th>
                <th>Costo / Hora</th>
                <th>Estado</th>
                <th class="text-center">Acciones</th>
              </tr>
            </thead>
            <tbody>
              @for (c of centros(); track c.id) {
                <tr>
                  <td><code>{{ c.codigo }}</code></td>
                  <td>
                    <strong>{{ c.nombre }}</strong>
                    <div style="font-size: 0.78rem; color: var(--text-muted);">{{ c.descripcion }}</div>
                  </td>
                  <td>{{ c.capacidad ? (c.capacidad | number:'1.1-2') + ' ' + (c.unidadCapacidad || 'UNID/HORA') : '—' }}</td>
                  <td><strong>S/ {{ c.costoHora | number:'1.2-2' }}</strong></td>
                  <td>
                    <span class="badge" [class.badge-success]="c.activo" [class.badge-danger]="!c.activo">
                      {{ c.activo ? 'OPERATIVO' : 'EN MANTENIMIENTO' }}
                    </span>
                  </td>
                  <td class="text-center">
                    <button class="btn btn-danger btn-sm" (click)="eliminar(c)">
                      <span class="material-symbols-outlined icon-inline" style="font-size: 1rem;">delete</span>
                    </button>
                  </td>
                </tr>
              } @empty {
                @if (!loading()) {
                  <tr>
                    <td colspan="6" class="text-center py-4">No hay centros de trabajo registrados.</td>
                  </tr>
                }
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Modal Nuevo Centro de Trabajo -->
    @if (modalNuevo()) {
      <div class="modal-backdrop">
        <div class="modal-content" style="max-width: 600px;">
          <div class="modal-header">
            <h3>Registrar Centro de Trabajo</h3>
            <button class="modal-close" (click)="modalNuevo.set(false)">×</button>
          </div>
          <form [formGroup]="formNuevo" (ngSubmit)="guardarNuevo()">
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; margin-bottom: 0.75rem;">
              <div class="form-group">
                <label class="form-label">Código *</label>
                <input type="text" formControlName="codigo" class="form-control" placeholder="Ej: CT-EXTR-05" />
              </div>
              <div class="form-group">
                <label class="form-label">Costo por Hora (S/) *</label>
                <input type="number" step="0.5" formControlName="costoHora" class="form-control" />
              </div>
            </div>

            <div class="form-group mb-3">
              <label class="form-label">Nombre del Centro / Línea de Producción *</label>
              <input type="text" formControlName="nombre" class="form-control" placeholder="Ej: Línea de Centrifugado y Clarificado" />
            </div>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; margin-bottom: 0.75rem;">
              <div class="form-group">
                <label class="form-label">Capacidad Nominal</label>
                <input type="number" formControlName="capacidad" class="form-control" />
              </div>
              <div class="form-group">
                <label class="form-label">Unidad de Capacidad</label>
                <select formControlName="unidadCapacidad" class="form-control">
                  <option value="LITROS/HORA">LITROS/HORA</option>
                  <option value="KG/HORA">KG/HORA</option>
                  <option value="UNID/HORA">UNID/HORA</option>
                </select>
              </div>
            </div>

            <div class="form-group mb-3">
              <label class="form-label">Descripción Técnica</label>
              <textarea formControlName="descripcion" rows="2" class="form-control"></textarea>
            </div>

            <div class="modal-footer">
              <button type="button" class="btn btn-secondary" (click)="modalNuevo.set(false)">Cancelar</button>
              <button type="submit" class="btn btn-primary" [disabled]="loading()">Guardar Línea</button>
            </div>
          </form>
        </div>
      </div>
    }
  `,
  styles: [`
    .feature-container { display: flex; flex-direction: column; gap: 1.5rem; }
    .page-header { display: flex; justify-content: space-between; align-items: flex-start; }
    .page-header h1 { font-size: 1.65rem; margin-top: 0.25rem; }
    .subtitle { color: var(--text-muted); font-size: 0.85rem; }
    .header-actions { display: flex; gap: 0.75rem; }
    .mb-3 { margin-bottom: 1rem; }
  `]
})
export class CentroTrabajoList implements OnInit {
  private readonly produccionService = inject(ProduccionService);
  private readonly fb = inject(FormBuilder);

  protected readonly centros = signal<CentroTrabajo[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly successMsg = signal<string | null>(null);
  protected readonly modalNuevo = signal(false);

  protected readonly formNuevo = this.fb.nonNullable.group({
    codigo: ['', [Validators.required]],
    nombre: ['', [Validators.required]],
    descripcion: [''],
    capacidad: [200],
    unidadCapacidad: ['KG/HORA'],
    costoHora: [45.0, [Validators.required, Validators.min(0)]],
    activo: [true],
  });

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.loading.set(true);
    this.error.set(null);
    this.produccionService.listarCentrosTrabajo().subscribe({
      next: (data) => {
        this.centros.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el listado de centros de trabajo.');
        this.loading.set(false);
      },
    });
  }

  abrirModalNuevo(): void {
    this.modalNuevo.set(true);
  }

  guardarNuevo(): void {
    if (this.formNuevo.invalid) return;
    this.loading.set(true);
    const val = this.formNuevo.getRawValue();

    const req: CentroTrabajoRequest = {
      codigo: val.codigo,
      nombre: val.nombre,
      descripcion: val.descripcion,
      capacidad: val.capacidad,
      unidadCapacidad: val.unidadCapacidad,
      costoHora: Number(val.costoHora),
      activo: val.activo,
    };

    this.produccionService.crearCentroTrabajo(req).subscribe({
      next: () => {
        this.successMsg.set(`Centro de trabajo ${req.codigo} creado.`);
        this.modalNuevo.set(false);
        this.cargarDatos();
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Error al guardar el centro de trabajo.');
        this.loading.set(false);
      },
    });
  }

  eliminar(c: CentroTrabajo): void {
    if (!confirm(`¿Eliminar centro de trabajo "${c.nombre}"?`)) return;
    this.produccionService.eliminarCentroTrabajo(c.id).subscribe({
      next: () => {
        this.successMsg.set(`Centro ${c.codigo} eliminado.`);
        this.cargarDatos();
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Error al eliminar el centro de trabajo.');
      },
    });
  }
}

