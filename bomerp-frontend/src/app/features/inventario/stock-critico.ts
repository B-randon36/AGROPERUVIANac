import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { InventarioService } from './inventario-service';
import { StockCritico } from './inventario.model';

@Component({
  selector: 'app-stock-critico',
  imports: [CommonModule, RouterLink, FormsModule, DecimalPipe],
  template: `
    <div class="feature-container">
      <div class="page-header">
        <div>
          <div class="badge badge-danger">CONTROL SANITARIO & REABASTECIMIENTO</div>
          <h1>Reporte de Stock Crítico & Semáforo de Ruptura</h1>
          <p class="subtitle">Monitoreo preventivo de existencias mínimas y puntos de reorden para planta</p>
        </div>
        <div class="header-actions">
          <a routerLink="/produccion/nueva" class="btn btn-primary">
            <span class="material-symbols-outlined icon-inline">precision_manufacturing</span> Ordenar Producción
          </a>
          <a routerLink="/inventario/lotes" class="btn btn-secondary">← Lotes FEFO</a>
          <button class="btn btn-secondary" (click)="cargarDatos()" title="Actualizar">
            <span class="material-symbols-outlined" style="vertical-align: middle;">refresh</span>
          </button>
        </div>
      </div>

      <!-- Filtro y parámetros de umbral -->
      <div class="card p-3" style="display: flex; gap: 1rem; align-items: center; flex-wrap: wrap;">
        <div style="display: flex; align-items: center; gap: 0.5rem;">
          <label style="font-weight: 600; font-size: 0.85rem;">Umbral de Stock Mínimo:</label>
          <input
            type="number"
            class="form-control"
            style="width: 110px;"
            [(ngModel)]="stockMinimo"
            (change)="cargarDatos()"
          />
        </div>

        <div style="display: flex; align-items: center; gap: 0.5rem;">
          <input
            type="checkbox"
            id="chkSoloAlertas"
            [(ngModel)]="soloAlertas"
            (change)="cargarDatos()"
          />
          <label for="chkSoloAlertas" style="font-size: 0.85rem; cursor: pointer;">
            Mostrar únicamente productos con déficit o alerta
          </label>
        </div>
      </div>

      @if (error()) {
        <div class="alert alert-danger" style="display: flex; align-items: center; gap: 0.5rem;">
          <span class="material-symbols-outlined">warning</span>
          <span>{{ error() }}</span>
        </div>
      }

      <div class="card">
        <div class="table-container">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Producto Oficial</th>
                <th>Categoría</th>
                <th>Stock Catálogo</th>
                <th>En Lotes FEFO</th>
                <th>Umbral Mínimo</th>
                <th>Déficit Reposición</th>
                <th>Semáforo Alerta</th>
              </tr>
            </thead>
            <tbody>
              @for (item of items(); track item.productoId) {
                <tr>
                  <td>#{{ item.productoId }}</td>
                  <td><strong>{{ item.nombreProducto }}</strong></td>
                  <td><span class="badge badge-secondary">{{ item.categoria }}</span></td>
                  <td>
                    <strong [class.text-danger]="item.stockActual <= 0">
                      {{ item.stockActual }} u.
                    </strong>
                  </td>
                  <td>{{ item.stockLotesVigentes | number:'1.0-2' }} u.</td>
                  <td>{{ item.stockMinimo }} u.</td>
                  <td>
                    @if (item.deficit > 0) {
                      <span class="text-danger" style="font-weight: 700;">+{{ item.deficit }} u.</span>
                    } @else {
                      <span class="text-success">Cubierto</span>
                    }
                  </td>
                  <td>
                    <span class="badge"
                      [class.badge-danger]="item.nivelAlerta === 'AGOTADO' || item.nivelAlerta === 'CRITICO'"
                      [class.badge-warning]="item.nivelAlerta === 'PREVENTIVO'"
                      [class.badge-success]="item.nivelAlerta === 'OPTIMO'">
                      {{ item.nivelAlerta }}
                    </span>
                  </td>
                </tr>
              } @empty {
                @if (!loading()) {
                  <tr>
                    <td colspan="8" class="text-center py-4">No se encontraron productos bajo los criterios de alerta seleccionados.</td>
                  </tr>
                }
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .feature-container { display: flex; flex-direction: column; gap: 1.5rem; }
    .page-header { display: flex; justify-content: space-between; align-items: flex-start; }
    .page-header h1 { font-size: 1.65rem; margin-top: 0.25rem; }
    .subtitle { color: var(--text-muted); font-size: 0.85rem; }
    .header-actions { display: flex; gap: 0.75rem; }
    .text-danger { color: #dc2626; }
    .text-success { color: #16a34a; }
    .p-3 { padding: 1rem; }
  `]
})
export class StockCriticoComponent implements OnInit {
  private readonly inventarioService = inject(InventarioService);

  protected readonly items = signal<StockCritico[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected stockMinimo = 100;
  protected soloAlertas = false;

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.loading.set(true);
    this.error.set(null);
    this.inventarioService.obtenerStockCritico(this.stockMinimo, this.soloAlertas).subscribe({
      next: (data) => {
        this.items.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el reporte de stock crítico.');
        this.loading.set(false);
      },
    });
  }
}

