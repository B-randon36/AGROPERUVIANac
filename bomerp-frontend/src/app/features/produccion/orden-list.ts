import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProduccionService } from './produccion.service';
import { OrdenProduccion, ProduccionReporte } from './produccion.model';

@Component({
  selector: 'app-orden-list',
  imports: [CommonModule, RouterLink, ReactiveFormsModule, DatePipe],
  templateUrl: './orden-list.html',
  styleUrl: './orden-list.css',
})
export class OrdenList implements OnInit {
  private readonly produccionService = inject(ProduccionService);
  private readonly fb = inject(FormBuilder);

  protected readonly ordenes = signal<OrdenProduccion[]>([]);
  protected readonly resumen = signal<ProduccionReporte | null>(null);
  protected readonly loading = signal(false);
  protected readonly descargandoPdf = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly successMsg = signal<string | null>(null);

  protected readonly ordenSeleccionada = signal<OrdenProduccion | null>(null);
  protected readonly modalCompletar = signal(false);
  protected readonly modalDetalle = signal(false);

  protected readonly formCompletar = this.fb.nonNullable.group({
    cantidadProducida: [350, [Validators.required, Validators.min(1)]],
    fechaVencimiento: ['2027-09-20', [Validators.required]],
  });

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.loading.set(true);
    this.error.set(null);

    this.produccionService.listarOrdenes().subscribe({
      next: (data) => {
        this.ordenes.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el listado de órdenes de producción.');
        this.loading.set(false);
      },
    });

    this.produccionService.reporte().subscribe({
      next: (rep) => this.resumen.set(rep),
      error: () => {},
    });
  }

  iniciar(orden: OrdenProduccion): void {
    this.loading.set(true);
    this.produccionService.iniciarOrden(orden.id).subscribe({
      next: () => {
        this.successMsg.set(`Orden ${orden.codigoOrden} iniciada en planta.`);
        this.cargarDatos();
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Error al iniciar la orden.');
        this.loading.set(false);
      },
    });
  }

  abrirCompletar(orden: OrdenProduccion): void {
    this.ordenSeleccionada.set(orden);
    this.formCompletar.patchValue({
      cantidadProducida: orden.cantidadProgramada,
    });
    this.modalCompletar.set(true);
  }

  confirmarCompletar(): void {
    const orden = this.ordenSeleccionada();
    if (!orden || this.formCompletar.invalid) return;

    this.loading.set(true);
    const val = this.formCompletar.getRawValue();

    this.produccionService.completarOrden(orden.id, val).subscribe({
      next: () => {
        this.successMsg.set(`Orden ${orden.codigoOrden} completada. Lote generado con vencimiento ${val.fechaVencimiento}.`);
        this.modalCompletar.set(false);
        this.cargarDatos();
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Error al completar la orden.');
        this.loading.set(false);
      },
    });
  }

  descargarPdf(orden: OrdenProduccion): void {
    this.descargandoPdf.set(orden.id);
    this.produccionService.descargarReportePdf(orden.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `OrdenProduccion_${orden.codigoOrden || orden.id}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        this.descargandoPdf.set(null);
      },
      error: () => {
        this.error.set(`No se pudo generar el reporte PDF para la orden ${orden.codigoOrden}.`);
        this.descargandoPdf.set(null);
      },
    });
  }

  verDetalle(orden: OrdenProduccion): void {
    this.produccionService.obtenerOrden(orden.id).subscribe({
      next: (o) => {
        this.ordenSeleccionada.set(o);
        this.modalDetalle.set(true);
      },
      error: () => {
        this.ordenSeleccionada.set(orden);
        this.modalDetalle.set(true);
      },
    });
  }
}
