# Modelo de dominio

## Lenguaje del negocio

| Concepto | Significado |
|---|---|
| Producto | Artículo comercial con precio y stock de catálogo |
| Categoría | Clasificación del producto |
| Venta | Operación comercial con vendedor, pago, ticket y totales |
| Detalle de venta | Producto, cantidad y precio conservados en la operación |
| Lote | Existencias con fabricación, vencimiento y costo |
| Kardex | Historial de movimientos físicos de inventario |
| Sesión de caja | Turno con apertura, movimientos y cierre |
| Cuenta por cobrar | Obligación pendiente de un cliente por venta a crédito |
| BOM | Lista de materiales de un producto a fabricar |

## Agregado candidato: Venta

`Venta` posee su colección `DetalleVenta` con cascada y eliminación de huérfanos en JPA. Los detalles requieren una cabecera, cantidades válidas y precios tomados del catálogo. Esto justifica presentar Venta–DetalleVenta como agregado candidato: sus datos deben mantenerse coherentes dentro de la operación.

La propiedad `productoId` es una referencia por identificador; no convierte Producto ni Lote en entidades internas del agregado Venta. `sesionCajaId` también es un identificador, no una relación JPA a `SesionCaja`.

El servicio coordina efectos sobre otros módulos con `@Transactional`. Esa coordinación no acredita por sí sola una implementación completa de DDD: el código mantiene una organización por capas y varias reglas en servicios.

## Invariantes y responsabilidades

| Invariante | Responsable observado |
|---|---|
| Detalles no vacíos y cantidades positivas | DTO y validación de entrada |
| Precio aplicado desde el catálogo | Servicio de ventas |
| Existencias suficientes | Servicio de producto y transacción |
| Despacho y reversión por lote | Inventario / Kardex |
| Totales coherentes | Servicio de ventas con BigDecimal |
| Identidad del vendedor | Controlador y autenticación |
| Movimiento financiero | API de Finanzas |

## Límites entre módulos

Catálogo conserva artículos y categorías; Ventas conserva operaciones; Inventario conserva lotes y movimientos; Finanzas conserva caja y deudas. Compras y Producción tienen sus propias cabeceras y detalles. Deben revisarse los imports entre módulos antes de afirmar aislamiento de repositorios completo.

## Fuentes del proyecto

- [Venta.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/ventas/entity/Venta.java)
- [DetalleVenta.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/ventas/entity/DetalleVenta.java)
- [DetalleVentaRequest.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/ventas/dto/DetalleVentaRequest.java)
