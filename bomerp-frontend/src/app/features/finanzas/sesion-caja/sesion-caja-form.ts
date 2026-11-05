import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { SesionCajaService } from './sesion-caja-service';
import { CategoriaFinancieraService } from '../categoria-financiera/categoria-financiera-service';
import { CategoriaFinanciera } from '../categoria-financiera/categoria-financiera.model';
import {
  ArqueoDenominacion,
  MetodoPago,
  MovimientoCaja,
  SesionCaja,
  SesionCajaDetalle,
  TipoDenominacion,
  TipoFinanciero,
} from './sesion-caja.model';

import { AuthService } from '../../../core/auth/auth-service';

export interface ItemArqueo {
  denominacionId: number;
  valor: number;
  tipo: TipoDenominacion;
  cantidad: number;
}

@Component({
  selector: 'app-sesion-caja-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, CurrencyPipe, DatePipe],
  templateUrl: './sesion-caja-form.html',
  styleUrls: ['./sesion-caja-form.css'],
})
export class SesionCajaForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly sesionService = inject(SesionCajaService);
  private readonly categoriaService = inject(CategoriaFinancieraService);
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  // Estados
  protected readonly modoApertura = signal(false);
  protected readonly sesionId = signal<number | null>(null);
  protected readonly sesion = signal<SesionCajaDetalle | null>(null);
  protected readonly categorias = signal<CategoriaFinanciera[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly exitoMensaje = signal<string | null>(null);
  protected readonly turnoActivoExistente = signal<SesionCaja | null>(null);
  protected readonly esTurnoJornadaAnterior = signal(false);
  protected readonly saldoSugeridoTurnoAnterior = signal<number | null>(null);

  // Denominaciones de Arqueo Físico M:N (Cargadas dinámicamente desde el backend)
  protected readonly billetes = signal<ItemArqueo[]>([
    { denominacionId: 1, valor: 200, tipo: 'BILLETE', cantidad: 0 },
    { denominacionId: 2, valor: 100, tipo: 'BILLETE', cantidad: 0 },
    { denominacionId: 3, valor: 50, tipo: 'BILLETE', cantidad: 0 },
    { denominacionId: 4, valor: 20, tipo: 'BILLETE', cantidad: 0 },
    { denominacionId: 5, valor: 10, tipo: 'BILLETE', cantidad: 0 },
  ]);

  protected readonly monedas = signal<ItemArqueo[]>([
    { denominacionId: 6, valor: 5, tipo: 'MONEDA', cantidad: 0 },
    { denominacionId: 7, valor: 2, tipo: 'MONEDA', cantidad: 0 },
    { denominacionId: 8, valor: 1, tipo: 'MONEDA', cantidad: 0 },
    { denominacionId: 9, valor: 0.5, tipo: 'MONEDA', cantidad: 0 },
    { denominacionId: 10, valor: 0.2, tipo: 'MONEDA', cantidad: 0 },
    { denominacionId: 11, valor: 0.1, tipo: 'MONEDA', cantidad: 0 },
  ]);

  // Formulario Apertura
  protected readonly formApertura = this.fb.nonNullable.group({
    usuarioId: [1, [Validators.required]],
    cajero: ['Cajero de Turno', [Validators.required, Validators.maxLength(120)]],
    cajeroRol: ['ADMIN, CAJERO', [Validators.maxLength(80)]],
    saldoInicial: [300, [Validators.required, Validators.min(0)]],
    codigoSesion: [''],
  });

  // Formulario Registro Rápido de Movimientos
  protected readonly formMovimiento = this.fb.nonNullable.group({
    tipo: ['INGRESO' as TipoFinanciero, [Validators.required]],
    metodoPago: ['EFECTIVO' as MetodoPago, [Validators.required]],
    categoriaFinancieraId: [null as number | null, [Validators.required]],
    monto: [null as number | null, [Validators.required, Validators.min(0.01)]],
    descripcion: ['', [Validators.required, Validators.maxLength(255)]],
    referenciaExterna: ['', [Validators.maxLength(100)]],
  });

  // Formulario Cierre
  protected readonly formCierre = this.fb.nonNullable.group({
    observaciones: ['', [Validators.maxLength(500)]],
  });

  // Cálculos reactivos de Arqueo Físico
  protected readonly totalBilletes = computed(() =>
    this.billetes().reduce((acc, b) => acc + b.valor * b.cantidad, 0)
  );

  protected readonly totalMonedas = computed(() =>
    this.monedas().reduce((acc, m) => acc + m.valor * m.cantidad, 0)
  );

  protected readonly saldoRealArqueado = computed(() =>
    Number((this.totalBilletes() + this.totalMonedas()).toFixed(2))
  );

  protected readonly diferenciaArqueo = computed(() => {
    const esperado = this.sesion()?.saldoEsperadoEfectivo ?? 0;
    return Number((this.saldoRealArqueado() - esperado).toFixed(2));
  });

  // Estado semántico de cuadre
  protected readonly estadoCuadre = computed(() => {
    const diff = this.diferenciaArqueo();
    if (diff === 0) return 'CUADRE_EXACTO';
    return diff > 0 ? 'SOBRANTE' : 'FALTANTE';
  });

  // =========================================================================
  // REGLA RN-FIN-01 EN FRONTEND: Alerta visual reactiva de falta de liquidez
  // =========================================================================
  protected readonly alertaRnFin01 = computed(() => {
    const s = this.sesion();
    if (!s || s.estado !== 'ABIERTA') return null;

    const tipo = this.formMovimiento.controls.tipo.value;
    const metodo = this.formMovimiento.controls.metodoPago.value;
    const monto = this.formMovimiento.controls.monto.value ?? 0;

    if (tipo === 'EGRESO' && metodo === 'EFECTIVO' && monto > s.saldoEsperadoEfectivo) {
      return `RN-FIN-01: El monto a retirar (S/ ${monto.toFixed(2)}) supera el saldo disponible en gaveta (S/ ${s.saldoEsperadoEfectivo.toFixed(2)}). La operación será bloqueada.`;
    }
    return null;
  });

  ngOnInit(): void {
    this.cargarCategorias();
    this.cargarDenominaciones();

    // Auto-asignación de cajero y rol desde la sesión Keycloak / JWT activa
    const usuarioAuth = this.authService.usuario();
    if (usuarioAuth?.nombre) {
      this.formApertura.patchValue({ cajero: usuarioAuth.nombre });
    }
    const roles = this.authService.roles();
    if (roles && roles.length > 0) {
      const rolesRelevantes = roles.filter(
        (r) => !r.startsWith('default-roles') && r !== 'offline_access' && r !== 'uma_authorization'
      );
      const rolStr = rolesRelevantes.length > 0 ? rolesRelevantes.join(', ') : 'CAJERO';
      this.formApertura.patchValue({ cajeroRol: rolStr });
    }

    const idParam = this.route.snapshot.paramMap.get('id');

    if (idParam && idParam !== 'nueva') {
      const id = Number(idParam);
      this.sesionId.set(id);
      this.cargarDetalle(id);
    } else {
      // Intento de apertura de nuevo turno diario
      this.modoApertura.set(true);

      // Auto-generar código de sesión diario correlativo sugerido
      const hoyStr = new Date().toISOString().slice(0, 10).replace(/-/g, '');
      const horaStr = new Date().toTimeString().slice(0, 5).replace(/:/g, '');
      this.formApertura.patchValue({
        codigoSesion: `SES-${hoyStr}-${horaStr}`,
      });

      // Continuidad de fondo de caja: consultar el último turno cerrado
      this.sesionService.listar().subscribe({
        next: (sesiones) => {
          const ultimoCerrado = sesiones.find((s) => s.estado === 'CERRADA');
          if (ultimoCerrado && ultimoCerrado.saldoRealEfectivo != null) {
            this.saldoSugeridoTurnoAnterior.set(ultimoCerrado.saldoRealEfectivo);
          }
        },
      });

      // Verificar si ya existe una sesión abierta
      this.sesionService.obtenerActiva().subscribe({
        next: (activa) => {
          this.turnoActivoExistente.set(activa);
          // Evaluar si es un turno olvidado de una jornada/día anterior
          if (activa.fechaApertura) {
            const fechaAperturaDia = new Date(activa.fechaApertura).toISOString().slice(0, 10);
            const hoyDia = new Date().toISOString().slice(0, 10);
            this.esTurnoJornadaAnterior.set(fechaAperturaDia !== hoyDia);
          }
        },
        error: () => {
          this.turnoActivoExistente.set(null);
          this.esTurnoJornadaAnterior.set(false);
        },
      });
    }
  }

  cargarDenominaciones(): void {
    this.sesionService.listarDenominaciones().subscribe({
      next: (catalogo) => {
        if (catalogo && catalogo.length > 0) {
          const bills: ItemArqueo[] = catalogo
            .filter((d) => d.tipo === 'BILLETE')
            .sort((a, b) => b.valorFacial - a.valorFacial)
            .map((d) => ({
              denominacionId: d.id,
              valor: d.valorFacial,
              tipo: 'BILLETE',
              cantidad: 0,
            }));

          const coins: ItemArqueo[] = catalogo
            .filter((d) => d.tipo === 'MONEDA')
            .sort((a, b) => b.valorFacial - a.valorFacial)
            .map((d) => ({
              denominacionId: d.id,
              valor: d.valorFacial,
              tipo: 'MONEDA',
              cantidad: 0,
            }));

          if (bills.length > 0) this.billetes.set(bills);
          if (coins.length > 0) this.monedas.set(coins);
        }
      },
      error: () => {
        console.warn('Usando catálogo local de denominaciones del Perú.');
      },
    });
  }

  irATurnoActivo(): void {
    const activa = this.turnoActivoExistente();
    if (activa) {
      this.sesionId.set(activa.id);
      this.cargarDetalle(activa.id);
    }
  }

  aplicarSaldoSugerido(): void {
    const saldo = this.saldoSugeridoTurnoAnterior();
    if (saldo != null) {
      this.formApertura.patchValue({ saldoInicial: saldo });
    }
  }

  cargarCategorias(): void {
    this.categoriaService.listar().subscribe({
      next: (cats) => {
        this.categorias.set(cats);
        if (cats.length > 0) {
          this.formMovimiento.patchValue({ categoriaFinancieraId: cats[0].id });
        }
      },
    });
  }

  cargarDetalle(id: number): void {
    this.loading.set(true);
    this.sesionService.obtenerDetalle(id).subscribe({
      next: (data) => {
        this.sesion.set(data);
        this.modoApertura.set(false);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la información del turno de caja.');
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
    this.error.set(null);
    const val = this.formApertura.getRawValue();

    this.sesionService.abrir(val).subscribe({
      next: (s) => {
        this.exitoMensaje.set(`Turno de caja ${s.codigoSesion} abierto con éxito.`);
        this.sesionId.set(s.id);
        this.cargarDetalle(s.id);
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'No se pudo abrir el turno de caja.');
        this.loading.set(false);
      },
    });
  }

  registrarMovimiento(): void {
    if (this.alertaRnFin01()) return;

    if (this.formMovimiento.invalid) {
      this.formMovimiento.markAllAsTouched();
      return;
    }

    const id = this.sesionId();
    if (!id) return;

    const val = this.formMovimiento.getRawValue();
    if (!val.categoriaFinancieraId || !val.monto) return;

    this.loading.set(true);
    this.error.set(null);

    this.sesionService
      .registrarMovimiento(id, {
        tipo: val.tipo,
        metodoPago: val.metodoPago,
        categoriaFinancieraId: val.categoriaFinancieraId,
        monto: val.monto,
        descripcion: val.descripcion,
        referenciaExterna: val.referenciaExterna,
      })
      .subscribe({
        next: () => {
          this.exitoMensaje.set('Movimiento registrado correctamente.');
          this.formMovimiento.reset({
            tipo: 'INGRESO',
            metodoPago: 'EFECTIVO',
            categoriaFinancieraId: this.categorias()[0]?.id ?? null,
            monto: null,
            descripcion: '',
            referenciaExterna: '',
          });
          this.cargarDetalle(id);
        },
        error: (err) => {
          this.error.set(err?.error?.message || 'Error al registrar el movimiento.');
          this.loading.set(false);
        },
      });
  }

  actualizarCantidadBillete(index: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    const valor = Math.max(0, parseInt(input.value || '0', 10));
    const lista = [...this.billetes()];
    lista[index] = { ...lista[index], cantidad: valor };
    this.billetes.set(lista);
  }

  incrementarBillete(index: number): void {
    const lista = [...this.billetes()];
    lista[index] = { ...lista[index], cantidad: (lista[index].cantidad || 0) + 1 };
    this.billetes.set(lista);
  }

  decrementarBillete(index: number): void {
    const lista = [...this.billetes()];
    const cant = Math.max(0, (lista[index].cantidad || 0) - 1);
    lista[index] = { ...lista[index], cantidad: cant };
    this.billetes.set(lista);
  }

  actualizarCantidadMoneda(index: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    const valor = Math.max(0, parseInt(input.value || '0', 10));
    const lista = [...this.monedas()];
    lista[index] = { ...lista[index], cantidad: valor };
    this.monedas.set(lista);
  }

  incrementarMoneda(index: number): void {
    const lista = [...this.monedas()];
    lista[index] = { ...lista[index], cantidad: (lista[index].cantidad || 0) + 1 };
    this.monedas.set(lista);
  }

  decrementarMoneda(index: number): void {
    const lista = [...this.monedas()];
    const cant = Math.max(0, (lista[index].cantidad || 0) - 1);
    lista[index] = { ...lista[index], cantidad: cant };
    this.monedas.set(lista);
  }

  cerrarTurnoCaja(): void {
    const id = this.sesionId();
    if (!id) return;

    const saldoReal = this.saldoRealArqueado();
    const obs = this.formCierre.controls.observaciones.value;

    if (!confirm(`¿Confirmas el cierre del turno con un arqueo físico de S/ ${saldoReal.toFixed(2)}?`)) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const desglose: import('./sesion-caja.model').DesgloseEfectivoItem[] = [
      ...this.billetes(),
      ...this.monedas(),
    ]
      .filter((d) => d.cantidad > 0)
      .map((d) => ({
        denominacionId: d.denominacionId,
        valorFacial: d.valor,
        tipo: d.tipo,
        cantidad: d.cantidad,
        subtotal: Number((d.valor * d.cantidad).toFixed(2)),
      }));

    this.sesionService
      .cerrar(id, {
        saldoRealEfectivo: saldoReal,
        observaciones: obs,
        desglose,
      })
      .subscribe({
        next: (cerrada) => {
          this.exitoMensaje.set(`Turno ${cerrada.codigoSesion} cerrado exitosamente.`);
          this.cargarDetalle(id);
        },
        error: (err) => {
          this.error.set(err?.error?.message || 'No se pudo cerrar el turno de caja.');
          this.loading.set(false);
        },
      });
  }

  abrirNuevoTurnoDiario(): void {
    this.router.navigate(['/finanzas/sesiones/nueva']).then(() => {
      this.modoApertura.set(true);
      this.sesion.set(null);
      this.sesionId.set(null);
      this.turnoActivoExistente.set(null);
      const hoyStr = new Date().toISOString().slice(0, 10).replace(/-/g, '');
      const horaStr = new Date().toTimeString().slice(0, 5).replace(/:/g, '');
      const roles = this.authService.roles();
      const rolesRelevantes = roles.filter(
        (r) => !r.startsWith('default-roles') && r !== 'offline_access' && r !== 'uma_authorization'
      );
      const rolStr = rolesRelevantes.length > 0 ? rolesRelevantes.join(', ') : 'ADMIN, CAJERO';
      this.formApertura.reset({
        usuarioId: 1,
        cajero: this.authService.usuario()?.nombre || 'Cajero de Turno',
        cajeroRol: rolStr,
        saldoInicial: 300,
        codigoSesion: `SES-${hoyStr}-${horaStr}`,
      });
    });
  }

  imprimirTicket(): void {
    window.print();
  }

  volver(): void {
    this.router.navigate(['/finanzas/sesiones']);
  }
}
