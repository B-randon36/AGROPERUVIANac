import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { InventarioService } from './inventario-service';
import { Almacen, AlmacenRequest } from './inventario.model';

@Component({
  selector: 'app-almacen-list',
  imports: [CommonModule, RouterLink, ReactiveFormsModule, DecimalPipe],
  template: `
    <div class="feature-container">
      <div class="page-header">
        <div>
          <div class="badge badge-info">INVENTARIO & ALMACENES FÍSICOS</div>
          <h1>Gestión de Almacenes y Capacidad Nominal</h1>
          <p class="subtitle">Monitoreo de ocupación volumétrica, control de cuartos limpios y bodegas HACCP</p>
        </div>
        <div class="header-actions">
          <button class="btn btn-primary" (click)="abrirModalNuevo()">
            <span class="material-symbols-outlined icon-inline">add_circle</span> Nuevo Almacén
          </button>
          <a routerLink="/inventario/lotes" class="btn btn-secondary">← Lotes FEFO</a>
          <button class="btn btn-secondary" (click)="cargarDatos()" title="Actualizar">
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
                <th>Almacén / Ubicación</th>
                <th>Tipo</th>
                <th style="width: 25%;">Capacidad & Ocupación</th>
                <th>Responsable</th>
                <th>Nivel</th>
                <th class="text-center">Acciones</th>
              </tr>
            </thead>
            <tbody>
              @for (a of almacenes(); track a.id) {
                <tr>
                  <td><code>{{ a.codigo }}</code></td>
                  <td>
                    <strong>{{ a.nombre }}</strong>
                    <div style="font-size: 0.78rem; color: var(--text-muted);">{{ a.ubicacion }}</div>
                  </td>
                  <td><span class="badge badge-secondary">{{ a.tipo }}</span></td>
                  <td>
                    <div style="display: flex; justify-content: space-between; font-size: 0.8rem; margin-bottom: 0.25rem;">
                      <span>{{ a.ocupacionActual | number:'1.0-0' }} / {{ a.capacidadMaxima | number:'1.0-0' }} u.</span>
                      <strong>{{ a.porcentajeOcupacion }}%</strong>
                    </div>
                    <div class="progress-bar-bg">
                      <div
                        class="progress-bar-fill"
                        [style.width.%]="a.porcentajeOcupacion"
                        [class.bg-success]="a.nivelOcupacion === 'OPTIMO'"
                        [class.bg-warning]="a.nivelOcupacion === 'ALTO'"
                        [class.bg-danger]="a.nivelOcupacion === 'CRITICO'"
                      ></div>
                    </div>
                  </td>
                  <td>{{ a.responsable || 'Sin asignar' }}</td>
                  <td>
                    <span class="badge"
                      [class.badge-success]="a.nivelOcupacion === 'OPTIMO'"
                      [class.badge-warning]="a.nivelOcupacion === 'ALTO'"
                      [class.badge-danger]="a.nivelOcupacion === 'CRITICO'">
                      {{ a.nivelOcupacion }}
                    </span>
                  </td>
                  <td class="text-center">
                    <button class="btn btn-danger btn-sm" (click)="eliminar(a)">
                      <span class="material-symbols-outlined icon-inline" style="font-size: 1rem;">delete</span>
                    </button>
                  </td>
                </tr>
              } @empty {
                @if (!loading()) {
                  <tr>
                    <td colspan="7" class="text-center py-4">No hay almacenes registrados.</td>
                  </tr>
                }
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Modal Nuevo Almacén -->
    @if (modalNuevo()) {
      <div class="modal-backdrop">
        <div class="modal-content" style="max-width: 620px;">
          <div class="modal-header">
            <h3>Registrar Almacén Físico</h3>
            <button class="modal-close" (click)="modalNuevo.set(false)">×</button>
          </div>
          <form [formGroup]="formNuevo" (ngSubmit)="guardarNuevo()">
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; margin-bottom: 0.75rem;">
              <div class="form-group">
                <label class="form-label">Código Almacén *</label>
                <input type="text" formControlName="codigo" class="form-control" placeholder="Ej: ALM-SEC-05" />
              </div>
              <div class="form-group">
                <label class="form-label">Tipo de Almacén</label>
                <select formControlName="tipo" class="form-control">
                  <option value="PRODUCTO_TERMINADO">PRODUCTO_TERMINADO</option>
                  <option value="MATERIA_PRIMA">MATERIA_PRIMA</option>
                  <option value="ENVASES_EMPAQUES">ENVASES_EMPAQUES</option>
                  <option value="CAMARA_DESPACHO">CAMARA_DESPACHO</option>
                </select>
              </div>
            </div>

            <div class="form-group mb-3">
              <label class="form-label">Nombre del Almacén *</label>
              <input type="text" formControlName="nombre" class="form-control" placeholder="Ej: Cámara Fría N° 2" />
            </div>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; margin-bottom: 0.75rem;">
              <div class="form-group">
                <label class="form-label">Capacidad Máxima *</label>
                <input type="number" formControlName="capacidadMaxima" class="form-control" />
              </div>
              <div class="form-group">
                <label class="form-label">Ocupación Inicial</label>
                <input type="number" formControlName="ocupacionActual" class="form-control" />
              </div>
            </div>

            <div class="form-group mb-3">
              <label class="form-label">Ubicación Física</label>
              <input type="text" formControlName="ubicacion" class="form-control" placeholder="Ej: Planta Pisco - Muelle 2" />
            </div>

            <div class="form-group mb-3">
              <label class="form-label">Responsable Sanitario / Logística</label>
              <input type="text" formControlName="responsable" class="form-control" placeholder="Ej: Ing. Juan Pérez" />
            </div>

            <div class="modal-footer">
              <button type="button" class="btn btn-secondary" (click)="modalNuevo.set(false)">Cancelar</button>
              <button type="submit" class="btn btn-primary" [disabled]="loading()">Guardar Almacén</button>
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
    .progress-bar-bg { width: 100%; height: 8px; background: #e2e8f0; border-radius: 9999px; overflow: hidden; }
    .progress-bar-fill { height: 100%; border-radius: 9999px; transition: width 0.3s ease; }
    .bg-success { background-color: #10b981; }
    .bg-warning { background-color: #f59e0b; }
    .bg-danger { background-color: #ef4444; }
  `]
})
export class AlmacenList implements OnInit {
  private readonly inventarioService = inject(InventarioService);
  private readonly fb = inject(FormBuilder);

  protected readonly almacenes = signal<Almacen[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly successMsg = signal<string | null>(null);
  protected readonly modalNuevo = signal(false);

  protected readonly formNuevo = this.fb.nonNullable.group({
    codigo: ['', [Validators.required]],
    nombre: ['', [Validators.required]],
    ubicacion: [''],
    tipo: ['PRODUCTO_TERMINADO'],
    capacidadMaxima: [5000, [Validators.required, Validators.min(1)]],
    ocupacionActual: [0, [Validators.min(0)]],
    responsable: [''],
    activo: [true],
  });

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.loading.set(true);
    this.error.set(null);
    this.inventarioService.listarAlmacenes().subscribe({
      next: (data) => {
        this.almacenes.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el listado de almacenes.');
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

    const req: AlmacenRequest = {
      codigo: val.codigo,
      nombre: val.nombre,
      ubicacion: val.ubicacion,
      tipo: val.tipo,
      capacidadMaxima: Number(val.capacidadMaxima),
      ocupacionActual: Number(val.ocupacionActual),
      responsable: val.responsable,
      activo: val.activo,
    };

    this.inventarioService.crearAlmacen(req).subscribe({
      next: () => {
        this.successMsg.set(`Almacén ${req.codigo} creado.`);
        this.modalNuevo.set(false);
        this.cargarDatos();
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Error al guardar el almacén.');
        this.loading.set(false);
      },
    });
  }

  eliminar(a: Almacen): void {
    if (!confirm(`¿Eliminar almacén "${a.nombre}"?`)) return;
    this.inventarioService.eliminarAlmacen(a.id).subscribe({
      next: () => {
        this.successMsg.set(`Almacén ${a.codigo} eliminado.`);
        this.cargarDatos();
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Error al eliminar el almacén.');
      },
    });
  }
}

