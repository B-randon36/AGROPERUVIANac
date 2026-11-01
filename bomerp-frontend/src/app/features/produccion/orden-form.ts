import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProduccionService } from './produccion.service';
import { CentroTrabajo, DetalleConsumoRequest, OrdenProduccionRequest } from './produccion.model';
import { ProductoService } from '../catalogo/producto/producto.service';
import { Producto } from '../catalogo/producto/producto.model';

export interface InsumoBomCatalogo {
  id: number;
  nombre: string;
  unidad: string;
}

const INSUMOS_BOM_CATALOGO: InsumoBomCatalogo[] = [
  { id: 101, nombre: 'Copra Blanca Fresca Prensable Orgánica', unidad: 'KG' },
  { id: 102, nombre: 'Frasco de Vidrio Grado Alimentario 250ml', unidad: 'UNID' },
  { id: 103, nombre: 'Tapa Twist-Off + Precinto Termocontraíble', unidad: 'UNID' },
  { id: 104, nombre: 'Frasco de Vidrio Grado Alimentario 1L', unidad: 'UNID' },
  { id: 105, nombre: 'Etiqueta Metalizada Sanitaria + Sello Orgánico', unidad: 'UNID' },
  { id: 106, nombre: 'Balde HDPE Grado Alimentario 4L con Asa y Precinto', unidad: 'UNID' },
  { id: 107, nombre: 'Cacao Nativo Chuncho Fermentado Selección', unidad: 'KG' },
  { id: 108, nombre: 'Bolsa Bilaminada Kraft con Válvula Desgasificadora 1kg', unidad: 'UNID' },
  { id: 109, nombre: 'Pulpa Fresca de Coco Deshidratada Hebra Larga', unidad: 'KG' },
  { id: 110, nombre: 'Bolsa Doypack Zipper Barrera UV 500g', unidad: 'UNID' },
  { id: 111, nombre: 'Frasco de Vidrio Grado Alimentario 30ml + Tapa', unidad: 'UNID' },
  { id: 112, nombre: 'Frasco de Vidrio Grado Alimentario 120ml + Tapa', unidad: 'UNID' },
  { id: 113, nombre: 'Frasco de Vidrio Grado Alimentario 460ml + Tapa Twist-Off', unidad: 'UNID' },
  { id: 114, nombre: 'Balde Industrial HDPE 20L Grado Alimentario', unidad: 'UNID' },
  { id: 115, nombre: 'Caja de Cartón Corrugado Master Planta HACCP', unidad: 'UNID' },
];

@Component({
  selector: 'app-orden-form',
  imports: [CommonModule, ReactiveFormsModule, RouterLink, DecimalPipe],
  templateUrl: './orden-form.html',
  styleUrl: './orden-form.css',
})
export class OrdenForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly produccionService = inject(ProduccionService);
  private readonly productoService = inject(ProductoService);
  private readonly router = inject(Router);

  protected readonly productosDisponibles = signal<Producto[]>([]);
  protected readonly centrosTrabajo = signal<CentroTrabajo[]>([]);
  protected readonly catalogoInsumos = INSUMOS_BOM_CATALOGO;
  protected readonly loadingProductos = signal(true);
  protected readonly loadingCentros = signal(true);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    productoId: [0, [Validators.required, Validators.min(1)]],
    cantidadProgramada: [100, [Validators.required, Validators.min(1)]],
    centroTrabajoId: [0],
    observaciones: ['Lote de producción, prensado en frío y envasado bajo norma sanitaria HACCP'],
    insumos: this.fb.array([this.crearFilaInsumo(101, 'Copra Blanca Fresca Prensable Orgánica', 45, 'KG')], [Validators.minLength(1)]),
  });

  protected get insumosArray() {
    return this.form.controls.insumos;
  }

  ngOnInit(): void {
    this.cargarCentrosTrabajo();

    this.productoService.listar().subscribe({
      next: (productos) => {
        this.productosDisponibles.set(productos);
        this.loadingProductos.set(false);
        if (productos.length > 0) {
          this.form.patchValue({ productoId: productos[0].id });
          this.aplicarRecetaEstandar();
        }
      },
      error: () => {
        this.loadingProductos.set(false);
        this.error.set('No se pudo cargar el catálogo oficial de productos de Agro Peruvian Foods.');
      },
    });
  }

  private cargarCentrosTrabajo(): void {
    this.produccionService.listarCentrosTrabajo().subscribe({
      next: (centros) => {
        this.centrosTrabajo.set(centros);
        this.loadingCentros.set(false);
        if (centros.length > 0) {
          this.form.patchValue({ centroTrabajoId: centros[0].id });
        }
      },
      error: () => {
        this.loadingCentros.set(false);
      },
    });
  }

  private crearFilaInsumo(id: number, nombre: string, cantidad: number, unidad: string) {
    const redondeado = Math.round(cantidad * 1000) / 1000;
    return this.fb.nonNullable.group({
      insumoId: [id, [Validators.required, Validators.min(1)]],
      nombreInsumo: [nombre, [Validators.required, Validators.maxLength(150)]],
      cantidadRequerida: [redondeado, [Validators.required, Validators.min(0.001)]],
      cantidadConsumida: [redondeado, [Validators.required, Validators.min(0.001)]],
      unidadMedida: [unidad, [Validators.required, Validators.maxLength(20)]],
    });
  }

  onProductoOCantidadChange(): void {
    this.aplicarRecetaEstandar();
  }

  aplicarRecetaEstandar(): void {
    const prodId = Number(this.form.controls.productoId.value);
    const cant = Math.max(1, Number(this.form.controls.cantidadProgramada.value) || 1);
    const producto = this.productosDisponibles().find((p) => p.id === prodId);
    if (!producto) {
      return;
    }

    // Intentar cargar la Receta Maestra BOM oficial registrada en backend
    this.produccionService.obtenerRecetaPorProducto(prodId).subscribe({
      next: (recetaBom) => {
        if (recetaBom && recetaBom.detalles && recetaBom.detalles.length > 0) {
          const factor = recetaBom.cantidadBase > 0 ? cant / recetaBom.cantidadBase : 1;
          this.insumosArray.clear();
          for (const item of recetaBom.detalles) {
            const req = item.cantidadRequerida * factor;
            this.insumosArray.push(
              this.crearFilaInsumo(item.insumoId, item.nombreInsumo, req, item.unidadMedida)
            );
          }
        } else {
          this.aplicarRecetaCalculadaLocal(producto.nombre, cant);
        }
      },
      error: () => {
        // Fallback a cálculo de explosión local
        this.aplicarRecetaCalculadaLocal(producto.nombre, cant);
      },
    });
  }

  private aplicarRecetaCalculadaLocal(nombreProducto: string, cant: number): void {
    const receta = this.calcularRecetaPorProducto(nombreProducto, cant);
    this.insumosArray.clear();
    for (const item of receta) {
      this.insumosArray.push(
        this.crearFilaInsumo(item.insumoId, item.nombreInsumo, item.cantidadRequerida, item.unidadMedida)
      );
    }
  }

  private calcularRecetaPorProducto(nombreProducto: string, cant: number): DetalleConsumoRequest[] {
    const n = nombreProducto.toLowerCase();

    if (n.includes('cacao')) {
      const factorKg = n.includes('500') ? 0.52 : n.includes('250') ? 0.26 : 1.04;
      return [
        {
          insumoId: 107,
          nombreInsumo: 'Cacao Nativo Chuncho Fermentado Selección',
          cantidadRequerida: cant * factorKg,
          cantidadConsumida: cant * factorKg,
          unidadMedida: 'KG',
        },
        {
          insumoId: 108,
          nombreInsumo: 'Bolsa Bilaminada Kraft con Válvula Desgasificadora 1kg',
          cantidadRequerida: cant,
          cantidadConsumida: cant,
          unidadMedida: 'UNID',
        },
      ];
    }

    if (n.includes('rallado') || n.includes('harina')) {
      const factorKg = n.includes('1kg') || n.includes('1 kg') ? 1.05 : n.includes('500') ? 0.525 : 0.265;
      return [
        {
          insumoId: 109,
          nombreInsumo: 'Pulpa Fresca de Coco Deshidratada Hebra Larga',
          cantidadRequerida: cant * factorKg,
          cantidadConsumida: cant * factorKg,
          unidadMedida: 'KG',
        },
        {
          insumoId: 110,
          nombreInsumo: 'Bolsa Doypack Zipper Barrera UV 500g',
          cantidadRequerida: cant,
          cantidadConsumida: cant,
          unidadMedida: 'UNID',
        },
      ];
    }

    // Aceites de Coco
    let litros = 0.25;
    let envaseId = 102;
    let envaseNom = 'Frasco de Vidrio Grado Alimentario 250ml';
    if (n.includes('4l')) {
      litros = 4.0;
      envaseId = 106;
      envaseNom = 'Balde HDPE Grado Alimentario 4L con Asa y Precinto';
    } else if (n.includes('1l') || n.includes('1 l')) {
      litros = 1.0;
      envaseId = 104;
      envaseNom = 'Frasco de Vidrio Grado Alimentario 1L';
    }

    const copra = cant * litros * 1.8;
    return [
      {
        insumoId: 101,
        nombreInsumo: 'Copra Blanca Fresca Prensable Orgánica',
        cantidadRequerida: copra,
        cantidadConsumida: copra,
        unidadMedida: 'KG',
      },
      {
        insumoId: envaseId,
        nombreInsumo: envaseNom,
        cantidadRequerida: cant,
        cantidadConsumida: cant,
        unidadMedida: 'UNID',
      },
      {
        insumoId: 103,
        nombreInsumo: 'Tapa Twist-Off + Precinto Termocontraíble',
        cantidadRequerida: cant,
        cantidadConsumida: cant,
        unidadMedida: 'UNID',
      },
    ];
  }

  agregarInsumoManual(): void {
    const sugerido = INSUMOS_BOM_CATALOGO[0];
    this.insumosArray.push(this.crearFilaInsumo(sugerido.id, sugerido.nombre, 10, sugerido.unidad));
  }

  eliminarInsumo(index: number): void {
    if (this.insumosArray.length > 1) {
      this.insumosArray.removeAt(index);
    }
  }

  seleccionarInsumoCatalogo(index: number, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const idSeleccionado = Number(select.value);
    const insumo = INSUMOS_BOM_CATALOGO.find((i) => i.id === idSeleccionado);
    if (insumo) {
      this.insumosArray.at(index).patchValue({
        insumoId: insumo.id,
        nombreInsumo: insumo.nombre,
        unidadMedida: insumo.unidad,
      });
    }
  }

  guardar(): void {
    if (this.form.invalid || this.insumosArray.length === 0) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const val = this.form.getRawValue();
    const prod = this.productosDisponibles().find((p) => p.id === Number(val.productoId));
    const ct = this.centrosTrabajo().find((c) => c.id === Number(val.centroTrabajoId));

    const request: OrdenProduccionRequest = {
      productoId: Number(val.productoId),
      nombreProducto: prod ? prod.nombre : 'Producto Agro Peruvian Foods',
      cantidadProgramada: Number(val.cantidadProgramada),
      centroTrabajoId: ct ? ct.id : undefined,
      centroTrabajoNombre: ct ? ct.nombre : undefined,
      observaciones: val.observaciones,
      insumos: val.insumos.map((item) => ({
        insumoId: Number(item.insumoId),
        nombreInsumo: item.nombreInsumo.trim(),
        cantidadRequerida: Number(item.cantidadRequerida),
        cantidadConsumida: Number(item.cantidadConsumida),
        unidadMedida: item.unidadMedida.trim().toUpperCase(),
      })),
    };

    this.produccionService.crearOrden(request).subscribe({
      next: () => {
        this.router.navigate(['/produccion']);
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Error al planificar la orden de producción.');
        this.loading.set(false);
      },
    });
  }
}
