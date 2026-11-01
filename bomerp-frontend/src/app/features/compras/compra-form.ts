import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { CompraService } from './compra.service';
import { Proveedor, CompraRequest } from './compra.model';

export interface InsumoCatalogo {
  id: number;
  nombre: string;
  unidad: string;
  precioSugerido: number;
}

const INSUMOS_AGROPERUVIAN: InsumoCatalogo[] = [
  { id: 101, nombre: 'Cacao Nativo Chuncho en Grano Seleccionado (kg)', unidad: 'KG', precioSugerido: 20.0 },
  { id: 102, nombre: 'Cocos Frescos Orgánicos del Chira para Prensado (unid)', unidad: 'UNID', precioSugerido: 2.2 },
  { id: 103, nombre: 'Copra Blanca Deshidratada Grado Alimentario (kg)', unidad: 'KG', precioSugerido: 9.5 },
  { id: 104, nombre: 'Frasco de Vidrio Grado Alimentario 30ml + Tapa', unidad: 'UNID', precioSugerido: 1.1 },
  { id: 105, nombre: 'Frasco de Vidrio Grado Alimentario 120ml + Tapa', unidad: 'UNID', precioSugerido: 1.6 },
  { id: 106, nombre: 'Frasco de Vidrio Grado Alimentario 250ml + Tapa Twist-Off', unidad: 'UNID', precioSugerido: 2.1 },
  { id: 107, nombre: 'Frasco de Vidrio Grado Alimentario 460ml + Tapa Twist-Off', unidad: 'UNID', precioSugerido: 2.65 },
  { id: 108, nombre: 'Frasco de Vidrio Grado Alimentario 1L Boca Ancha', unidad: 'UNID', precioSugerido: 3.8 },
  { id: 109, nombre: 'Balde HDPE Grado Alimentario 4L con Asa y Tapa Precinto', unidad: 'UNID', precioSugerido: 8.5 },
  { id: 110, nombre: 'Balde Industrial HDPE 20L Grado Alimentario', unidad: 'UNID', precioSugerido: 24.0 },
  { id: 111, nombre: 'Bolsa Doypack Kraft Bilaminada con Zipper 250g/500g', unidad: 'MILLAR', precioSugerido: 420.0 },
  { id: 112, nombre: 'Rollo Etiquetas Impermeables BOPP + Precintos Termocontraíbles', unidad: 'ROLLO', precioSugerido: 95.0 },
  { id: 113, nombre: 'Caja de Cartón Corrugado Bandeja 24/36 Unidades Exportación', unidad: 'UNID', precioSugerido: 3.4 },
];

@Component({
  selector: 'app-compra-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, CurrencyPipe],
  templateUrl: './compra-form.html',
  styleUrl: './compra-form.css',
})
export class CompraForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly compraService = inject(CompraService);
  private readonly router = inject(Router);

  protected readonly proveedores = signal<Proveedor[]>([]);
  protected readonly catalogoInsumos = INSUMOS_AGROPERUVIAN;
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    proveedorId: [0, [Validators.required, Validators.min(1)]],
    numeroComprobante: [
      `F001-${Math.floor(100000 + Math.random() * 900000)}`,
      [Validators.required, Validators.maxLength(30)],
    ],
    fechaEmision: [new Date().toISOString().slice(0, 10), [Validators.required]],
    detalles: this.fb.array([this.crearLineaInicial()], [Validators.minLength(1)]),
  });

  protected get detallesArray() {
    return this.form.controls.detalles;
  }

  protected readonly valoresDetalles = toSignal(this.form.controls.detalles.valueChanges, {
    initialValue: this.form.controls.detalles.getRawValue(),
  });

  protected readonly subtotalesLinea = computed(() =>
    this.valoresDetalles().map((d) => (Number(d.cantidad) || 0) * (Number(d.precioUnitario) || 0))
  );

  protected readonly totalFactura = computed(() =>
    this.subtotalesLinea().reduce((acc, s) => acc + s, 0)
  );

  protected readonly igvCalculado = computed(() => Math.round(this.totalFactura() * 0.18 * 100) / 100);

  protected readonly baseImponible = computed(() =>
    Math.max(0, this.totalFactura() - this.igvCalculado())
  );

  ngOnInit(): void {
    this.compraService.listarProveedores().subscribe({
      next: (provs) => {
        this.proveedores.set(provs);
        if (provs.length > 0) {
          this.form.patchValue({ proveedorId: provs[0].id });
        }
      },
      error: () => {
        this.error.set('No se pudo cargar el directorio de proveedores.');
      },
    });
  }

  private crearLineaInicial() {
    const insumoInicial = INSUMOS_AGROPERUVIAN[0];
    return this.fb.nonNullable.group({
      insumoId: [insumoInicial.id, [Validators.required, Validators.min(1)]],
      nombreInsumo: [insumoInicial.nombre, [Validators.required, Validators.maxLength(150)]],
      cantidad: [100, [Validators.required, Validators.min(0.01)]],
      precioUnitario: [insumoInicial.precioSugerido, [Validators.required, Validators.min(0.01)]],
    });
  }

  agregarLinea(): void {
    const sugerido = INSUMOS_AGROPERUVIAN[Math.min(this.detallesArray.length, INSUMOS_AGROPERUVIAN.length - 1)];
    this.detallesArray.push(
      this.fb.nonNullable.group({
        insumoId: [sugerido.id, [Validators.required, Validators.min(1)]],
        nombreInsumo: [sugerido.nombre, [Validators.required, Validators.maxLength(150)]],
        cantidad: [50, [Validators.required, Validators.min(0.01)]],
        precioUnitario: [sugerido.precioSugerido, [Validators.required, Validators.min(0.01)]],
      })
    );
  }

  eliminarLinea(index: number): void {
    if (this.detallesArray.length > 1) {
      this.detallesArray.removeAt(index);
    }
  }

  seleccionarInsumoCatalogo(index: number, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const idSeleccionado = Number(select.value);
    const encontrado = INSUMOS_AGROPERUVIAN.find((i) => i.id === idSeleccionado);
    if (encontrado) {
      this.detallesArray.at(index).patchValue({
        insumoId: encontrado.id,
        nombreInsumo: encontrado.nombre,
        precioUnitario: encontrado.precioSugerido,
      });
    }
  }

  guardar(): void {
    if (this.form.invalid || this.detallesArray.length === 0) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const raw = this.form.getRawValue();
    const request: CompraRequest = {
      proveedorId: Number(raw.proveedorId),
      numeroComprobante: raw.numeroComprobante.trim(),
      fechaEmision: raw.fechaEmision,
      detalles: raw.detalles.map((d) => ({
        insumoId: Number(d.insumoId),
        nombreInsumo: d.nombreInsumo.trim(),
        cantidad: Number(d.cantidad),
        precioUnitario: Number(d.precioUnitario),
      })),
    };

    this.compraService.crearCompra(request).subscribe({
      next: () => {
        this.router.navigate(['/compras']);
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Error al registrar la orden de compra.');
        this.loading.set(false);
      },
    });
  }
}

