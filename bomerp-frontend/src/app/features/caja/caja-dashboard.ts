import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CajaService } from './caja.service';
import { SesionCaja, CajaReporte } from './caja.model';

@Component({
  selector: 'app-caja-dashboard',
  imports: [CommonModule, RouterLink, ReactiveFormsModule, CurrencyPipe, DatePipe],
  templateUrl: './caja-dashboard.html',
  styleUrl: './caja-dashboard.css',
})
export class CajaDashboard implements OnInit {
  private readonly cajaService = inject(CajaService);
  private readonly fb = inject(FormBuilder);

  protected readonly sesionActiva = signal<SesionCaja | null>(null);
  protected readonly historial = signal<SesionCaja[]>([]);
  protected readonly resumen = signal<CajaReporte | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly successMsg = signal<string | null>(null);

  // Modales
  protected readonly modalMovimiento = signal(false);
  protected readonly modalApertura = signal(false);
  protected readonly modalCierre = signal(false);

  // Formularios Reactivos
  protected readonly formMovimiento = this.fb.nonNullable.group({
    tipo: ['INGRESO' as 'INGRESO' | 'EGRESO', [Validators.required]],
    monto: [10, [Validators.required, Validators.min(0.1)]],
    concepto: ['', [Validators.required, Validators.maxLength(100)]],
    metodoPago: ['EFECTIVO' as 'EFECTIVO' | 'YAPE' | 'PLIN' | 'TRANSFERENCIA', [Validators.required]],
    referencia: [''],
  });

  protected readonly formApertura = this.fb.nonNullable.group({
    cajeroNombre: ['Cajero de Turno', [Validators.required]],
    montoApertura: [500, [Validators.required, Validators.min(0)]],
    observaciones: ['Apertura de turno de caja diaria'],
  });

  protected readonly formCierre = this.fb.nonNullable.group({
    saldoReal: [0, [Validators.required, Validators.min(0)]],
    observaciones: ['Cierre y arqueo de caja conforme'],
  });

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.loading.set(true);
    this.error.set(null);

    this.cajaService.obtenerSesionActiva().subscribe({
      next: (sesion) => {
        this.sesionActiva.set(sesion);
        if (sesion) {
          this.formCierre.patchValue({ saldoReal: sesion.saldoTeorico });
        }
      },
      error: () => {
        this.sesionActiva.set(null);
      },
    });

    this.cajaService.listarHistorial().subscribe({
      next: (hist) => this.historial.set(hist),
      error: () => this.error.set('No se pudo cargar el historial de sesiones de caja.'),
    });

    this.cajaService.obtenerResumen().subscribe({
      next: (rep) => {
        this.resumen.set(rep);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  registrarMovimiento(): void {
    const sesion = this.sesionActiva();
    if (!sesion) return;

    if (this.formMovimiento.invalid) {
      this.formMovimiento.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    const val = this.formMovimiento.getRawValue();

    this.cajaService.registrarMovimiento(sesion.id, val).subscribe({
      next: () => {
        this.successMsg.set(`Movimiento de ${val.tipo} registrado correctamente.`);
        this.modalMovimiento.set(false);
        this.formMovimiento.reset({
          tipo: 'INGRESO',
          monto: 10,
          concepto: '',
          metodoPago: 'EFECTIVO',
          referencia: '',
        });
        this.cargarDatos();
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Error al registrar el movimiento.');
        this.loading.set(false);
      },
    });
  }

  abrirTurno(): void {
    if (this.formApertura.invalid) {
      this.formApertura.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.cajaService.abrirCaja(this.formApertura.getRawValue()).subscribe({
      next: () => {
        this.successMsg.set('Nuevo turno de caja aperturado con éxito.');
        this.modalApertura.set(false);
        this.cargarDatos();
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Error al aperturar caja.');
        this.loading.set(false);
      },
    });
  }

  cerrarTurno(): void {
    const sesion = this.sesionActiva();
    if (!sesion) return;

    this.loading.set(true);
    this.cajaService.cerrarCaja(sesion.id, this.formCierre.getRawValue()).subscribe({
      next: () => {
        this.successMsg.set('Turno de caja cerrado y arqueo asentado con éxito.');
        this.modalCierre.set(false);
        this.cargarDatos();
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Error al cerrar caja.');
        this.loading.set(false);
      },
    });
  }
}
