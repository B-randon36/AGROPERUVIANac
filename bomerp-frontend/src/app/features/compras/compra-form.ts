import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { CompraService } from './compra.service';
import { Proveedor, CompraRequest, Insumo } from './compra.model';

export interface InsumoCatalogo {
  id: number;
  nombre: string;
  unidad: string;
  precioSugerido: number;
}

const INSUMOS_DEFAULT: InsumoCatalogo[] = [
  { id: 101, nombre: 'Cacao Nativo Chuncho en Grano Seleccionado', unidad: 'KG', precioSugerido: 20.0 },
  { id: 102, nombre: 'Cocos Frescos Orgánicos del Chira para Prensado', unidad: 'UNID', precioSugerido: 2.2 },
  { id: 103, nombre: 'Copra Blanca Deshidratada Grado Alimentario', unidad: 'KG', precioSugerido: 9.5 },
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
  protected readonly catalogoInsumos = signal<InsumoCatalogo[]>(INSUMOS_DEFAULT);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly modalConfirmacion = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    proveedorId: [0, [Validators.required, Validators.min(1)]],
    serieComprobante: ['F001', [Validators.required, Validators.maxLength(10)]],
    numeroComprobante: [
      `${Math.floor(100000 + Math.random() * 900000)}`,
      [Validators.required, Validators.maxLength(30)],
    ],
    fechaEmision: [new Date().toISOString().slice(0, 10), [Validators.required]],
    condicionPago: ['CONTADO', [Validators.required]],
    costoFlete: [0, [Validators.min(0)]],
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

  protected readonly cantidadesIngreso = computed(() =>
    this.valoresDetalles().map((d) => (Number(d.cantidad) || 0) * (Number(d.factorConversion) || 1))
  );

  protected readonly subtotalNeto = computed(() =>
    this.subtotalesLinea().reduce((acc, s) => acc + s, 0)
  );

  protected readonly igvCalculado = computed(() =>
    Math.round(this.subtotalNeto() * 0.18 * 100) / 100
  );

  protected readonly fleteVal = computed(() =>
    Number(this.form.controls.costoFlete.value) || 0
  );

  protected readonly totalFactura = computed(() =>
    this.subtotalNeto() + this.igvCalculado() + this.fleteVal()
  );

  ngOnInit(): void {
    // 1. Cargar directorio de proveedores
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

    // 2. Cargar catálogo de insumos dinámicos desde backend
    this.compraService.listarInsumos().subscribe({
      next: (insumosApi: Insumo[]) => {
        if (insumosApi && insumosApi.length > 0) {
          const mapeados: InsumoCatalogo[] = insumosApi.map((i) => ({
            id: i.id,
            nombre: `${i.nombre} (${i.unidadMedida})`,
            unidad: i.unidadMedida,
            precioSugerido: Number(i.precioSugerido) || 10.0,
          }));
          this.catalogoInsumos.set(mapeados);
        }
      },
      error: () => {
        // Mantiene INSUMOS_DEFAULT
      },
    });
  }

  private crearLineaInicial() {
    const insumoInicial = this.catalogoInsumos()[0] || INSUMOS_DEFAULT[0];
    const hoy = new Date();
    const vencimientoDefault = new Date(hoy.setFullYear(hoy.getFullYear() + 1)).toISOString().slice(0, 10);

    return this.fb.nonNullable.group({
      insumoId: [insumoInicial.id, [Validators.required, Validators.min(1)]],
      nombreInsumo: [insumoInicial.nombre, [Validators.required, Validators.maxLength(150)]],
      loteProveedor: [''],
      fechaVencimiento: [vencimientoDefault],
      factorConversion: [1, [Validators.required, Validators.min(0.01)]],
      cantidad: [100, [Validators.required, Validators.min(0.01)]],
      precioUnitario: [insumoInicial.precioSugerido, [Validators.required, Validators.min(0.01)]],
    });
  }

  agregarLinea(): void {
    const catalogo = this.catalogoInsumos();
    const sugerido = catalogo[Math.min(this.detallesArray.length, catalogo.length - 1)];
    const hoy = new Date();
    const vencimientoDefault = new Date(hoy.setFullYear(hoy.getFullYear() + 1)).toISOString().slice(0, 10);

    this.detallesArray.push(
      this.fb.nonNullable.group({
        insumoId: [sugerido.id, [Validators.required, Validators.min(1)]],
        nombreInsumo: [sugerido.nombre, [Validators.required, Validators.maxLength(150)]],
        loteProveedor: [''],
        fechaVencimiento: [vencimientoDefault],
        factorConversion: [1, [Validators.required, Validators.min(0.01)]],
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
    const encontrado = this.catalogoInsumos().find((i) => i.id === idSeleccionado);
    if (encontrado) {
      this.detallesArray.at(index).patchValue({
        insumoId: encontrado.id,
        nombreInsumo: encontrado.nombre,
        precioUnitario: encontrado.precioSugerido,
      });
    }
  }

  abrirConfirmacion(): void {
    if (this.form.invalid || this.detallesArray.length === 0) {
      this.form.markAllAsTouched();
      this.error.set('Por favor verifique los campos requeridos en el formulario.');
      return;
    }
    this.error.set(null);
    this.modalConfirmacion.set(true);
  }

  cerrarConfirmacion(): void {
    this.modalConfirmacion.set(false);
  }

  proveedorSeleccionado(): Proveedor | undefined {
    const id = Number(this.form.controls.proveedorId.value);
    return this.proveedores().find((p) => p.id === id);
  }

  guardar(): void {
    this.loading.set(true);
    this.error.set(null);
    this.modalConfirmacion.set(false);

    const raw = this.form.getRawValue();
    const serie = raw.serieComprobante.trim().toUpperCase();
    const numero = raw.numeroComprobante.trim();
    const numeroCompleto = `${serie}-${numero}`;

    const request: CompraRequest = {
      proveedorId: Number(raw.proveedorId),
      serieComprobante: serie,
      numeroComprobante: numeroCompleto,
      fechaEmision: raw.fechaEmision,
      condicionPago: raw.condicionPago,
      costoFlete: Number(raw.costoFlete) || 0,
      detalles: raw.detalles.map((d) => ({
        insumoId: Number(d.insumoId),
        nombreInsumo: d.nombreInsumo.trim(),
        loteProveedor: d.loteProveedor ? d.loteProveedor.trim().toUpperCase() : undefined,
        fechaVencimiento: d.fechaVencimiento || undefined,
        factorConversion: Number(d.factorConversion) || 1,
        cantidad: Number(d.cantidad),
        precioUnitario: Number(d.precioUnitario),
      })),
    };

    this.compraService.crearCompra(request).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/compras']);
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Error al registrar la orden de compra.');
        this.loading.set(false);
      },
    });
  }
}
