# Frontend Angular

## Estructura observada

`src/app/core` contiene autenticación, interceptores, guards y layout. `src/app/features` organiza catálogo, ventas, caja, inventario, producción, compras, terceros y finanzas. Las rutas usan `loadComponent`, lo que permite cargar pantallas por demanda.

El paquete declara Angular 22 y TypeScript 6. El README conserva una referencia a Angular 19: para esta edición se usa `package.json` como fuente de versión.

## Pantallas de ventas

| Archivo | Responsabilidad |
|---|---|
| `venta-form.ts` / `.html` | Carrito, cliente, pago, escaneo y ticket |
| `venta-list.ts` / `.html` | Consulta de ventas y detalle |
| `venta-reporte.ts` / `.html` | Filtros, paginación y anulaciones por rol |
| `venta.service.ts` | Peticiones a la API |
| `venta.model.ts` | Modelos TypeScript |

El frontend puede evitar errores con formularios y botones deshabilitados. La autorización y las invariantes deben aplicarse en el backend porque una petición puede enviarse sin la pantalla.

## Entornos y compilación

`environment.ts` usa la API local del puerto 8080. `environment.prod.ts` declara otra dirección local, pero su existencia no garantiza que Angular lo sustituya al compilar: revisar `fileReplacements` en `angular.json` o el mecanismo de configuración elegido. No se presenta `npm run build` como despliegue productivo completo.

```powershell
cd bomerp-frontend
npm ci
npm run build
npm test -- --watch=false
```

Guardar tamaño de bundles, salida de compilación y resultado de pruebas. Tener rutas diferidas demuestra una técnica aplicada; medir la mejora requiere una comparación.

## Fuentes del proyecto

- [package.json](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/bomerp-frontend/package.json)
- [angular.json](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/bomerp-frontend/angular.json)
- [app.routes.ts](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/bomerp-frontend/src/app/app.routes.ts)
- [venta-form.ts](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/bomerp-frontend/src/app/features/ventas/venta-form.ts)
