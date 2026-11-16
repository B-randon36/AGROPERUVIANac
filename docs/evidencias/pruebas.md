# Plan de pruebas y evidencias

## Qué se verificó en esta entrega

Se revisó el código base y se construyó la documentación. No se volvió a ejecutar aquí la aplicación ni su batería completa de pruebas. Los resultados de revisiones anteriores no se trasladan automáticamente a este commit.

## Casos prioritarios

| ID | Caso | Resultado esperado | Fuente/prueba existente relacionada |
|---|---|---|---|
| CP-01 | Venta válida | Cabecera, detalles, ticket y total coherentes | VentaServiceTest |
| CP-02 | Precio final S/ 118 | Base 100 e impuesto 18 | VentaServiceTest |
| CP-03 | Sin stock | Rechazo sin efectos parciales | VentaServiceTest e integración |
| CP-04 | Venta ajena | Rechazo por propiedad | VentaSecurityTest |
| CP-05 | Auditoría anónima o vendedor | 401/403 según autenticación | VentaSecurityTest |
| CP-06 | Ticket concurrente | Números únicos | VentaPersistenciaTest |
| CP-07 | Anulación | Estado conservado, stock y finanzas conciliados | VentaEndToEndTest |
| CP-08 | Anulación multi-lote | Cantidad devuelta a cada lote original | Añadir/confirmar cobertura de integración |
| CP-09 | Dos cajeros | Cobro en sesión autorizada correcta | Añadir/confirmar cobertura |
| CP-10 | Reintento con otro usuario o contenido | No filtrar datos ni duplicar efectos | Añadir cobertura |
| CP-11 | Venta sobre Oracle | Operación y rollback en base objetivo | Pendiente de ejecución Oracle |
| CP-12 | Dependencias de módulos | Verificación sin violaciones | ModularityTests |

## Ejecutar pruebas

Desde `backend` con Java 21:

```powershell
.\mvnw.cmd test
```

Desde `bomerp-frontend`:

```powershell
npm ci
npm test -- --watch=false
npm run build
```

Conservar `target/surefire-reports` y la salida del frontend. Si un fallo es de ambiente, registrarlo como tal; no cambiarlo a aprobado ni omitirlo del informe. La selección reducida de pruebas debe indicarse explícitamente.

## Ficha reproducible

Para cada evidencia registrar: fecha, commit, entorno y versión de base, rol, datos preparados, pasos, resultado esperado, resultado obtenido y ruta del archivo. Las capturas deben mostrar el contexto necesario sin revelar credenciales ni información real de clientes.

## Fuentes del proyecto

- [VentaServiceTest.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/test/java/pe/edu/upeu/bomerp/ventas/VentaServiceTest.java)
- [VentaSecurityTest.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/test/java/pe/edu/upeu/bomerp/ventas/VentaSecurityTest.java)
- [VentaEndToEndTest.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/test/java/pe/edu/upeu/bomerp/ventas/VentaEndToEndTest.java)
- [ModularityTests.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/test/java/pe/edu/upeu/bomerp/ModularityTests.java)
