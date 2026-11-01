import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProduccionService } from './produccion.service';
import { DetalleConsumoRequest, OrdenProduccionRequest } from './produccion.model';
import { ProductoService } from '../catalogo/producto/producto.service';
import { Producto } from '../catalogo/producto/producto.model';

export interface InsumoBomCatalogo {
  id: number;
  nombre: string;
  unidad: string;
}

const INSUMOS_BOM_CATALOGO: InsumoBomCatalogo[] = [
  { id: 101, nombre: 'Cacao Nativo Chuncho en Grano Seleccionado', unidad: 'KG' },
  { id: 102, nombre: 'Cocos Frescos Orgánicos del Chira para Prensado', unidad: 'UNID' },
  { id: 103, nombre: 'Copra Blanca Deshidratada Grado Alimentario', unidad: 'KG' },
  { id: 104, nombre: 'Frasco de Vidrio Grado Alimentario 30ml + Tapa', unidad: 'UNID' },
  { id: 105, nombre: 'Frasco de Vidrio Grado Alimentario 120ml + Tapa', unidad: 'UNID' },
  { id: 106, nombre: 'Frasco de Vidrio Grado Alimentario 250ml + Tapa Twist-Off', unidad: 'UNID' },
  { id: 107, nombre: 'Frasco de Vidrio Grado Alimentario 460ml + Tapa Twist-Off', unidad: 'UNID' },
  { id: 108, nombre: 'Frasco de Vidrio Grado Alimentario 1L Boca Ancha', unidad: 'UNID' },
  { id: 109, nombre: 'Balde HDPE Grado Alimentario 4L con Asa y Precinto', unidad: 'UNID' },
  { id: 110, nombre: 'Balde Industrial HDPE 20L Grado Alimentario', unidad: 'UNID' },
  { id: 111, nombre: 'Bolsa Doypack Kraft Bilaminada con Zipper', unidad: 'UNID' },
  { id: 112, nombre: 'Etiqueta Impermeable BOPP + Precinto Termocontraíble', unidad: 'UNID' },
  { id: 113, nombre: 'Caja de Cartón Corrugado Master Planta HACCP', unidad: 'UNID' },
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
  protected readonly catalogoInsumos = INSUMOS_BOM_CATALOGO;
  protected readonly loadingProductos = signal(true);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    productoId: [0, [Validators.required, Validators.min(1)]],
    cantidadProgramada: [100, [Validators.required, Validators.min(1)]],
    observaciones: ['Lote de producción, prensado en frío y envasado bajo norma sanitaria HACCP'],
    insumos: this.fb.array([this.crearFilaInsumo(103, 'Copra Blanca Deshidratada Grado Alimentario', 50, 'KG')], [Validators.minLength(1)]),
  });

  protected get insumosArray() {
    return this.form.controls.insumos;
  }

  ngOnInit(): void {
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

  private crearFilaInsumo(id: number, nombre: string, cantidad: number, unidad: string) {
    const redondeado = Math.round(cantidad * 100) / 100;
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

    const receta = this.calcularRecetaPorProducto(producto.nombre, cant);
    this.insumosArray.clear();
    for (const item of receta) {
      this.insumosArray.push(
        this.crearFilaInsumo(item.insumoId, item.nombreInsumo, item.cantidadRequerida, item.unidadMedida)
      );
    }
  }

  private calcularRecetaPorProducto(nombreProducto: string, cant: number): DetalleConsumoRequest[] {
    const n = nombreProducto.toLowerCase();

    // Línea Cacao en Grano / Derivados de Cacao
    if (n.includes('cacao')) {
      const factorKg = n.includes('500') ? 0.52 : n.includes('250') ? 0.26 : 1.04; // 4% merma de selección/tostado
      return [
        {
          insumoId: 101,
          nombreInsumo: 'Cacao Nativo Chuncho en Grano Seleccionado',
          cantidadRequerida: cant * factorKg,
          cantidadConsumida: cant * factorKg,
          unidadMedida: 'KG',
        },
        {
          insumoId: 111,
          nombreInsumo: 'Bolsa Doypack Kraft Bilaminada con Zipper',
          cantidadRequerida: cant,
          cantidadConsumida: cant,
          unidadMedida: 'UNID',
        },
        {
          insumoId: 112,
          nombreInsumo: 'Etiqueta Impermeable BOPP + Precinto Termocontraíble',
          cantidadRequerida: cant,
          cantidadConsumida: cant,
          unidadMedida: 'UNID',
        },
        {
          insumoId: 113,
          nombreInsumo: 'Caja de Cartón Corrugado Master Planta HACCP',
          cantidadRequerida: Math.ceil(cant / 12),
          cantidadConsumida: Math.ceil(cant / 12),
          unidadMedida: 'UNID',
        },
      ];
    }

    // Línea Coco Rallado Deshidratado Doypack
    if (n.includes('rallado') || n.includes('harina')) {
      const factorKg = n.includes('1kg') || n.includes('1 kg') ? 1.05 : n.includes('500') ? 0.525 : 0.265;
      return [
        {
          insumoId: 103,
          nombreInsumo: 'Copra Blanca Deshidratada Grado Alimentario',
          cantidadRequerida: cant * factorKg,
          cantidadConsumida: cant * factorKg,
          unidadMedida: 'KG',
        },
        {
          insumoId: 111,
          nombreInsumo: 'Bolsa Doypack Kraft Bilaminada con Zipper',
          cantidadRequerida: cant,
          cantidadConsumida: cant,
          unidadMedida: 'UNID',
        },
        {
          insumoId: 112,
          nombreInsumo: 'Etiqueta Impermeable BOPP + Precinto Termocontraíble',
          cantidadRequerida: cant,
          cantidadConsumida: cant,
          unidadMedida: 'UNID',
        },
      ];
    }

    // Línea Aceite de Coco Virgen (Frascos, Bandejas Mayoristas y Baldes Industriales)
    let litrosPorUnidad = 0.25;
    let envaseId = 106;
    let envaseNombre = 'Frasco de Vidrio Grado Alimentario 250ml + Tapa Twist-Off';
    let envasesPorUnidad = 1;

    if (n.includes('bandeja') && n.includes('30')) {
      // Bandeja x 36 unid de 30ml = 1.08 L por bandeja
      const unids = n.includes('36') ? 36 : 24;
      litrosPorUnidad = unids * 0.03;
      envaseId = 104;
      envaseNombre = 'Frasco de Vidrio Grado Alimentario 30ml + Tapa';
      envasesPorUnidad = unids;
    } else if (n.includes('bandeja') && n.includes('120')) {
      const unids = n.includes('24') ? 24 : 12;
      litrosPorUnidad = unids * 0.12;
      envaseId = 105;
      envaseNombre = 'Frasco de Vidrio Grado Alimentario 120ml + Tapa';
      envasesPorUnidad = unids;
    } else if (n.includes('30ml') || n.includes('30 ml')) {
      litrosPorUnidad = 0.03;
      envaseId = 104;
      envaseNombre = 'Frasco de Vidrio Grado Alimentario 30ml + Tapa';
    } else if (n.includes('120ml') || n.includes('120 ml')) {
      litrosPorUnidad = 0.12;
      envaseId = 105;
      envaseNombre = 'Frasco de Vidrio Grado Alimentario 120ml + Tapa';
    } else if (n.includes('250ml') || n.includes('250 ml')) {
      litrosPorUnidad = 0.25;
      envaseId = 106;
      envaseNombre = 'Frasco de Vidrio Grado Alimentario 250ml + Tapa Twist-Off';
    } else if (n.includes('460ml') || n.includes('460 ml') || n.includes('500ml')) {
      litrosPorUnidad = 0.46;
      envaseId = 107;
      envaseNombre = 'Frasco de Vidrio Grado Alimentario 460ml + Tapa Twist-Off';
    } else if (n.includes('20l') || n.includes('20 l')) {
      litrosPorUnidad = 20;
      envaseId = 110;
      envaseNombre = 'Balde Industrial HDPE 20L Grado Alimentario';
    } else if (n.includes('4l') || n.includes('4 l') || n.includes('galon') || n.includes('galón')) {
      litrosPorUnidad = 4;
      envaseId = 109;
      envaseNombre = 'Balde HDPE Grado Alimentario 4L con Asa y Precinto';
    } else if (n.includes('1l') || n.includes('1 l') || n.includes('1000ml')) {
      litrosPorUnidad = 1.0;
      envaseId = 108;
      envaseNombre = 'Frasco de Vidrio Grado Alimentario 1L Boca Ancha';
    }

    // Rendimiento estándar prensado en frío: ~1.85 kg de copra deshidratada por litro de aceite virgen
    const kgCopraRequerida = Math.max(0.5, cant * litrosPorUnidad * 1.85);
    const totalEnvases = cant * envasesPorUnidad;
    const totalCajas = n.includes('bandeja')
      ? cant
      : litrosPorUnidad >= 4
        ? Math.ceil(cant / 2)
        : Math.ceil(totalEnvases / 12);

    return [
      {
        insumoId: 103,
        nombreInsumo: 'Copra Blanca Deshidratada Grado Alimentario',
        cantidadRequerida: kgCopraRequerida,
        cantidadConsumida: kgCopraRequerida,
        unidadMedida: 'KG',
      },
      {
        insumoId: envaseId,
        nombreInsumo: envaseNombre,
        cantidadRequerida: totalEnvases,
        cantidadConsumida: totalEnvases,
        unidadMedida: 'UNID',
      },
      {
        insumoId: 112,
        nombreInsumo: 'Etiqueta Impermeable BOPP + Precinto Termocontraíble',
        cantidadRequerida: totalEnvases,
        cantidadConsumida: totalEnvases,
        unidadMedida: 'UNID',
      },
      {
        insumoId: 113,
        nombreInsumo: 'Caja de Cartón Corrugado Master Planta HACCP',
        cantidadRequerida: Math.max(1, totalCajas),
        cantidadConsumida: Math.max(1, totalCajas),
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

    const request: OrdenProduccionRequest = {
      productoId: Number(val.productoId),
      nombreProducto: prod ? prod.nombre : 'Producto Agro Peruvian Foods',
      cantidadProgramada: Number(val.cantidadProgramada),
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
