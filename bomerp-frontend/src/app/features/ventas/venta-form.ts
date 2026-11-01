import { Component, OnInit, OnDestroy, computed, inject, signal, ViewChild, ElementRef } from '@angular/core';
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
  private readonly router = inject(Router);

  @ViewChild('videoScanner') videoScannerRef?: ElementRef<HTMLVideoElement>;
  private codeReader: BrowserMultiFormatReader | null = null;
  private scannerControls: IScannerControls | null = null;
  private ultimoCodigoTiempo = 0;
  private ultimoCodigoValor = '';

  protected readonly productos = signal<Producto[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);
  protected readonly buscandoDni = signal(false);

  // Estados para el escáner de código de barras por cámara
  protected readonly mostrarModalEscaner = signal(false);
  protected readonly camarasDisponibles = signal<MediaDeviceInfo[]>([]);
  protected readonly camaraSeleccionadaId = signal<string>('');
  protected readonly escaneando = signal(false);
  protected readonly errorCamara = signal<string | null>(null);
  protected readonly ultimoEscaneado = signal<string | null>(null);
  protected readonly mensajeEscaneo = signal<{ texto: string; exito: boolean } | null>(null);

  // Estados para la previsualización del ticket térmico
  protected readonly ventaGenerada = signal<VentaResponse | null>(null);
  protected readonly mostrarModalTicket = signal(false);
  protected readonly esBorrador = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    clienteNombre: ['Mercado Mayorista de Frutas Lima', [Validators.maxLength(100)]],
    clienteDocumento: [''],
    metodoPago: ['EFECTIVO' as MetodoPagoPos],
    diasCredito: [30, [Validators.min(1), Validators.max(180)]],
    detalles: this.fb.array([this.crearLinea()], [Validators.minLength(1)]),
  });

  protected get lineasForm() {
    return this.form.controls.detalles;
  }

  protected readonly valoresDetalles = toSignal(
    this.form.controls.detalles.valueChanges,
    { initialValue: this.form.controls.detalles.getRawValue() },
  );

  protected readonly subtotales = computed(() =>
    this.valoresDetalles().map((linea) => {
      const producto = this.productos().find((p) => p.id === linea.productoId);
      return (producto?.precio ?? 0) * (linea.cantidad ?? 0);
    }),
  );

  protected readonly total = computed(() =>
    this.subtotales().reduce((suma, s) => suma + s, 0),
  );

  protected readonly preciosUnitarios = computed(() =>
    this.valoresDetalles().map((linea) => {
      const producto = this.productos().find((p) => p.id === linea.productoId);
      return producto?.precio ?? 0;
    }),
  );

  protected readonly opcionesPorLinea = computed(() =>
    this.valoresDetalles().map((linea) => {
      const texto = (linea.busqueda ?? '').toLowerCase();
      return this.productos().filter((p) => p.nombre.toLowerCase().includes(texto));
    }),
  );

  ngOnInit(): void {
    this.cargarProductos();
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
  }

  protected quitarLinea(indice: number): void {
    if (this.lineasForm.length > 1) {
      this.lineasForm.removeAt(indice);
    }
  }

  protected seleccionarProducto(indice: number, evento: MatAutocompleteSelectedEvent): void {
    const producto: Producto = evento.option.value;
    this.lineasForm.at(indice).patchValue({
      productoId: producto.id,
      busqueda: producto.nombre,
    });
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
    const igv = Math.round(tot * 0.18 * 100) / 100;
    const sub = Math.round((tot - igv) * 100) / 100;

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
    const cantidadLineas = this.lineasForm.length;
    const esCred = this.form.controls.metodoPago.value === 'CREDITO';
    const notaIntegracion = esCred
      ? ` (Se generará Cuenta por Cobrar a ${this.form.controls.diasCredito.value} días + Despacho FEFO)`
      : ' (Impactará Sesión de Caja activa + Despacho FEFO)';
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
          this.ventaGenerada.set(ventaCreada);
          this.esBorrador.set(false);
          this.mostrarModalTicket.set(true);
          this.cargarProductos();
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
    this.form.reset({
      clienteNombre: 'Cliente General',
      clienteDocumento: '',
      metodoPago: 'EFECTIVO',
      diasCredito: 30,
    });
    this.lineasForm.clear();
    this.agregarLinea();
    this.error.set(null);
  }

  protected irAReporte(): void {
    this.mostrarModalTicket.set(false);
    this.router.navigate(['/ventas/reporte']);
  }

  ngOnDestroy(): void {
    this.detenerEscaner();
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
            this.procesarCodigoEscaneado(result.getText());
          }
        },
      );
      this.escaneando.set(true);
    } catch (err: unknown) {
      console.error('Error al iniciar stream de video:', err);
      this.errorCamara.set('No se pudo acceder a la transmisión de video de la cámara seleccionada.');
      this.escaneando.set(false);
    }
  }

  protected detenerEscaner(): void {
    if (this.scannerControls) {
      this.scannerControls.stop();
      this.scannerControls = null;
    }
    this.escaneando.set(false);
  }

  protected cerrarEscanerCamara(): void {
    this.detenerEscaner();
    this.mostrarModalEscaner.set(false);
    this.mensajeEscaneo.set(null);
  }

  protected procesarCodigoEscaneado(codigoRaw: string): void {
    const ahora = Date.now();
    const codigo = (codigoRaw || '').trim();
    if (!codigo) return;

    if (codigo === this.ultimoCodigoValor && ahora - this.ultimoCodigoTiempo < 1800) {
      return;
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
      this.agregarProductoAlCarrito(producto);
      this.mensajeEscaneo.set({
        texto: `¡Escaneado con éxito! "${producto.nombre}" agregado (+1 un. · S/ ${producto.precio.toFixed(2)})`,
        exito: true,
      });
    } else {
      this.mensajeEscaneo.set({
        texto: `Código leído: "${codigo}", pero no coincide con ningún producto del catálogo.`,
        exito: false,
      });
    }
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
      } else {
        const nuevaLinea = this.fb.nonNullable.group({
          productoId: [producto.id, [Validators.min(1)]],
          busqueda: [producto.nombre],
          cantidad: [1, [Validators.required, Validators.min(1)]],
        });
        this.lineasForm.push(nuevaLinea);
      }
    }
  }

  protected simularEscaneo(productoId: number): void {
    this.procesarCodigoEscaneado(productoId.toString());
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
