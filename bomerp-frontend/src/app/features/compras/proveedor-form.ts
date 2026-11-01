import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CompraService } from './compra.service';

@Component({
  selector: 'app-proveedor-form',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './proveedor-form.html',
  styleUrl: './proveedor-form.css',
})
export class ProveedorForm {
  private readonly fb = inject(FormBuilder);
  private readonly compraService = inject(CompraService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly buscandoRuc = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly rucStatus = signal<{ estado: string; condicion: string } | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    ruc: ['', [Validators.required, Validators.pattern(/^[0-9]{11}$/)]],
    razonSocial: ['', [Validators.required, Validators.maxLength(150)]],
    telefono: ['', [Validators.maxLength(20)]],
    email: ['', [Validators.email]],
    direccion: ['', [Validators.maxLength(200)]],
  });

  consultarRuc(): void {
    const ruc = this.form.controls.ruc.value.trim();
    if (!/^[0-9]{11}$/.test(ruc)) {
      this.error.set('Ingrese un RUC válido de 11 dígitos para consultar en SUNAT.');
      return;
    }

    this.buscandoRuc.set(true);
    this.error.set(null);

    this.compraService.consultarRuc(ruc).subscribe({
      next: (data) => {
        this.buscandoRuc.set(false);
        if (data.razonSocial) {
          this.form.patchValue({
            razonSocial: data.razonSocial,
            direccion: data.direccion || '',
          });
          this.rucStatus.set({
            estado: data.estado || 'ACTIVO',
            condicion: data.condicion || 'HABIDO',
          });
        }
      },
      error: (err) => {
        this.buscandoRuc.set(false);
        this.error.set(err?.error?.message || 'No se pudo consultar el RUC en SUNAT / APIsPERU.');
      },
    });
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    const val = this.form.getRawValue();

    this.compraService.crearProveedor(val).subscribe({
      next: () => {
        this.router.navigate(['/compras/proveedores']);
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Error al registrar el proveedor.');
        this.loading.set(false);
      },
    });
  }

  protected mensajeValidacion(campo: 'ruc' | 'razonSocial' | 'email'): string {
    const control = this.form.controls[campo];
    if (!control.touched) return '';

    if (control.hasError('required')) return 'Este campo es obligatorio.';
    if (control.hasError('pattern')) return 'El RUC debe tener exactamente 11 dígitos numéricos.';
    if (control.hasError('email')) return 'Ingrese un formato de correo válido.';
    return '';
  }
}
