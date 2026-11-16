# Datos y persistencia

## Cabecera y detalle de venta

`Venta` se mapea a `BOM_VENTAS.VENTAS`; `DetalleVenta` a `BOM_VENTAS.DETALLE_VENTAS` mediante `ID_VENTA`. La cabecera guarda ticket, fecha, estado, método, importes, vendedor, sesión y operación. El detalle conserva producto, lote referenciado, nombre, precio, cantidad y subtotal.

`numeroTicket` y `operacionId` tienen unicidad declarada en la entidad. `operacionId` es opcional: la restricción no implica que todos los clientes lo envíen ni resuelve por sí sola las carreras y la autorización de reintentos.

## Inventario de tablas declarado por JPA

La tabla siguiente se extrajo de las anotaciones del código. **Sin esquema explícito** significa que JPA depende de la conexión/configuración; no se debe asignar automáticamente BOM_CAJA a esas entidades. Esto no acredita que las tablas estén instaladas en Oracle.

| Entidad | Esquema en la anotación | Tabla | Fuente |
|---|---|---|---|
| `Categoria` | `BOM_CATALOGO` | `CATEGORIAS` | [Categoria.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/catalogo/categoria/entity/Categoria.java) |
| `Producto` | `BOM_CATALOGO` | `PRODUCTOS` | [Producto.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/catalogo/producto/entity/Producto.java) |
| `Compra` | `BOM_COMPRAS` | `COMPRAS` | [Compra.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/compras/entity/Compra.java) |
| `DetalleCompra` | `BOM_COMPRAS` | `DETALLE_COMPRAS` | [DetalleCompra.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/compras/entity/DetalleCompra.java) |
| `Insumo` | `BOM_COMPRAS` | `INSUMOS` | [Insumo.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/compras/entity/Insumo.java) |
| `PagoCompra` | `BOM_COMPRAS` | `PAGOS_COMPRAS` | [PagoCompra.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/compras/entity/PagoCompra.java) |
| `Proveedor` | `BOM_COMPRAS` | `PROVEEDORES` | [Proveedor.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/compras/entity/Proveedor.java) |
| `AbonoCuentaPorCobrar` | `Sin esquema explícito` | `ABONOS_CUENTA_POR_COBRAR` | [AbonoCuentaPorCobrar.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/finanzas/entity/AbonoCuentaPorCobrar.java) |
| `AsignacionCajaCentroCosto` | `Sin esquema explícito` | `ASIGNACIONES_CAJA_CENTRO_COSTO` | [AsignacionCajaCentroCosto.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/finanzas/entity/AsignacionCajaCentroCosto.java) |
| `CajaChica` | `Sin esquema explícito` | `CAJAS_CHICAS` | [CajaChica.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/finanzas/entity/CajaChica.java) |
| `CategoriaFinanciera` | `Sin esquema explícito` | `CATEGORIAS_FINANCIERAS` | [CategoriaFinanciera.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/finanzas/entity/CategoriaFinanciera.java) |
| `CentroCosto` | `Sin esquema explícito` | `CENTROS_COSTO` | [CentroCosto.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/finanzas/entity/CentroCosto.java) |
| `CuentaPorCobrar` | `Sin esquema explícito` | `CUENTAS_POR_COBRAR` | [CuentaPorCobrar.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/finanzas/entity/CuentaPorCobrar.java) |
| `DenominacionMoneda` | `Sin esquema explícito` | `DENOMINACIONES_MONEDA` | [DenominacionMoneda.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/finanzas/entity/DenominacionMoneda.java) |
| `DetalleArqueoEfectivo` | `Sin esquema explícito` | `DETALLES_ARQUEO_EFECTIVO` | [DetalleArqueoEfectivo.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/finanzas/entity/DetalleArqueoEfectivo.java) |
| `DetalleRendicionCajaChica` | `Sin esquema explícito` | `DETALLES_RENDICION_CAJA_CHICA` | [DetalleRendicionCajaChica.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/finanzas/entity/DetalleRendicionCajaChica.java) |
| `MovimientoCaja` | `Sin esquema explícito` | `MOVIMIENTOS_CAJA` | [MovimientoCaja.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/finanzas/entity/MovimientoCaja.java) |
| `RendicionCajaChica` | `Sin esquema explícito` | `RENDICIONES_CAJA_CHICA` | [RendicionCajaChica.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/finanzas/entity/RendicionCajaChica.java) |
| `SesionCaja` | `Sin esquema explícito` | `SESIONES_CAJA` | [SesionCaja.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/finanzas/entity/SesionCaja.java) |
| `TransaccionFinanciera` | `Sin esquema explícito` | `TRANSACCIONES_FINANCIERAS` | [TransaccionFinanciera.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/finanzas/entity/TransaccionFinanciera.java) |
| `Almacen` | `Sin esquema explícito` | `TBL_ALMACENES` | [Almacen.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/inventario/entity/Almacen.java) |
| `Lote` | `Sin esquema explícito` | `TBL_LOTES` | [Lote.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/inventario/entity/Lote.java) |
| `MovimientoKardex` | `Sin esquema explícito` | `TBL_MOVIMIENTOS_KARDEX` | [MovimientoKardex.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/inventario/entity/MovimientoKardex.java) |
| `CentroTrabajo` | `BOM_PRODUCCION` | `CENTROS_TRABAJO` | [CentroTrabajo.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/produccion/entity/CentroTrabajo.java) |
| `DetalleConsumoInsumo` | `BOM_PRODUCCION` | `DETALLES_CONSUMO_INSUMO` | [DetalleConsumoInsumo.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/produccion/entity/DetalleConsumoInsumo.java) |
| `DetalleListaMateriales` | `BOM_PRODUCCION` | `DETALLES_LISTA_MATERIALES` | [DetalleListaMateriales.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/produccion/entity/DetalleListaMateriales.java) |
| `ListaMateriales` | `BOM_PRODUCCION` | `LISTAS_MATERIALES` | [ListaMateriales.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/produccion/entity/ListaMateriales.java) |
| `LoteProducto` | `BOM_PRODUCCION` | `LOTES_PRODUCTO` | [LoteProducto.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/produccion/entity/LoteProducto.java) |
| `OrdenProduccion` | `BOM_PRODUCCION` | `ORDENES_PRODUCCION` | [OrdenProduccion.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/produccion/entity/OrdenProduccion.java) |
| `Rol` | `Sin esquema explícito` | `roles` | [Rol.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/seguridad/usuario/entity/Rol.java) |
| `Usuario` | `Sin esquema explícito` | `usuarios` | [Usuario.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/seguridad/usuario/entity/Usuario.java) |
| `Tercero` | `BOM_CRM` | `TERCEROS` | [Tercero.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/terceros/entity/Tercero.java) |
| `DetalleVenta` | `BOM_VENTAS` | `DETALLE_VENTAS` | [DetalleVenta.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/ventas/entity/DetalleVenta.java) |
| `PedidoDelivery` | `BOM_VENTAS` | `PEDIDOS_DELIVERY` | [PedidoDelivery.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/ventas/entity/PedidoDelivery.java) |
| `RegistroAuditoria` | `BOM_VENTAS` | `AUDITORIA` | [RegistroAuditoria.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/ventas/entity/RegistroAuditoria.java) |
| `Venta` | `BOM_VENTAS` | `VENTAS` | [Venta.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/ventas/entity/Venta.java) |

## Consistencia de esquema

Comparar este inventario con el DDL instalado y revisar longitudes, columnas nuevas, versiones optimistas y estados admitidos. El alcance empresarial exige conservar las relaciones y privilegios en la base objetivo, además de las anotaciones Java.
