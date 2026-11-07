import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { VentaService } from './venta.service';
import { Venta, VentaReporte } from './venta.model';

@Component({
  selector: 'app-venta-list',
  imports: [CommonModule, RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './venta-list.html',
  styleUrl: './venta-list.css',
})
export class VentaList implements OnInit {
  private readonly ventaService = inject(VentaService);

  protected readonly ventas = signal<Venta[]>([]);
  protected readonly resumen = signal<VentaReporte | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly successMsg = signal<string | null>(null);

  protected readonly ventaSeleccionada = signal<Venta | null>(null);
  protected readonly modalTicket = signal(false);

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.loading.set(true);
    this.error.set(null);

    this.ventaService.buscar().subscribe({
      next: (data) => {
        this.ventas.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el historial de ventas.');
        this.loading.set(false);
      },
    });

    this.ventaService.reporte().subscribe({
      next: (rep) => this.resumen.set(rep),
      error: () => {},
    });
  }

  verTicket(v: Venta): void {
    this.ventaService.obtener(v.id).subscribe({
      next: (ventaCompleta) => {
        this.ventaSeleccionada.set(ventaCompleta);
        this.modalTicket.set(true);
      },
      error: () => {
        this.ventaSeleccionada.set(v);
        this.modalTicket.set(true);
      },
    });
  }

  imprimirTicket(): void {
    window.print();
  }

  anularVenta(id: number): void {
    if (!confirm(`¿Está seguro de anular la venta #${id}? Esta acción repondrá automáticamente el stock de los productos.`)) {
      return;
    }

    this.loading.set(true);
    this.ventaService.anular(id).subscribe({
      next: () => {
        this.successMsg.set(`Venta #${id} anulada con éxito y stock repuesto en catálogo.`);
        this.cargarDatos();
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Error al anular la venta.');
        this.loading.set(false);
      },
    });
  }
}
