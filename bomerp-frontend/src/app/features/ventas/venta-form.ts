import { Component, OnInit, OnDestroy, computed, inject, signal, ViewChild, ElementRef, ChangeDetectorRef, HostListener } from '@angular/core';
import { CurrencyPipe, CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { BrowserMultiFormatReader, IScannerControls } from '@zxing/browser';
import { ProductoService } from '../catalogo/producto/producto.service';
import { Producto } from '../catalogo/producto/producto.model';
import { VentaService } from './venta.service';
import { VentaResponse, DetalleVentaResponse, MetodoPagoPos } from './venta.model';
import { SesionCajaService } from '../finanzas/sesion-caja/sesion-caja-service';
import { SesionCaja } from '../finanzas/sesion-caja/sesion-caja.model';
import { AuthService } from '../../core/auth/auth-service';

@Component({
  selector: 'app-venta-form',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    CurrencyPipe,
    MatAutocompleteModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './venta-form.html',
  styleUrl: './venta-form.css',
})
export class VentaForm implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly productoService = inject(ProductoService);
  private readonly ventaService = inject(VentaService);
  private readonly sesionCajaService = inject(SesionCajaService);
  protected readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  @ViewChild('videoScanner') videoScannerRef?: ElementRef<HTMLVideoElement>;
  private codeReader: BrowserMultiFormatReader | null = null;
  private scannerControls: IScannerControls | null = null;
  private ultimoCodigoTiempo = 0;
  private ultimoCodigoValor = '';

  protected readonly productos = signal<Producto[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);
  protected readonly buscandoDni = signal(false);

  // Estados de integración con Sesión de Caja Activa (Turno POS)
  protected readonly sesionCajaActiva = signal<SesionCaja | null>(null);
  protected readonly cargandoSesion = signal(false);
  protected readonly mostrarModalAperturaCaja = signal(false);
  protected readonly saldoInicialApertura = signal<number>(200);
  protected readonly abriendoCaja = signal(false);
  protected readonly errorAperturaCaja = signal<string | null>(null);

  // Estados para el escáner de código de barras por cámara
  protected readonly mostrarModalEscaner = signal(false);
  protected readonly camarasDisponibles = signal<MediaDeviceInfo[]>([]);
  protected readonly camaraSeleccionadaId = signal<string>('');
  protected readonly escaneando = signal(false);
  protected readonly errorCamara = signal<string | null>(null);
  protected readonly ultimoEscaneado = signal<string | null>(null);
  protected readonly mensajeEscaneo = signal<{ texto: string; exito: boolean } | null>(null);
  protected readonly flashEscaneo = signal(false);

  // Detección de pistola lectora de código de barras por hardware USB
  protected readonly toastEscaneoHardware = signal<{ nombre: string; total: number; cantidad: number } | null>(null);
  private toastTimer: any = null;
  private scannerGunBuffer = '';
  private scannerGunLastKeyTime = 0;

  // Reactividad forzada para zoneless
  protected readonly versionDetalles = signal(0);

  // Estados para la previsualización del ticket térmico
  protected readonly ventaGenerada = signal<VentaResponse | null>(null);
  protected readonly mostrarModalTicket = signal(false);
  protected readonly esBorrador = signal(false);

  // Estados para la calculadora de cobro en efectivo y cambio (vuelto)
  protected readonly montoRecibido = signal<number | null>(null);
  protected readonly billetesComunes = [10, 20, 50, 100, 200];
  protected readonly Math = Math;

  protected readonly ticketMontoRecibido = signal<number | null>(null);
  protected readonly ticketCambio = signal<number | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    clienteNombre: ['Mercado Mayorista de Frutas Lima', [Validators.maxLength(100)]],
    clienteDocumento: [''],
    metodoPago: ['EFECTIVO' as MetodoPagoPos],
    diasCredito: [30, [Validators.min(1), Validators.max(180)]],
    detalles: this.fb.array([this.crearLinea()], [Validators.minLength(1)]),
  });

  protected readonly metodoPagoActual = toSignal(
    this.form.controls.metodoPago.valueChanges,
    { initialValue: this.form.controls.metodoPago.value },
  );

  protected readonly esEfectivo = computed(
    () => this.metodoPagoActual() === 'EFECTIVO'
  );

  protected readonly cambio = computed(() => {
    const recibido = this.montoRecibido();
    if (recibido === null || recibido === undefined || isNaN(recibido)) {
      return null;
    }
    const tot = this.total();
    return Math.round((recibido - tot) * 100) / 100;
  });

  protected get lineasForm() {
    return this.form.controls.detalles;
  }

  protected readonly valoresDetalles = toSignal(
    this.form.controls.detalles.valueChanges,
    { initialValue: this.form.controls.detalles.getRawValue() },
  );

  protected readonly subtotales = computed(() => {
    this.versionDetalles();
    return this.valoresDetalles().map((linea) => {
      const producto = this.productos().find((p) => p.id === linea.productoId);
      return (producto?.precio ?? 0) * (linea.cantidad ?? 0);
    });
  });

  protected readonly total = computed(() => {
    this.versionDetalles();
    return this.subtotales().reduce((suma, s) => suma + s, 0);
  });

  protected readonly preciosUnitarios = computed(() => {
    this.versionDetalles();
    return this.valoresDetalles().map((linea) => {
      const producto = this.productos().find((p) => p.id === linea.productoId);
      return producto?.precio ?? 0;
    });
  });

  protected readonly opcionesPorLinea = computed(() => {
    this.versionDetalles();
    return this.valoresDetalles().map((linea) => {
      const texto = (linea.busqueda ?? '').toLowerCase();
      return this.productos().filter((p) => p.nombre.toLowerCase().includes(texto));
    });
  });

  // Lista viva de productos agregados en el ticket para visualización y control dentro del escáner
  protected readonly itemsEnCarrito = computed(() => {
    this.versionDetalles();
    const lineas = this.lineasForm.getRawValue();
    return lineas
      .map((l, index) => {
        const prod = this.productos().find((p) => p.id === l.productoId);
        return {
          index,
          productoId: l.productoId,
          producto: prod,
          nombre: prod?.nombre ?? l.busqueda ?? `Producto #${l.productoId}`,
          precio: prod?.precio ?? 0,
          cantidad: l.cantidad ?? 1,
          subtotal: (prod?.precio ?? 0) * (l.cantidad ?? 1),
        };
      })
      .filter((item) => item.productoId > 0 && item.cantidad > 0);
  });

  protected readonly totalUnidadesEscaneadas = computed(() =>
    this.itemsEnCarrito().reduce((sum, item) => sum + item.cantidad, 0),
  );

  ngOnInit(): void {
    this.cargarProductos();
    this.cargarSesionCajaActiva();
  }

  protected cargarSesionCajaActiva(): void {
    this.cargandoSesion.set(true);
    this.sesionCajaService.obtenerActiva().subscribe({
      next: (sesion) => {
        this.sesionCajaActiva.set(sesion);
        this.cargandoSesion.set(false);
      },
      error: () => {
        this.sesionCajaActiva.set(null);
        this.cargandoSesion.set(false);
      },
    });
  }

  protected abrirModalApertura(): void {
    this.errorAperturaCaja.set(null);
    this.mostrarModalAperturaCaja.set(true);
  }

  protected cerrarModalApertura(): void {
    this.mostrarModalAperturaCaja.set(false);
    this.errorAperturaCaja.set(null);
  }

  protected confirmarAperturaCaja(): void {
    const usuario = this.authService.usuario();
    const cajeroNombre = usuario?.nombre || 'Brandon Ccalla';
    const roles = this.authService.roles() || [];
    const rol = roles.includes('VENDEDOR') ? 'VENDEDOR, CAJERO' : (roles[0] || 'CAJERO');
    const saldo = Number(this.saldoInicialApertura());

    if (isNaN(saldo) || saldo < 0) {
      this.errorAperturaCaja.set('El saldo inicial debe ser un número mayor o igual a cero.');
      return;
    }

    this.abriendoCaja.set(true);
    this.errorAperturaCaja.set(null);

    this.sesionCajaService
      .abrir({
        usuarioId: this.authService.usuarioId(),
        cajero: cajeroNombre,
        cajeroRol: rol,
        saldoInicial: saldo,
      })
      .subscribe({
        next: (sesionCreada) => {
          this.abriendoCaja.set(false);
          this.sesionCajaActiva.set(sesionCreada);
          this.mostrarModalAperturaCaja.set(false);
        },
        error: (err) => {
          this.abriendoCaja.set(false);
          this.errorAperturaCaja.set(
            err?.error?.message || 'No se pudo aperturar el turno de caja.'
          );
        },
      });
  }

  private cargarProductos(): void {
    this.productoService.listar().subscribe({
      next: (data) => this.productos.set(data),
      error: () => this.error.set('No se pudieron cargar los productos del catálogo.'),
    });
  }

  protected buscarPorDni(dni: string): void {
    const limpio = dni ? dni.trim() : '';
    if (!/^[0-9]{8}$/.test(limpio)) {
      this.error.set('El DNI debe contener exactamente 8 dígitos numéricos.');
      return;
    }
    this.buscandoDni.set(true);
    this.error.set(null);
    this.ventaService.consultarDni(limpio).subscribe({
      next: (datos) => {
        this.buscandoDni.set(false);
        const nombre = datos.nombreCompleto || `${datos.nombres ?? ''} ${datos.apellidoPaterno ?? ''} ${datos.apellidoMaterno ?? ''}`.trim();
        this.form.controls.clienteNombre.setValue(nombre);
        this.form.controls.clienteDocumento.setValue(limpio);
      },
      error: (err) => {
        this.buscandoDni.set(false);
        this.error.set(err.error?.message ?? 'No se pudo consultar el DNI ingresado.');
      },
    });
  }

  private crearLinea() {
    return this.fb.nonNullable.group({
      productoId: [0, [Validators.min(1)]],
      busqueda: [''],
      cantidad: [1, [Validators.required, Validators.min(1)]],
    });
  }

  protected agregarLinea(): void {
    this.lineasForm.push(this.crearLinea());
    this.versionDetalles.update((v) => v + 1);
    this.cdr.markForCheck();
  }

  protected quitarLinea(indice: number): void {
    if (this.lineasForm.length > 1) {
      this.lineasForm.removeAt(indice);
    } else {
      this.lineasForm.at(0).patchValue({
        productoId: 0,
        busqueda: '',
        cantidad: 1,
      });
    }
    this.versionDetalles.update((v) => v + 1);
    this.cdr.markForCheck();
  }

  protected aumentarCantidadLinea(indice: number): void {
    if (indice >= 0 && indice < this.lineasForm.length) {
      const linea = this.lineasForm.at(indice);
      const cant = linea.controls.cantidad.value || 0;
      linea.controls.cantidad.setValue(cant + 1);
      linea.controls.cantidad.markAsDirty();
      this.versionDetalles.update((v) => v + 1);
      this.emitirBeepPos();
      this.cdr.markForCheck();
    }
  }

  protected disminuirCantidadLinea(indice: number): void {
    if (indice >= 0 && indice < this.lineasForm.length) {
      const linea = this.lineasForm.at(indice);
      const cant = linea.controls.cantidad.value || 0;
      if (cant > 1) {
        linea.controls.cantidad.setValue(cant - 1);
        linea.controls.cantidad.markAsDirty();
      } else {
        this.quitarLinea(indice);
      }
      this.versionDetalles.update((v) => v + 1);
      this.cdr.markForCheck();
    }
  }

  protected seleccionarProducto(indice: number, evento: MatAutocompleteSelectedEvent): void {
    const producto: Producto = evento.option.value;
    this.lineasForm.at(indice).patchValue({
      productoId: producto.id,
      busqueda: producto.nombre,
    });
    this.versionDetalles.update((v) => v + 1);
    this.cdr.markForCheck();
  }

  protected onMontoRecibidoChange(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    if (val === '' || val === null || val === undefined) {
      this.montoRecibido.set(null);
    } else {
      const num = parseFloat(val);
      this.montoRecibido.set(isNaN(num) ? null : num);
    }
  }

  protected setMontoRecibido(monto: number): void {
    this.montoRecibido.set(monto);
  }

  protected setMontoExacto(): void {
    this.montoRecibido.set(this.total());
  }

  protected limpiarMontoRecibido(): void {
    this.montoRecibido.set(null);
  }

  protected previsualizarBorrador(): void {
    const lineas = this.lineasForm.getRawValue();
    const itemsValidos = lineas.filter((l) => l.productoId > 0 && l.cantidad > 0);

    if (itemsValidos.length === 0) {
      this.error.set('Selecciona al menos un producto válido para previsualizar el ticket.');
      return;
    }

    const detalles: DetalleVentaResponse[] = itemsValidos.map((l) => {
      const prod = this.productos().find((p) => p.id === l.productoId);
      const precio = prod?.precio ?? 0;
      return {
        productoId: l.productoId,
        nombreProducto: prod?.nombre ?? `Producto #${l.productoId}`,
        precioUnitario: precio,
        cantidad: l.cantidad,
        subtotal: precio * l.cantidad,
      };
    });

    const tot = this.total();
    const sub = Math.round((tot / 1.18) * 100) / 100;
    const igv = Math.round((tot - sub) * 100) / 100;

    this.ticketMontoRecibido.set(this.esEfectivo() ? this.montoRecibido() : null);
    this.ticketCambio.set(this.esEfectivo() ? this.cambio() : null);

    this.ventaGenerada.set({
      id: 0,
      numeroTicket: 'TKT-VISTA-PREVIA',
      fecha: new Date().toISOString(),
      estado: 'BORRADOR',
      clienteNombre: this.form.controls.clienteNombre.value || 'Cliente General',
      metodoPago: this.form.controls.metodoPago.value,
      subtotal: sub,
      igv: igv,
      total: tot,
      detalles,
    });

    this.esBorrador.set(true);
    this.mostrarModalTicket.set(true);
  }

  protected confirmarYGuardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const esEfec = this.form.controls.metodoPago.value === 'EFECTIVO';
    if (esEfec && !this.sesionCajaActiva()) {
      if (confirm('Atención de Caja: No tienes un turno de caja abierto para registrar ventas en efectivo. ¿Deseas aperturar tu sesión de caja ahora para poder rendir cuentas en el arqueo?')) {
        this.abrirModalApertura();
        return;
      }
    }

    if (esEfec && this.montoRecibido() !== null && this.cambio() !== null && this.cambio()! < 0) {
      this.error.set(`El efectivo recibido (S/ ${this.montoRecibido()!.toFixed(2)}) es menor al total de la venta (S/ ${this.total().toFixed(2)}). Faltan S/ ${Math.abs(this.cambio()!).toFixed(2)}.`);
      return;
    }

    const cantidadLineas = this.lineasForm.length;
    const esCred = this.form.controls.metodoPago.value === 'CREDITO';
    const sesionTexto = this.sesionCajaActiva() ? ` (Caja: ${this.sesionCajaActiva()!.codigoSesion})` : '';
    const notaVuelto = esEfec && this.cambio() !== null && this.cambio()! >= 0
      ? ` | Paga con: S/ ${this.montoRecibido()!.toFixed(2)} (Vuelto: S/ ${this.cambio()!.toFixed(2)})`
      : '';

    const notaIntegracion = esCred
      ? ` (Se generará Cuenta por Cobrar a ${this.form.controls.diasCredito.value} días + Despacho FEFO)`
      : ` (Impactará Sesión de Caja activa${sesionTexto} + Despacho FEFO)${notaVuelto}`;

    const resumen = `Vas a registrar una venta de ${cantidadLineas} línea(s) por un total de S/ ${this.total().toFixed(2)}${notaIntegracion}. ¿Confirmar?`;
    if (!confirm(resumen)) return;
    this.guardar();
  }

  private guardar(): void {
    this.error.set(null);
    this.loading.set(true);
    const rawVal = this.form.getRawValue();
    const detalles = this.lineasForm
      .getRawValue()
      .map(({ productoId, cantidad }) => ({ productoId, cantidad }));

    this.ventaService
      .crear({
        clienteNombre: rawVal.clienteNombre,
        clienteDocumento: rawVal.clienteDocumento ? rawVal.clienteDocumento.trim() : undefined,
        diasCredito: rawVal.metodoPago === 'CREDITO' ? Number(rawVal.diasCredito) : undefined,
        metodoPago: rawVal.metodoPago,
        detalles,
      })
      .subscribe({
        next: (ventaCreada) => {
          this.loading.set(false);
          this.ticketMontoRecibido.set(this.esEfectivo() ? this.montoRecibido() : null);
          this.ticketCambio.set(this.esEfectivo() ? this.cambio() : null);
          this.ventaGenerada.set(ventaCreada);
          this.esBorrador.set(false);
          this.mostrarModalTicket.set(true);
          this.cargarProductos();
          this.cargarSesionCajaActiva();
        },
        error: (err: HttpErrorResponse) => this.manejarErrorGuardado(err),
      });
  }

  protected imprimirTicket(): void {
    window.print();
  }

  protected cerrarModalTicket(): void {
    const eraBorrador = this.esBorrador();
    this.mostrarModalTicket.set(false);
    this.ventaGenerada.set(null);
    if (!eraBorrador) {
      this.nuevaVenta();
    }
  }

  protected nuevaVenta(): void {
    this.mostrarModalTicket.set(false);
    this.ventaGenerada.set(null);
    this.montoRecibido.set(null);
    this.ticketMontoRecibido.set(null);
    this.ticketCambio.set(null);
    this.form.reset({
      clienteNombre: 'Cliente General',
      clienteDocumento: '',
      metodoPago: 'EFECTIVO',
      diasCredito: 30,
    });
    this.lineasForm.clear();
    this.agregarLinea();
    this.versionDetalles.set(0);
    this.error.set(null);
    this.cargarSesionCajaActiva();
    this.cdr.markForCheck();
  }

  protected irAReporte(): void {
    this.mostrarModalTicket.set(false);
    this.router.navigate(['/ventas/reporte']);
  }

  ngOnDestroy(): void {
    this.detenerEscaner();
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
    }
  }

  // =========================================================================
  // DETECCIÓN GLOBAL DE PISTOLA LECTORA DE CÓDIGOS DE BARRA USB (HARDWARE)
  // =========================================================================
  @HostListener('window:keydown', ['$event'])
  onGlobalKeyDown(event: KeyboardEvent): void {
    // Si estamos en un modal de confirmación o ticket, no interceptar
    if (this.mostrarModalTicket() || this.mostrarModalAperturaCaja()) {
      return;
    }

    const target = event.target as HTMLElement;
    const esInputTexto = target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA');

    const ahora = Date.now();
    const tiempoDesdeUltimaTecla = ahora - this.scannerGunLastKeyTime;
    this.scannerGunLastKeyTime = ahora;

    if (event.key === 'Enter') {
      const buffer = this.scannerGunBuffer.trim();
      this.scannerGunBuffer = '';
      if (buffer.length >= 1) {
        // Intercepta si los caracteres llegaron a velocidad de hardware (< 80ms entre teclas) o si no estamos en un input
        if (tiempoDesdeUltimaTecla < 80 || !esInputTexto) {
          event.preventDefault();
          event.stopPropagation();
          this.procesarCodigoEscaneado(buffer, true);
        }
      }
    } else if (event.key.length === 1) {
      if (tiempoDesdeUltimaTecla > 110) {
        this.scannerGunBuffer = '';
      }
      this.scannerGunBuffer += event.key;
    }
  }

  private mostrarToastHardware(nombre: string, cantidad: number, total: number): void {
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
    }
    this.toastEscaneoHardware.set({ nombre, cantidad, total });
    this.cdr.markForCheck();
    this.toastTimer = setTimeout(() => {
      this.toastEscaneoHardware.set(null);
      this.cdr.markForCheck();
    }, 3200);
  }

  // =========================================================================
  // FUNCIONALIDAD DE ESCÁNER DE CÓDIGO DE BARRAS (CÁMARA LAPTOP / CELULAR)
  // =========================================================================
  protected async abrirEscanerCamara(): Promise<void> {
    this.mostrarModalEscaner.set(true);
    this.errorCamara.set(null);
    this.mensajeEscaneo.set(null);
    this.escaneando.set(true);

    try {
      if (!this.codeReader) {
        this.codeReader = new BrowserMultiFormatReader();
      }

      const devices = await BrowserMultiFormatReader.listVideoInputDevices();
      this.camarasDisponibles.set(devices);

      if (devices.length === 0) {
        this.errorCamara.set('No se detectó ninguna cámara en este equipo (laptop o móvil).');
        this.escaneando.set(false);
        this.cdr.markForCheck();
        return;
      }

      const camaraTrasera = devices.find((d) => /back|rear|trasera|environment/i.test(d.label));
      const deviceId = camaraTrasera ? camaraTrasera.deviceId : devices[0].deviceId;
      this.camaraSeleccionadaId.set(deviceId);

      setTimeout(() => this.iniciarCaptura(deviceId), 150);
    } catch (err: unknown) {
      console.error('Error al inicializar cámara:', err);
      this.errorCamara.set('Permiso de cámara no otorgado o navegador sin soporte de medios.');
      this.escaneando.set(false);
      this.cdr.markForCheck();
    }
  }

  protected async cambiarCamara(deviceId: string): Promise<void> {
    this.camaraSeleccionadaId.set(deviceId);
    this.detenerEscaner();
    this.iniciarCaptura(deviceId);
  }

  private async iniciarCaptura(deviceId: string): Promise<void> {
    if (!this.videoScannerRef?.nativeElement || !this.codeReader) return;

    try {
      this.detenerEscaner();
      this.scannerControls = await this.codeReader.decodeFromVideoDevice(
        deviceId,
        this.videoScannerRef.nativeElement,
        (result) => {
          if (result) {
            this.procesarCodigoEscaneado(result.getText(), false);
          }
        },
      );
      this.escaneando.set(true);
      this.cdr.markForCheck();
    } catch (err: unknown) {
      console.error('Error al iniciar stream de video:', err);
      this.errorCamara.set('No se pudo acceder a la transmisión de video de la cámara seleccionada.');
      this.escaneando.set(false);
      this.cdr.markForCheck();
    }
  }

  protected detenerEscaner(): void {
    if (this.scannerControls) {
      this.scannerControls.stop();
      this.scannerControls = null;
    }
    this.escaneando.set(false);
    this.cdr.markForCheck();
  }

  protected cerrarEscanerCamara(): void {
    this.detenerEscaner();
    this.mostrarModalEscaner.set(false);
    this.mensajeEscaneo.set(null);
    this.cdr.markForCheck();
  }

  protected procesarCodigoEscaneado(codigoRaw: string, esDirecto = false): void {
    const ahora = Date.now();
    const codigo = (codigoRaw || '').trim();
    if (!codigo) return;

    // Control de debounce para escaneo continuo con cámara:
    // Si es el MISMO código que el anterior, esperar 900ms para no duplicar en 1 solo fotograma
    // Si es un código DISTINTO, permitir escaneo inmediato tras 300ms
    // Si es directo (pistola hardware, input manual o clic de prueba), 0ms de espera
    if (!esDirecto) {
      const mismoCodigo = codigo === this.ultimoCodigoValor;
      const cooldown = mismoCodigo ? 900 : 300;
      if (ahora - this.ultimoCodigoTiempo < cooldown) {
        return;
      }
    }

    this.ultimoCodigoTiempo = ahora;
    this.ultimoCodigoValor = codigo;
    this.ultimoEscaneado.set(codigo);

    const lista = this.productos();
    let producto = lista.find((p) => p.id === Number(codigo));

    if (!producto && /^7751\d{8}\d$/.test(codigo)) {
      const idCalculado = parseInt(codigo.substring(4, 12), 10);
      producto = lista.find((p) => p.id === idCalculado);
    }

    if (!producto) {
      const matchPrefijo = codigo.match(/(?:PROD|ID|SKU|#)[-:\s]*(\d+)/i);
      if (matchPrefijo) {
        const idExtraido = parseInt(matchPrefijo[1], 10);
        producto = lista.find((p) => p.id === idExtraido);
      }
    }

    if (!producto) {
      const num = parseInt(codigo, 10);
      if (!isNaN(num) && num > 0 && num <= 1000) {
        producto = lista.find((p) => p.id === num);
      }
    }

    if (!producto && codigo.startsWith('{') && codigo.endsWith('}')) {
      try {
        const obj = JSON.parse(codigo);
        if (obj && obj.id) {
          producto = lista.find((p) => p.id === Number(obj.id));
        }
      } catch {
        // no-op
      }
    }

    if (!producto) {
      const codLower = codigo.toLowerCase();
      producto = lista.find((p) => p.nombre.toLowerCase().includes(codLower));
    }

    if (producto) {
      this.emitirBeepPos();
      this.activarFlashEscaneo();
      this.agregarProductoAlCarrito(producto);

      const cantTotal = this.obtenerCantidadProducto(producto.id);
      this.mensajeEscaneo.set({
        texto: `¡Escaneado con éxito! "${producto.nombre}" (+1 un. · En ticket: ${cantTotal} un. · S/ ${producto.precio.toFixed(2)} c/u). ¡Sigue escaneando más productos!`,
        exito: true,
      });

      if (!this.mostrarModalEscaner()) {
        this.mostrarToastHardware(producto.nombre, cantTotal, this.total());
      }
    } else {
      this.mensajeEscaneo.set({
        texto: `Código leído: "${codigo}", pero no coincide con ningún producto del catálogo.`,
        exito: false,
      });
    }

    this.cdr.markForCheck();
  }

  private activarFlashEscaneo(): void {
    this.flashEscaneo.set(true);
    setTimeout(() => {
      this.flashEscaneo.set(false);
      this.cdr.markForCheck();
    }, 380);
  }

  private agregarProductoAlCarrito(producto: Producto): void {
    const indiceExistente = this.lineasForm.controls.findIndex(
      (l) => l.controls.productoId.value === producto.id,
    );

    if (indiceExistente >= 0) {
      const linea = this.lineasForm.at(indiceExistente);
      const cantActual = linea.controls.cantidad.value || 0;
      linea.controls.cantidad.setValue(cantActual + 1);
      linea.controls.cantidad.markAsDirty();
    } else {
      const primeraLinea = this.lineasForm.at(0);
      if (
        this.lineasForm.length === 1 &&
        (!primeraLinea.controls.productoId.value || primeraLinea.controls.productoId.value === 0)
      ) {
        primeraLinea.patchValue({
          productoId: producto.id,
          busqueda: producto.nombre,
          cantidad: 1,
        });
        primeraLinea.markAsDirty();
      } else {
        const nuevaLinea = this.fb.nonNullable.group({
          productoId: [producto.id, [Validators.min(1)]],
          busqueda: [producto.nombre],
          cantidad: [1, [Validators.required, Validators.min(1)]],
        });
        this.lineasForm.push(nuevaLinea);
      }
    }
    this.versionDetalles.update((v) => v + 1);
    this.cdr.markForCheck();
  }

  private obtenerCantidadProducto(productoId: number): number {
    const linea = this.lineasForm.controls.find(
      (l) => l.controls.productoId.value === productoId,
    );
    return linea?.controls.cantidad.value || 1;
  }

  protected simularEscaneo(productoId: number): void {
    this.procesarCodigoEscaneado(productoId.toString(), true);
  }

  private emitirBeepPos(): void {
    try {
      const AudioCtx =
        window.AudioContext ||
        (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      if (!AudioCtx) return;
      const ctx = new AudioCtx();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(1760, ctx.currentTime);
      gain.gain.setValueAtTime(0.25, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + 0.12);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start();
      osc.stop(ctx.currentTime + 0.12);
    } catch {
      // Fallback silencioso
    }
  }

  private manejarErrorGuardado(err: HttpErrorResponse): void {
    const mensaje: string = err.error?.message ?? '';

    if (err.status === 404 && mensaje.startsWith('Producto no encontrado')) {
      this.error.set('Uno de los productos elegidos ya no existe. Revisa las líneas de la venta.');
    } else if (err.status === 409) {
      this.error.set(mensaje || 'No hay stock suficiente para completar la venta.');
    } else if (err.status === 400) {
      this.error.set(mensaje || 'Los datos enviados no son válidos. Revisa las líneas de la venta.');
    } else {
      this.error.set(mensaje || 'No se pudo registrar la venta.');
    }
    this.loading.set(false);
  }
}
