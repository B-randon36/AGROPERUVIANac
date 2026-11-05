import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProduccionService } from './produccion.service';
import { ListaMateriales, ListaMaterialesRequest } from './produccion.model';
import { ProductoService } from '../catalogo/producto/producto.service';
import { Producto } from '../catalogo/producto/producto.model';

@Component({
  selector: 'app-lista-materiales-list',
  imports: [CommonModule, RouterLink, ReactiveFormsModule, DecimalPipe],
  template: `
    <div class="feature-container">
      <div class="page-header">
        <div>
          <div class="badge badge-warning">PRODUCCIÓN & RECETAS BOM</div>
          <h1>Recetas Maestras Bill of Materials (BOM)</h1>
          <p class="subtitle">Estructura persistente de materiales, mermas y componentes estándar por producto terminado</p>
        </div>
        <div class="header-actions">
          <button class="btn btn-primary" (click)="abrirModalNueva()">
            <span class="material-symbols-outlined icon-inline">add_circle</span> Nueva Receta BOM
          </button>
          <a routerLink="/produccion" class="btn btn-secondary">← Órdenes de Planta</a>
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
                <th>Código BOM</th>
                <th>Producto Terminado</th>
                <th>Cant. Base</th>
                <th>Insumos</th>
                <th>Estado</th>
                <th class="text-center">Acciones</th>
              </tr>
            </thead>
            <tbody>
              @for (rec of recetas(); track rec.id) {
                <tr>
                  <td><code>{{ rec.codigo }}</code></td>
                  <td><strong>{{ rec.nombreProducto }}</strong></td>
                  <td>{{ rec.cantidadBase }} u.</td>
                  <td>
                    <span class="badge badge-info">{{ rec.detalles.length }} componentes</span>
                  </td>
                  <td>
                    <span class="badge" [class.badge-success]="rec.activo" [class.badge-danger]="!rec.activo">
                      {{ rec.activo ? 'ACTIVA' : 'INACTIVA' }}
                    </span>
                  </td>
                  <td class="text-center">
                    <div class="btn-group">
                      <button class="btn btn-secondary btn-sm" (click)="verDetalle(rec)">
                        <span class="material-symbols-outlined icon-inline" style="font-size: 1rem;">visibility</span> Ver BOM
                      </button>
                      <button class="btn btn-danger btn-sm" (click)="eliminar(rec)">
                        <span class="material-symbols-outlined icon-inline" style="font-size: 1rem;">delete</span>
                      </button>
                    </div>
                  </td>
                </tr>
              } @empty {
                @if (!loading()) {
                  <tr>
                    <td colspan="6" class="text-center py-4">No hay recetas maestras registradas.</td>
                  </tr>
                }
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Modal Detalle BOM -->
    @if (modalDetalle() && recetaSeleccionada(); as r) {
      <div class="modal-backdrop">
        <div class="modal-content">
          <div class="modal-header">
            <h3>Receta: {{ r.codigo }} · {{ r.nombreProducto }}</h3>
            <button class="modal-close" (click)="modalDetalle.set(false)">×</button>
          </div>
          <p class="subtitle mb-3">Lote base de referencia: <strong>{{ r.cantidadBase }} unidades</strong></p>
          <div class="table-container">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Insumo / Componente</th>
                  <th>Cant. Requerida</th>
                  <th>U.M.</th>
                </tr>
              </thead>
              <tbody>
                @for (d of r.detalles; track d.id || d.insumoId) {
                  <tr>
                    <td>#{{ d.insumoId }}</td>
                    <td><strong>{{ d.nombreInsumo }}</strong></td>
                    <td>{{ d.cantidadRequerida | number:'1.2-4' }}</td>
                    <td><span class="badge badge-info">{{ d.unidadMedida }}</span></td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <div class="modal-footer mt-3">
            <button class="btn btn-secondary" (click)="modalDetalle.set(false)">Cerrar</button>
          </div>
        </div>
      </div>
    }

    <!-- Modal Nueva Receta BOM -->
    @if (modalNueva()) {
      <div class="modal-backdrop">
        <div class="modal-content" style="max-width: 680px;">
          <div class="modal-header">
            <h3>Registrar Nueva Receta Maestra BOM</h3>
            <button class="modal-close" (click)="modalNueva.set(false)">×</button>
          </div>
          <form [formGroup]="formNueva" (ngSubmit)="guardarNueva()">
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; margin-bottom: 0.75rem;">
              <div class="form-group">
                <label class="form-label">Código BOM *</label>
                <input type="text" formControlName="codigo" class="form-control" placeholder="Ej: BOM-COCO-NEW" />
              </div>
              <div class="form-group">
                <label class="form-label">Cantidad Base *</label>
                <input type="number" formControlName="cantidadBase" class="form-control" />
              </div>
            </div>

            <div class="form-group mb-3">
              <label class="form-label">Producto Terminado del Catálogo *</label>
              <select formControlName="productoId" class="form-control">
                @for (p of productos(); track p.id) {
                  <option [value]="p.id">{{ p.nombre }}</option>
                }
              </select>
            </div>

            <div class="form-group mb-3">
              <label class="form-label">Observaciones</label>
              <input type="text" formControlName="observaciones" class="form-control" />
            </div>

            <div class="modal-footer">
              <button type="button" class="btn btn-secondary" (click)="modalNueva.set(false)">Cancelar</button>
              <button type="submit" class="btn btn-primary" [disabled]="loading()">Guardar Receta</button>
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
    .btn-group { display: flex; gap: 0.4rem; justify-content: center; }
    .mb-3 { margin-bottom: 1rem; }
    .mt-3 { margin-top: 1rem; }
  `]
})
export class ListaMaterialesList implements OnInit {
  private readonly produccionService = inject(ProduccionService);
  private readonly productoService = inject(ProductoService);
  private readonly fb = inject(FormBuilder);

  protected readonly recetas = signal<ListaMateriales[]>([]);
  protected readonly productos = signal<Producto[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly successMsg = signal<string | null>(null);

  protected readonly recetaSeleccionada = signal<ListaMateriales | null>(null);
  protected readonly modalDetalle = signal(false);
  protected readonly modalNueva = signal(false);

  protected readonly formNueva = this.fb.nonNullable.group({
    codigo: ['', [Validators.required]],
    productoId: [14, [Validators.required]],
    cantidadBase: [100, [Validators.required, Validators.min(1)]],
    observaciones: ['Receta estándar de planta Pisco'],
  });

  ngOnInit(): void {
    this.cargarDatos();
    this.productoService.listar().subscribe({
      next: (prods) => this.productos.set(prods),
      error: () => {},
    });
  }

  cargarDatos(): void {
    this.loading.set(true);
    this.error.set(null);
    this.produccionService.listarListasMateriales().subscribe({
      next: (data) => {
        this.recetas.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la lista de recetas maestras BOM.');
        this.loading.set(false);
      },
    });
  }

  verDetalle(rec: ListaMateriales): void {
    this.recetaSeleccionada.set(rec);
    this.modalDetalle.set(true);
  }

  abrirModalNueva(): void {
    this.modalNueva.set(true);
  }

  guardarNueva(): void {
    if (this.formNueva.invalid) return;
    this.loading.set(true);
    const val = this.formNueva.getRawValue();
    const prod = this.productos().find(p => p.id === Number(val.productoId));

    const req: ListaMaterialesRequest = {
      codigo: val.codigo,
      productoId: Number(val.productoId),
      nombreProducto: prod ? prod.nombre : 'Producto Agro Peruvian Foods',
      cantidadBase: Number(val.cantidadBase),
      observaciones: val.observaciones,
      detalles: [
        {
          insumoId: 101,
          nombreInsumo: 'Copra Blanca Fresca Prensable Orgánica',
          cantidadRequerida: 45.0,
          unidadMedida: 'KG',
        },
        {
          insumoId: 102,
          nombreInsumo: 'Frasco de Vidrio Grado Alimentario 250ml',
          cantidadRequerida: 100.0,
          unidadMedida: 'UNID',
        }
      ],
    };

    this.produccionService.crearListaMateriales(req).subscribe({
      next: () => {
        this.successMsg.set(`Receta ${req.codigo} creada exitosamente.`);
        this.modalNueva.set(false);
        this.cargarDatos();
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Error al guardar la receta BOM.');
        this.loading.set(false);
      },
    });
  }

  eliminar(rec: ListaMateriales): void {
    if (!confirm(`¿Eliminar la receta BOM "${rec.codigo}"?`)) return;
    this.produccionService.eliminarListaMateriales(rec.id).subscribe({
      next: () => {
        this.successMsg.set(`Receta ${rec.codigo} eliminada.`);
        this.cargarDatos();
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Error al eliminar la receta.');
      },
    });
  }
}

