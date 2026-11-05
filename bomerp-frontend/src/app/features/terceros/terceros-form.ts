import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TercerosService } from './terceros-service';
import { SunatApiService, ConsultaDocumentoResponse } from '../../core/services/sunat-api.service';

@Component({
  selector: 'app-terceros-form',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './terceros-form.html',
  styleUrls: ['./terceros-form.css'],
})
export class TercerosForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly tercerosService = inject(TercerosService);
  private readonly sunatService = inject(SunatApiService);

  readonly guardando = signal<boolean>(false);
  readonly consultando = signal<boolean>(false);
  readonly cargando = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly resultadoSunat = signal<ConsultaDocumentoResponse | null>(null);
  readonly esEdicion = signal<boolean>(false);
  readonly terceroId = signal<number | null>(null);

  form: FormGroup = this.fb.group({
    tipoDocumento: ['RUC', [Validators.required]],
    numeroDocumento: ['', [Validators.required, Validators.maxLength(30)]],
    razonSocial: ['', [Validators.required, Validators.maxLength(150)]],
    tipoTercero: ['PROVEEDOR', [Validators.required]],
    telefono: [''],
    direccion: [''],
    correo: ['', [Validators.email]],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = parseInt(idParam, 10);
      if (!isNaN(id)) {
        this.esEdicion.set(true);
        this.terceroId.set(id);
        this.cargarTercero(id);
      }
    }
  }

  cargarTercero(id: number): void {
    this.cargando.set(true);
    this.tercerosService.obtenerPorId(id).subscribe({
      next: (t) => {
        this.form.patchValue({
          tipoDocumento: t.tipoDocumento,
          numeroDocumento: t.numeroDocumento,
          razonSocial: t.razonSocial,
          tipoTercero: t.tipoTercero,
          telefono: t.telefono || '',
          direccion: t.direccion || '',
          correo: t.correo || '',
        });
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set('No se pudo cargar la información del tercero: ' + (err.error?.message || err.message));
        this.cargando.set(false);
      },
    });
  }

  consultarDocumento(): void {
    const tipo = this.form.get('tipoDocumento')?.value;
    const numero = this.form.get('numeroDocumento')?.value?.trim();

    if (!numero) {
      this.error.set('Ingrese el número de RUC o DNI antes de consultar.');
      return;
    }

    this.consultando.set(true);
    this.error.set(null);
    this.resultadoSunat.set(null);

    this.sunatService.consultarDocumento(tipo, numero).subscribe({
      next: (res) => {
        this.consultando.set(false);
        if (res && res.exito) {
          this.resultadoSunat.set(res);
          this.form.patchValue({
            razonSocial: res.razonSocial,
            direccion: res.direccion || this.form.get('direccion')?.value,
          });
        } else {
          this.error.set(res?.mensaje || 'No se encontró información en SUNAT/RENIEC para este documento.');
        }
      },
      error: () => {
        this.consultando.set(false);
        this.error.set('No se pudo conectar con el servicio de SUNAT/RENIEC. Ingrese los datos manualmente.');
      },
    });
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    this.error.set(null);

    const data = this.form.value;
    const req$ = this.esEdicion() && this.terceroId()
      ? this.tercerosService.actualizar(this.terceroId()!, data)
      : this.tercerosService.crear(data);

    req$.subscribe({
      next: () => {
        this.guardando.set(false);
        this.router.navigate(['/terceros']);
      },
      error: (err) => {
        this.error.set(err.error?.message || err.message || 'Error al procesar el tercero comercial');
        this.guardando.set(false);
      },
    });
  }
}
