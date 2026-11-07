import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';
import { rolGuard } from './core/guards/rol-guard';

export const routes: Routes = [
  {
    path: 'auth/callback',
    loadComponent: () => import('./core/auth/auth-callback').then((m) => m.AuthCallback),
    title: 'Autenticación · ERP Agro Peruvian Foods',
  },
  {
    path: '',
    loadComponent: () => import('./core/layout/layout').then((m) => m.Layout),
    children: [
      {
        path: '',
        loadComponent: () => import('./core/home/home').then((m) => m.Home),
        title: 'Dashboard · ERP Agro Peruvian Foods',
      },
      // Gestión Financiera y Caja Diaria
      {
        path: 'caja',
        canActivate: [authGuard, rolGuard('ADMIN', 'SUPERVISOR', 'CAJERO', 'VENDEDOR')],
        loadComponent: () => import('./features/caja/caja-dashboard').then((m) => m.CajaDashboard),
        title: 'Caja & Movimientos · ERP Agro Peruvian Foods',
      },
      // --- Gestión Financiera: Transacciones, Rendiciones, Sesiones y Créditos ---
      {
        path: 'finanzas/transacciones',
        canActivate: [authGuard, rolGuard('ADMIN', 'SUPERVISOR')],
        loadComponent: () =>
          import('./features/finanzas/transaccion-financiera/transaccion-financiera-list').then(
            (m) => m.TransaccionFinancieraList
          ),
        title: 'Transacciones Financieras · ERP Agro Peruvian Foods',
      },
      {
        path: 'finanzas/transacciones/nueva',
        canActivate: [authGuard, rolGuard('ADMIN', 'SUPERVISOR')],
        loadComponent: () =>
          import('./features/finanzas/transaccion-financiera/transaccion-financiera-form').then(
            (m) => m.TransaccionFinancieraForm
          ),
        title: 'Nueva Transacción Financiera · ERP Agro Peruvian Foods',
      },
      {
        path: 'finanzas/transacciones/:id/editar',
        canActivate: [authGuard, rolGuard('ADMIN', 'SUPERVISOR')],
        loadComponent: () =>
          import('./features/finanzas/transaccion-financiera/transaccion-financiera-form').then(
            (m) => m.TransaccionFinancieraForm
          ),
        title: 'Editar Transacción · ERP Agro Peruvian Foods',
      },
      {
        path: 'finanzas/categorias',
        canActivate: [authGuard, rolGuard('ADMIN', 'SUPERVISOR')],
        loadComponent: () =>
          import('./features/finanzas/categoria-financiera/categoria-financiera-list').then(
            (m) => m.CategoriaFinancieraList
          ),
        title: 'Categorías Financieras · ERP Agro Peruvian Foods',
      },
      {
        path: 'finanzas/categorias/nueva',
        canActivate: [authGuard, rolGuard('ADMIN', 'SUPERVISOR')],
        loadComponent: () =>
          import('./features/finanzas/categoria-financiera/categoria-financiera-form').then(
            (m) => m.CategoriaFinancieraForm
          ),
        title: 'Nueva Categoría Financiera · ERP Agro Peruvian Foods',
      },
      {
        path: 'finanzas/categorias/:id/editar',
        canActivate: [authGuard, rolGuard('ADMIN', 'SUPERVISOR')],
        loadComponent: () =>
          import('./features/finanzas/categoria-financiera/categoria-financiera-form').then(
            (m) => m.CategoriaFinancieraForm
          ),
        title: 'Editar Categoría Financiera · ERP Agro Peruvian Foods',
      },
      {
        path: 'finanzas/rendiciones',
        canActivate: [authGuard, rolGuard('ADMIN', 'SUPERVISOR')],
        loadComponent: () =>
          import('./features/finanzas/rendicion-caja-chica/rendicion-caja-chica-list').then(
            (m) => m.RendicionCajaChicaList
          ),
        title: 'Rendiciones de Caja Chica · ERP Agro Peruvian Foods',
      },
      {
        path: 'finanzas/rendiciones/nueva',
        canActivate: [authGuard, rolGuard('ADMIN', 'SUPERVISOR')],
        loadComponent: () =>
          import('./features/finanzas/rendicion-caja-chica/rendicion-caja-chica-form').then(
            (m) => m.RendicionCajaChicaForm
          ),
        title: 'Nueva Rendición de Caja Chica · ERP Agro Peruvian Foods',
      },
      {
        path: 'finanzas/sesiones',
        canActivate: [authGuard, rolGuard('ADMIN', 'SUPERVISOR', 'CAJERO', 'VENDEDOR')],
        loadComponent: () =>
          import('./features/finanzas/sesion-caja/sesion-caja-list').then((m) => m.SesionCajaList),
        title: 'Sesiones de Caja Diaria · ERP Agro Peruvian Foods',
      },
      {
        path: 'finanzas/sesiones/nueva',
        canActivate: [authGuard, rolGuard('ADMIN', 'SUPERVISOR', 'CAJERO', 'VENDEDOR')],
        loadComponent: () =>
          import('./features/finanzas/sesion-caja/sesion-caja-form').then((m) => m.SesionCajaForm),
        title: 'Apertura de Caja · ERP Agro Peruvian Foods',
      },
      {
        path: 'finanzas/sesiones/:id',
        canActivate: [authGuard, rolGuard('ADMIN', 'SUPERVISOR', 'CAJERO', 'VENDEDOR')],
        loadComponent: () =>
          import('./features/finanzas/sesion-caja/sesion-caja-form').then((m) => m.SesionCajaForm),
        title: 'Detalle de Sesión de Caja · ERP Agro Peruvian Foods',
      },
      {
        path: 'finanzas/creditos',
        canActivate: [authGuard, rolGuard('ADMIN', 'SUPERVISOR')],
        loadComponent: () =>
          import('./features/finanzas/cuenta-cobrar/cuenta-cobrar-list').then(
            (m) => m.CuentaCobrarList
          ),
        title: 'Cuentas por Cobrar · ERP Agro Peruvian Foods',
      },
      {
        path: 'finanzas/creditos/nuevo',
        canActivate: [authGuard, rolGuard('ADMIN', 'SUPERVISOR')],
        loadComponent: () =>
          import('./features/finanzas/cuenta-cobrar/cuenta-cobrar-form').then(
            (m) => m.CuentaCobrarForm
          ),
        title: 'Nuevo Crédito · ERP Agro Peruvian Foods',
      },
      {
        path: 'finanzas/reportes',
        canActivate: [authGuard, rolGuard('ADMIN', 'SUPERVISOR')],
        loadComponent: () =>
          import('./features/finanzas/reporte-financiero/reporte-financiero-list').then(
            (m) => m.ReporteFinancieroList
          ),
        title: 'Reportes y Dashboard Financiero · ERP Agro Peruvian Foods',
      },
      // --- Inventario: Lotes, FEFO, Kardex, Almacenes y Stock Crítico ---
      {
        path: 'inventario/lotes',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/inventario/lote-list').then((m) => m.LoteList),
        title: 'Lotes y Control FEFO · ERP Agro Peruvian Foods',
      },
      {
        path: 'inventario/kardex',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/inventario/kardex-list').then((m) => m.KardexList),
        title: 'Kardex de Inventario · ERP Agro Peruvian Foods',
      },
      {
        path: 'inventario/almacenes',
        canActivate: [authGuard, rolGuard('SUPERVISOR', 'ADMIN')],
        loadComponent: () =>
          import('./features/inventario/almacen-list').then((m) => m.AlmacenList),
        title: 'Almacenes y Ocupación · ERP Agro Peruvian Foods',
      },
      {
        path: 'inventario/stock-critico',
        canActivate: [authGuard, rolGuard('SUPERVISOR', 'ADMIN')],
        loadComponent: () =>
          import('./features/inventario/stock-critico').then((m) => m.StockCriticoComponent),
        title: 'Stock Crítico & Semáforo · ERP Agro Peruvian Foods',
      },
      // --- Delivery y Omnicanal ---
      {
        path: 'ventas/delivery',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/ventas/delivery/delivery-list').then((m) => m.DeliveryList),
        title: 'Despachos y Pedidos Delivery · ERP Agro Peruvian Foods',
      },
      // Ventas y Punto de Venta (POS)
      {
        path: 'ventas',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/ventas/venta-reporte').then((m) => m.VentaReporteComponent),
        title: 'Reporte de Ventas POS  · ERP Agro Peruvian Foods',
      },
      {
        path: 'ventas/reporte',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/ventas/venta-reporte').then((m) => m.VentaReporteComponent),
        title: 'Reporte de Ventas POS  · ERP Agro Peruvian Foods',
      },
      {
        path: 'ventas/nueva',
        canActivate: [authGuard, rolGuard('ADMIN', 'VENDEDOR', 'SUPERVISOR')],
        loadComponent: () => import('./features/ventas/venta-form').then((m) => m.VentaForm),
        title: 'Nueva Venta POS  · ERP Agro Peruvian Foods',
      },
      // Producción & BOM + Catálogo
      {
        path: 'produccion',
        canActivate: [authGuard, rolGuard('SUPERVISOR', 'ADMIN')],
        loadComponent: () => import('./features/produccion/orden-list').then((m) => m.OrdenList),
        title: 'Órdenes de Producción BOM · ERP Agro Peruvian Foods',
      },
      {
        path: 'produccion/nueva',
        canActivate: [authGuard, rolGuard('SUPERVISOR', 'ADMIN')],
        loadComponent: () => import('./features/produccion/orden-form').then((m) => m.OrdenForm),
        title: 'Nueva Orden de Trabajo · ERP Agro Peruvian Foods',
      },
      {
        path: 'produccion/recetas',
        canActivate: [authGuard, rolGuard('SUPERVISOR', 'ADMIN')],
        loadComponent: () =>
          import('./features/produccion/lista-materiales-list').then((m) => m.ListaMaterialesList),
        title: 'Recetas Maestras BOM · ERP Agro Peruvian Foods',
      },
      {
        path: 'produccion/centros-trabajo',
        canActivate: [authGuard, rolGuard('SUPERVISOR', 'ADMIN')],
        loadComponent: () =>
          import('./features/produccion/centro-trabajo-list').then((m) => m.CentroTrabajoList),
        title: 'Centros de Trabajo de Planta · ERP Agro Peruvian Foods',
      },
      // Catálogo: Categorías
      {
        path: 'catalogo/categorias',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/catalogo/categoria/categoria-list').then((m) => m.CategoriaList),
        title: 'Categorías  · ERP Agro Peruvian Foods',
      },
      {
        path: 'catalogo/categorias/nueva',
        canActivate: [authGuard, rolGuard('ADMIN')],
        loadComponent: () =>
          import('./features/catalogo/categoria/categoria-form').then((m) => m.CategoriaForm),
        title: 'Nueva Categoría · ERP Agro Peruvian Foods',
      },
      {
        path: 'catalogo/categorias/:id',
        canActivate: [authGuard, rolGuard('ADMIN')],
        loadComponent: () =>
          import('./features/catalogo/categoria/categoria-form').then((m) => m.CategoriaForm),
        title: 'Editar Categoría · ERP Agro Peruvian Foods',
      },
      // Catálogo: Productos
      {
        path: 'catalogo/productos',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/catalogo/producto/producto-list').then((m) => m.ProductoList),
        title: 'Catálogo de Productos  · ERP Agro Peruvian Foods',
      },
      {
        path: 'catalogo/productos/nuevo',
        canActivate: [authGuard, rolGuard('ADMIN')],
        loadComponent: () =>
          import('./features/catalogo/producto/producto-form').then((m) => m.ProductoForm),
        title: 'Nuevo Producto  · ERP Agro Peruvian Foods',
      },
      {
        path: 'catalogo/productos/:id/editar',
        canActivate: [authGuard, rolGuard('ADMIN')],
        loadComponent: () =>
          import('./features/catalogo/producto/producto-form').then((m) => m.ProductoForm),
        title: 'Editar Producto  · ERP Agro Peruvian Foods',
      },
      {
        path: 'catalogo/productos/:id',
        canActivate: [authGuard, rolGuard('ADMIN')],
        loadComponent: () =>
          import('./features/catalogo/producto/producto-form').then((m) => m.ProductoForm),
        title: 'Ver/Editar Producto  · ERP Agro Peruvian Foods',
      },
      // Compras y Abastecimiento
      {
        path: 'compras',
        canActivate: [authGuard, rolGuard('SUPERVISOR', 'ADMIN')],
        loadComponent: () => import('./features/compras/compra-list').then((m) => m.CompraList),
        title: 'Compras de Insumos · ERP Agro Peruvian Foods',
      },
      {
        path: 'compras/nueva',
        canActivate: [authGuard, rolGuard('SUPERVISOR', 'ADMIN')],
        loadComponent: () => import('./features/compras/compra-form').then((m) => m.CompraForm),
        title: 'Nueva Orden de Compra · ERP Agro Peruvian Foods',
      },
      {
        path: 'compras/proveedores',
        canActivate: [authGuard, rolGuard('SUPERVISOR', 'ADMIN')],
        loadComponent: () =>
          import('./features/compras/proveedor-list').then((m) => m.ProveedorList),
        title: 'Directorio de Proveedores · ERP Agro Peruvian Foods',
      },
      {
        path: 'compras/proveedores/nuevo',
        canActivate: [authGuard, rolGuard('ADMIN')],
        loadComponent: () =>
          import('./features/compras/proveedor-form').then((m) => m.ProveedorForm),
        title: 'Registrar Proveedor · ERP Agro Peruvian Foods',
      },
      // Directorio Comercial: Clientes y Proveedores (Terceros)
      {
        path: 'terceros',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/terceros/terceros-list').then((m) => m.TercerosList),
        title: 'Directorio de Clientes y Proveedores · ERP Agro Peruvian Foods',
      },
      {
        path: 'terceros/nuevo',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/terceros/terceros-form').then((m) => m.TercerosForm),
        title: 'Registrar Cliente o Proveedor · ERP Agro Peruvian Foods',
      },
      {
        path: 'terceros/:id/editar',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/terceros/terceros-form').then((m) => m.TercerosForm),
        title: 'Editar Tercero Comercial · ERP Agro Peruvian Foods',
      },
      {
        path: 'clientes',
        redirectTo: 'terceros',
        pathMatch: 'full',
      },
      {
        path: 'proveedores',
        redirectTo: 'terceros',
        pathMatch: 'full',
      },
      {
        path: 'sin-permiso',
        loadComponent: () => import('./core/sin-permiso/sin-permiso').then((m) => m.SinPermiso),
        title: 'Acceso Denegado (403) · ERP Agro Peruvian Foods',
      },
    ],
  },
  {
    path: '**',
    redirectTo: '',
  },
];
