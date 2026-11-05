import {
  Component,
  OnInit,
  OnDestroy,
  AfterViewInit,
  inject,
  signal,
  computed,
  ViewChild,
  ElementRef,
} from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as L from 'leaflet';
import { DeliveryService } from './delivery-service';
import { PedidoDelivery, EstadoDelivery } from './delivery.model';

const COORD_PLANTA: [number, number] = [-12.0970, -77.0270]; // Planta AgroPeruvian - San Isidro

const DISTRITOS_LIMA: Record<string, [number, number]> = {
  'san isidro': [-12.0950, -77.0370],
  'miraflores': [-12.1190, -77.0300],
  'surco': [-12.1320, -76.9950],
  'santiago de surco': [-12.1320, -76.9950],
  'san borja': [-12.0930, -77.0010],
  'la molina': [-12.0830, -76.9450],
  'barranco': [-12.1480, -77.0210],
  'san miguel': [-12.0790, -77.0910],
  'magdalena': [-12.0910, -77.0700],
  'jesus maria': [-12.0750, -77.0470],
  'lince': [-12.0840, -77.0350],
  'pueblo libre': [-12.0740, -77.0650],
  'lima': [-12.0520, -77.0340],
  'cercado de lima': [-12.0520, -77.0340],
  'ate': [-12.0280, -76.9180],
  'callao': [-12.0560, -77.1180],
};

@Component({
  selector: 'app-delivery-list',
  standalone: true,
  imports: [CommonModule, FormsModule, CurrencyPipe],
  templateUrl: './delivery-list.html',
  styleUrls: ['./delivery-list.css'],
})
export class DeliveryList implements OnInit, AfterViewInit, OnDestroy {
  private readonly deliveryService = inject(DeliveryService);

  @ViewChild('mapaElemento') mapaElemento?: ElementRef<HTMLDivElement>;

  readonly pedidos = signal<PedidoDelivery[]>([]);
  readonly cargando = signal<boolean>(true);
  readonly error = signal<string | null>(null);
  readonly exito = signal<string | null>(null);

  // Selector de vista y filtros
  readonly vistaActual = signal<'tarjetas' | 'mapa'>('mapa');
  readonly tipoCapa = signal<'mapa' | 'satelite'>('mapa');
  readonly isFullscreen = signal<boolean>(false);
  readonly filtroMapa = signal<'TODOS' | 'PLANTA' | 'EN_RUTA' | 'ENTREGADO'>('TODOS');
  readonly filtroTabla = signal<'TODOS' | 'PENDIENTE' | 'EN_RUTA' | 'ENTREGADO'>('TODOS');
  readonly terminoBusqueda = signal<string>('');

  // Instancia Leaflet del Mapa Principal
  private map: L.Map | null = null;
  private layerMapa: L.TileLayer | null = null;
  private layerSatelite: L.TileLayer | null = null;
  private markersLayer: L.LayerGroup | null = null;
  private routesLayer: L.LayerGroup | null = null;

  // Mapa Picker Interactivo para Nuevo Delivery
  readonly geocodificando = signal<boolean>(false);
  private mapaPicker: L.Map | null = null;
  private markerPicker: L.Marker | null = null;
  coordSeleccionada: [number, number] | null = null;
  private readonly coordsPedidosNuevos = new Map<number, [number, number]>();

  // Modal para nuevo delivery
  mostrarModalNuevo = false;
  nuevoClienteNombre = '';
  nuevoClienteTelefono = '';
  nuevaDireccion = '';
  nuevaReferencia = '';
  nuevoCostoEnvio = 10;
  nuevoMontoTotal = 120;
  nuevoRepartidor = 'Carlos Mendoza (Moto 01)';
  nuevasObservaciones = '';

  // Pedido seleccionado para ver detalles en panel lateral
  pedidoDetalleMapa = signal<PedidoDelivery | null>(null);

  // Lista dinámica de repartidores: flota base + los ya usados en pedidos
  readonly repartidoresDisponibles = computed(() => {
    const listaBase = [
      'Carlos Mendoza (Moto 01)',
      'Pedro Castillo (Furgón 02)',
      'Manuel Ramos (Moto 03)',
      'Jorge Valdivia (Camión Refrigerado 04)',
      'Delivery Express Tercerizado',
    ];
    const repSet = new Set<string>(listaBase);
    for (const p of this.pedidos()) {
      if (p.repartidorNombre && p.repartidorNombre.trim()) {
        repSet.add(p.repartidorNombre.trim());
      }
    }
    return Array.from(repSet);
  });

  // Computados para KPIs y Dashboard Logístico TailAdmin
  readonly totalPedidosHoy = computed(() => this.pedidos().length);
  readonly enRutaCount = computed(() => this.pedidosPorEstado('EN_RUTA').length);
  readonly entregadosCount = computed(() => this.pedidosPorEstado('ENTREGADO').length);
  readonly pendientesCount = computed(() => this.pedidosPorEstado('PENDIENTE').length);

  readonly totalTarifasEnvio = computed(() =>
    this.pedidos().reduce((acc, p) => acc + (p.costoEnvio || 0), 0)
  );

  readonly totalMontoVentas = computed(() =>
    this.pedidos().reduce((acc, p) => acc + (p.montoTotal || 0), 0)
  );

  readonly motorizadosActivosCount = computed(() => {
    const repSet = new Set<string>();
    this.pedidos()
      .filter((p) => p.estadoDelivery === 'EN_RUTA' && p.repartidorNombre)
      .forEach((p) => repSet.add(p.repartidorNombre!));
    return repSet.size || (this.enRutaCount() > 0 ? 1 : 0);
  });

  readonly pedidoEnfocado = computed(() => {
    return this.pedidoDetalleMapa() || this.pedidos()[0] || null;
  });

  readonly pedidosFiltrados = computed(() => {
    let lista = this.pedidos();
    const filtro = this.filtroTabla();
    if (filtro !== 'TODOS') {
      lista = lista.filter((p) => p.estadoDelivery === filtro);
    }
    const q = this.terminoBusqueda().trim().toLowerCase();
    if (q) {
      lista = lista.filter(
        (p) =>
          p.codigoPedido.toLowerCase().includes(q) ||
          p.clienteNombre.toLowerCase().includes(q) ||
          p.direccionEntrega.toLowerCase().includes(q) ||
          (p.repartidorNombre && p.repartidorNombre.toLowerCase().includes(q))
      );
    }
    return lista;
  });

  ngOnInit(): void {
    this.cargarPedidos();
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.inicializarMapa(), 120);
  }

  ngOnDestroy(): void {
    this.destruirMapa();
    this.destruirMapaPicker();
  }

  cambiarVista(vista: 'tarjetas' | 'mapa'): void {
    this.vistaActual.set(vista);
    if (vista === 'mapa') {
      setTimeout(() => {
        if (!this.map) {
          this.inicializarMapa();
        } else {
          this.map.invalidateSize();
          this.actualizarMarcadores();
        }
      }, 100);
    }
  }

  cambiarCapa(capa: 'mapa' | 'satelite'): void {
    this.tipoCapa.set(capa);
    if (!this.map || !this.layerMapa || !this.layerSatelite) return;

    if (capa === 'satelite') {
      this.map.removeLayer(this.layerMapa);
      this.layerSatelite.addTo(this.map);
    } else {
      this.map.removeLayer(this.layerSatelite);
      this.layerMapa.addTo(this.map);
    }
  }

  toggleFullscreen(): void {
    const contenedor = document.getElementById('mapaContainer');
    if (!contenedor) return;

    if (!document.fullscreenElement) {
      contenedor.requestFullscreen().then(() => {
        this.isFullscreen.set(true);
        setTimeout(() => this.map?.invalidateSize(), 150);
      }).catch(() => {});
    } else {
      document.exitFullscreen().then(() => {
        this.isFullscreen.set(false);
        setTimeout(() => this.map?.invalidateSize(), 150);
      }).catch(() => {});
    }
  }

  zoomIn(): void {
    this.map?.zoomIn();
  }

  zoomOut(): void {
    this.map?.zoomOut();
  }

  seleccionarPedido(p: PedidoDelivery): void {
    this.pedidoDetalleMapa.set(p);
    if (this.map) {
      const idx = this.pedidos().findIndex((x) => x.id === p.id);
      const coord = this.obtenerCoordenada(p, idx >= 0 ? idx : 0);
      this.map.setView(coord, 14, { animate: true });
    }
  }

  filtrarPorMapa(filtro: 'TODOS' | 'PLANTA' | 'EN_RUTA' | 'ENTREGADO'): void {
    this.filtroMapa.set(filtro);
    this.actualizarMarcadores();
  }

  private inicializarMapa(): void {
    if (this.map) return;
    const elem = document.getElementById('mapaRutas');
    if (!elem) return;

    this.map = L.map('mapaRutas', {
      center: [-12.1050, -77.0250],
      zoom: 13,
      zoomControl: false,
      attributionControl: false,
    });

    this.layerMapa = L.tileLayer(
      'https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}',
      { maxZoom: 19 }
    );

    this.layerSatelite = L.tileLayer(
      'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
      { maxZoom: 19 }
    );

    this.layerMapa.addTo(this.map);
    this.markersLayer = L.layerGroup().addTo(this.map);
    this.routesLayer = L.layerGroup().addTo(this.map);

    this.actualizarMarcadores();
  }

  private destruirMapa(): void {
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
  }

  private actualizarMarcadores(): void {
    if (!this.map || !this.markersLayer || !this.routesLayer) return;

    this.markersLayer.clearLayers();
    this.routesLayer.clearLayers();

    const filtro = this.filtroMapa();

    // Marcador Planta Central
    if (filtro === 'TODOS' || filtro === 'PLANTA') {
      const iconoPlanta = L.divIcon({
        className: 'custom-leaflet-marker',
        html: `
          <div class="map-marker-pin pin-planta">
            <svg class="marker-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/>
            </svg>
          </div>
        `,
        iconSize: [38, 38],
        iconAnchor: [19, 19],
      });

      const markerPlanta = L.marker(COORD_PLANTA, { icon: iconoPlanta })
        .bindPopup(`
          <div class="map-popup-card">
            <div class="popup-badge badge-planta">ORIGEN PRINCIPAL</div>
            <h4 class="popup-title">Planta San Isidro</h4>
            <p class="popup-subtitle">Agro Peruvian Foods S.A.C.</p>
            <div class="popup-meta">
              <span>Av. Canaval y Moreyra 450, San Isidro</span>
            </div>
          </div>
        `);
      this.markersLayer.addLayer(markerPlanta);
    }

    // Marcadores Pedidos
    const lista = this.pedidos();

    lista.forEach((p, index) => {
      if (filtro !== 'TODOS') {
        if (filtro === 'EN_RUTA' && p.estadoDelivery !== 'EN_RUTA') return;
        if (filtro === 'ENTREGADO' && p.estadoDelivery !== 'ENTREGADO') return;
        if (filtro === 'PLANTA') return;
      }

      const coord = this.obtenerCoordenada(p, index);

      let claseColor = 'pin-registrado';
      let svgContenido = `
        <svg class="marker-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"/>
        </svg>
      `;

      if (p.estadoDelivery === 'EN_RUTA') {
        claseColor = 'pin-enruta';
        svgContenido = `
          <svg class="marker-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M13 16V6a1 1 0 00-1-1H4a1 1 0 00-1 1v10a1 1 0 001 1h1m8-1a1 1 0 01-1 1H9m4-1V8a1 1 0 011-1h2.586a1 1 0 01.707.293l3.414 3.414a1 1 0 01.293.707V16a1 1 0 01-1 1h-1m-6-1a2 2 0 104 0m-4 0a2 2 0 114 0m-8 0a2 2 0 104 0m-4 0a2 2 0 114 0"/>
          </svg>
        `;

        const polyline = L.polyline([COORD_PLANTA, coord], {
          color: '#0284C7',
          weight: 3.5,
          opacity: 0.85,
          dashArray: '6, 8',
        });
        this.routesLayer?.addLayer(polyline);
      } else if (p.estadoDelivery === 'ENTREGADO') {
        claseColor = 'pin-entregado';
        svgContenido = `
          <svg class="marker-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
            <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"/>
          </svg>
        `;
      }

      const iconoPedido = L.divIcon({
        className: 'custom-leaflet-marker',
        html: `<div class="map-marker-pin ${claseColor}">${svgContenido}</div>`,
        iconSize: [36, 36],
        iconAnchor: [18, 18],
      });

      const marker = L.marker(coord, { icon: iconoPedido });

      marker.on('click', () => {
        this.pedidoDetalleMapa.set(p);
      });

      marker.bindPopup(`
        <div class="map-popup-card">
          <div class="popup-header-row">
            <span class="popup-code">${p.codigoPedido}</span>
            <span class="popup-status-badge ${claseColor}">
              ${p.estadoDelivery === 'PENDIENTE' ? 'Registrado' : (p.estadoDelivery === 'EN_RUTA' ? 'En Ruta' : 'Entregado')}
            </span>
          </div>
          <h4 class="popup-title">${p.clienteNombre}</h4>
          <p class="popup-meta"><strong>Dirección:</strong> ${p.direccionEntrega}</p>
          <p class="popup-meta"><strong>Total:</strong> S/ ${(p.montoTotal || 0).toFixed(2)}</p>
          ${p.repartidorNombre ? `<p class="popup-meta"><strong>Repartidor:</strong> ${p.repartidorNombre}</p>` : ''}
        </div>
      `);

      this.markersLayer?.addLayer(marker);
    });
  }

  private obtenerCoordenada(p: PedidoDelivery, index: number): [number, number] {
    if (p.latitud != null && p.longitud != null) {
      return [p.latitud, p.longitud];
    }
    if (this.coordsPedidosNuevos.has(p.id)) {
      return this.coordsPedidosNuevos.get(p.id)!;
    }
    const dir = (p.direccionEntrega || '').toLowerCase();
    for (const [distrito, coords] of Object.entries(DISTRITOS_LIMA)) {
      if (dir.includes(distrito)) {
        return [coords[0] + (index * 0.0025), coords[1] + (index * 0.0025)];
      }
    }
    const offsets: [number, number][] = [
      [-12.1190, -77.0300],
      [-12.0950, -77.0370],
      [-12.1320, -76.9950],
      [-12.0930, -77.0010],
      [-12.0830, -76.9450],
    ];
    return offsets[index % offsets.length];
  }

  cargarPedidos(): void {
    this.cargando.set(true);
    this.deliveryService.listar().subscribe({
      next: (data) => {
        this.pedidos.set(data);
        this.cargando.set(false);
        this.actualizarMarcadores();
      },
      error: (err) => {
        this.error.set('Error al cargar pedidos de delivery: ' + (err.error?.message || err.message));
        this.cargando.set(false);
      },
    });
  }

  pedidosPorEstado(estado: EstadoDelivery): PedidoDelivery[] {
    return this.pedidos().filter((p) => p.estadoDelivery === estado);
  }

  marcarEnRuta(p: PedidoDelivery): void {
    this.deliveryService.actualizarEstado(p.id, {
      estado: 'EN_RUTA',
      observaciones: 'Repartidor salió a ruta de entrega',
    }).subscribe({
      next: () => {
        this.exito.set(`Pedido ${p.codigoPedido} ahora está EN RUTA.`);
        this.cargarPedidos();
        this.pedidoDetalleMapa.set(p);
        setTimeout(() => this.exito.set(null), 4000);
      },
      error: (err) => alert('Error: ' + err.message),
    });
  }

  marcarEntregado(p: PedidoDelivery): void {
    this.deliveryService.actualizarEstado(p.id, {
      estado: 'ENTREGADO',
      observaciones: 'Entregado en destino conforme con firma o foto',
    }).subscribe({
      next: () => {
        this.exito.set(`Pedido ${p.codigoPedido} marcado como ENTREGADO.`);
        this.cargarPedidos();
        this.pedidoDetalleMapa.set(p);
        setTimeout(() => this.exito.set(null), 4000);
      },
      error: (err) => alert('Error: ' + err.message),
    });
  }

  abrirModalNuevo(): void {
    this.mostrarModalNuevo = true;
    this.nuevoClienteNombre = '';
    this.nuevoClienteTelefono = '';
    this.nuevaDireccion = '';
    this.nuevaReferencia = '';
    this.nuevoCostoEnvio = 10;
    this.nuevoMontoTotal = 150;
    this.nuevoRepartidor = 'Carlos Mendoza (Moto 01)';
    this.nuevasObservaciones = '';
    this.coordSeleccionada = null;

    // Inicializar mini-mapa interactivo para fijar el punto de entrega
    setTimeout(() => this.inicializarMapaPicker(), 180);
  }

  cerrarModalNuevo(): void {
    this.destruirMapaPicker();
    this.mostrarModalNuevo = false;
  }

  /* =========================================================================
     MAPA INTERACTIVO PICKER (FIJAR PUNTO & DIRECCIÓN AUTOMÁTICA O MANUAL)
     ========================================================================= */
  private inicializarMapaPicker(): void {
    if (this.mapaPicker) {
      this.destruirMapaPicker();
    }
    const elem = document.getElementById('mapaPicker');
    if (!elem) return;

    this.mapaPicker = L.map('mapaPicker', {
      center: [-12.1150, -77.0300], // Miraflores / San Isidro centro
      zoom: 13,
      zoomControl: true,
      attributionControl: false,
    });

    L.tileLayer(
      'https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}',
      { maxZoom: 19 }
    ).addTo(this.mapaPicker);

    // Evento de clic en el mapa para marcar el punto y autocompletar la dirección
    this.mapaPicker.on('click', (e: L.LeafletMouseEvent) => {
      this.seleccionarPuntoEnMapa(e.latlng.lat, e.latlng.lng);
    });

    setTimeout(() => this.mapaPicker?.invalidateSize(), 120);
  }

  private destruirMapaPicker(): void {
    if (this.mapaPicker) {
      this.mapaPicker.remove();
      this.mapaPicker = null;
      this.markerPicker = null;
    }
  }

  async seleccionarPuntoEnMapa(lat: number, lng: number): Promise<void> {
    if (!this.mapaPicker) return;
    this.coordSeleccionada = [lat, lng];

    this.colocarPinPicker(lat, lng);
    this.mapaPicker.panTo([lat, lng]);

    this.geocodificando.set(true);
    try {
      const controller = new AbortController();
      const timer = setTimeout(() => controller.abort(), 3500);

      const url = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`;
      const res = await fetch(url, {
        signal: controller.signal,
        headers: { 'Accept-Language': 'es' },
      });
      clearTimeout(timer);

      if (res.ok) {
        const data = await res.json();
        const addr = data.address || {};
        const via = addr.road || addr.pedestrian || addr.neighbourhood || '';
        const numero = addr.house_number ? ` ${addr.house_number}` : '';
        const distrito = addr.city_district || addr.suburb || addr.city || addr.town || '';

        let direccionArmada = '';
        if (via) {
          direccionArmada = `${via}${numero}`;
          if (distrito && !direccionArmada.toLowerCase().includes(distrito.toLowerCase())) {
            direccionArmada += `, ${distrito}`;
          }
        } else if (data.display_name) {
          direccionArmada = data.display_name.split(',').slice(0, 3).join(',').trim();
        }

        if (direccionArmada) {
          this.nuevaDireccion = direccionArmada;
        } else {
          this.nuevaDireccion = this.obtenerDistritoMasCercano(lat, lng);
        }
      } else {
        this.nuevaDireccion = this.obtenerDistritoMasCercano(lat, lng);
      }
    } catch {
      this.nuevaDireccion = this.obtenerDistritoMasCercano(lat, lng);
    } finally {
      this.geocodificando.set(false);
    }
  }

  colocarPinPicker(lat: number, lng: number): void {
    if (!this.mapaPicker) return;

    if (this.markerPicker) {
      this.markerPicker.setLatLng([lat, lng]);
      return;
    }

    const pinIcon = L.divIcon({
      className: 'custom-leaflet-marker',
      html: `
        <div class="map-marker-pin pin-enruta" style="background:#0284c7; border-color:#ffffff; box-shadow: 0 0 0 5px rgba(2, 132, 199, 0.4);">
          <svg class="marker-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/>
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/>
          </svg>
        </div>
      `,
      iconSize: [36, 36],
      iconAnchor: [18, 18],
    });

    this.markerPicker = L.marker([lat, lng], { icon: pinIcon }).addTo(this.mapaPicker);
  }

  obtenerDistritoMasCercano(lat: number, lng: number): string {
    let minDistance = Infinity;
    let distritoElegido = 'Miraflores';

    for (const [distrito, [dLat, dLng]] of Object.entries(DISTRITOS_LIMA)) {
      const dist = Math.hypot(lat - dLat, lng - dLng);
      if (dist < minDistance) {
        minDistance = dist;
        distritoElegido = distrito.charAt(0).toUpperCase() + distrito.slice(1);
      }
    }
    return `Av. Principal, ${distritoElegido}, Lima`;
  }

  buscarDireccionEnMapa(): void {
    if (!this.nuevaDireccion.trim() || !this.mapaPicker) return;
    const texto = this.nuevaDireccion.toLowerCase();

    // 1. Revisar si contiene un distrito de Lima conocido
    for (const [distrito, coords] of Object.entries(DISTRITOS_LIMA)) {
      if (texto.includes(distrito)) {
        this.coordSeleccionada = coords;
        this.mapaPicker.setView(coords, 14, { animate: true });
        this.colocarPinPicker(coords[0], coords[1]);
        return;
      }
    }

    // 2. Geocodificación directa de dirección vía Nominatim
    fetch(
      `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(
        this.nuevaDireccion + ', Lima, Peru'
      )}&limit=1`,
      { headers: { 'Accept-Language': 'es' } }
    )
      .then((r) => r.json())
      .then((data) => {
        if (Array.isArray(data) && data.length > 0) {
          const lat = parseFloat(data[0].lat);
          const lon = parseFloat(data[0].lon);
          this.coordSeleccionada = [lat, lon];
          this.mapaPicker?.setView([lat, lon], 15, { animate: true });
          this.colocarPinPicker(lat, lon);
        }
      })
      .catch(() => {});
  }

  guardarNuevoDelivery(): void {
    if (!this.nuevoClienteNombre.trim() || !this.nuevaDireccion.trim()) {
      alert('Por favor complete el nombre del cliente y la dirección.');
      return;
    }

    const coordGuardada = this.coordSeleccionada;

    this.deliveryService.crearPedido({
      clienteNombre: this.nuevoClienteNombre,
      clienteTelefono: this.nuevoClienteTelefono,
      direccionEntrega: this.nuevaDireccion,
      referencia: this.nuevaReferencia,
      costoEnvio: +this.nuevoCostoEnvio,
      montoTotal: +this.nuevoMontoTotal,
      repartidorNombre: this.nuevoRepartidor,
      latitud: coordGuardada ? coordGuardada[0] : undefined,
      longitud: coordGuardada ? coordGuardada[1] : undefined,
      observaciones: this.nuevasObservaciones,
    }).subscribe({
      next: (nuevoPedido) => {
        if (coordGuardada && nuevoPedido?.id) {
          this.coordsPedidosNuevos.set(nuevoPedido.id, coordGuardada);
        }
        this.cerrarModalNuevo();
        this.cargarPedidos();
        this.exito.set('Pedido de delivery registrado correctamente.');
        setTimeout(() => this.exito.set(null), 4000);
      },
      error: (err) => alert('Error: ' + (err.error?.message || err.message)),
    });
  }
}
