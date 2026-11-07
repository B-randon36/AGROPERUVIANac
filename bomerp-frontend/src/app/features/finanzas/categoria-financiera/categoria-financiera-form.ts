import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CategoriaFinancieraService } from './categoria-financiera-service';
import { TipoFinanciero } from './categoria-financiera.model';

@Component({
  selector: 'app-categoria-financiera-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './categoria-financiera-form.html',
})
export class CategoriaFinancieraForm {
  private readonly fb = inject(FormBuilder);
  private readonly categoriaService = inject(CategoriaFinancieraService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly id = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);
  protected readonly errorCarga = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    tipoDefault: ['INGRESO' as TipoFinanciero, [Validators.required]],
    descripcion: ['', [Validators.maxLength(255)]],
  });

  constructor() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      this.id.set(id);
      this.loading.set(true);
      this.categoriaService.obtener(id).subscribe({
        next: (cat) => {
          this.form.patchValue({
            nombre: cat.nombre,
            tipoDefault: cat.tipoDefault ?? 'INGRESO',
            descripcion: cat.descripcion ?? '',
          });
          this.loading.set(false);
        },
        error: () => {
          this.errorCarga.set(true);
          this.error.set('No se pudo cargar la categoría financiera.');
          this.loading.set(false);
        },
      });
    }
  }

  guardar(): void {
    if (this.loading() || this.errorCarga()) return;
    this.error.set(null);

    const nombre = this.form.controls.nombre;
    nombre.setValue(nombre.value.trim());

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const valor = this.form.getRawValue();
    const id = this.id();
    const peticion = id
      ? this.categoriaService.actualizar(id, valor)
      : this.categoriaService.crear(valor);

    this.loading.set(true);
    peticion.subscribe({
      next: () => this.router.navigate(['/finanzas/categorias']),
      error: () => {
        this.error.set('No se pudo guardar la categoría financiera.');
        this.loading.set(false);
      },
    });
  }

  cancelar(): void {
    this.router.navigate(['/finanzas/categorias']);
  }
}
