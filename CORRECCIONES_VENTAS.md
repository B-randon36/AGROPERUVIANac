# Correcciones de ventas — AGROPERUVIANac

Base revisada: rama `ventas_avance`, commit `20305af4e89f4bb93a50bd4cbedd0756f91564d7`.
Rama de trabajo local: `fix/ventas-integridad-y-reportes`.

## Cambios implementados

1. **Conservación del historial.** Se retiraron los endpoints, métodos y botones de eliminación definitiva de ventas. Spring Security rechaza DELETE incluso para administrador. Se conserva el flujo de solicitud, aprobación/rechazo y anulación.
2. **Auditoría protegida.** Auditoría y reporte omnicanal requieren ADMIN o SUPERVISOR. Ya no existe acceso anónimo a auditoría.
3. **Datos por vendedor.** El resumen, sus totales y sus filas reciben el identificador autenticado. Se reutilizan las autoridades de Spring Security, tanto para roles de reino como de cliente. Un vendedor no puede solicitar la anulación de una venta ajena. El identificador suministrado por el navegador no determina el acceso.
4. **Desglose de precios.** Se asume que los precios del catálogo incluyen el 18 % que ya utiliza el proyecto: base = total / 1.18; impuesto = total - base. Se aplica al servidor y al borrador del ticket. El total cobrado se conserva. Ejemplo: S/ 118 = S/ 100 + S/ 18.
5. **Reportes con datos almacenados.** Se retiraron montos, transacciones, rankings y porcentajes inventados tanto del backend como de las respuestas de respaldo del frontend. Las respuestas fallidas muestran un error. Una base sin operaciones devuelve cero. Una venta vinculada a delivery se cuenta una sola vez. Los deliveries independientes se reconocen cuando están ENTREGADOS, conforme al registro financiero existente. El ranking agrupa cantidades e ingresos de detalles reales de ventas no anuladas. Se omiten costos, utilidades y márgenes mientras no exista costo histórico respaldado. No se incluye un canal feria ficticio.
6. **Tickets únicos.** Se utiliza la secuencia `BOM_VENTAS.SEQ_TICKET_VENTA`. Hibernate selecciona la sintaxis de H2 u Oracle. El formato es `TKT-S-0000000001`; el prefijo distingue los tickets nuevos de los históricos. La secuencia admite concurrencia y no reutiliza valores tras rollback. La entidad conserva la restricción UNIQUE.

## Aplicación del paquete en Windows

El ZIP contiene los archivos modificados/nuevos en sus rutas originales y `cambios.patch`. No es un repositorio completo: aplica el parche sobre tu clon existente. No copies el ZIP encima de archivos con cambios pendientes sin revisarlos.

1. Abre una terminal en la raíz de tu repositorio, donde están `backend` y `bomerp-frontend`.
2. Revisa `git status` y conserva tus cambios pendientes antes de continuar.
3. Crea una rama y aplica el parche. Sustituye la ruta de ejemplo por donde descomprimiste el ZIP:

```powershell
git switch ventas_avance
git switch -c correcciones-ventas
git apply --check "C:\ruta\AGROPERUVIAN_ventas_corregidas\cambios.patch"
git apply "C:\ruta\AGROPERUVIAN_ventas_corregidas\cambios.patch"
git diff --stat
```

Si `git apply --check` informa conflictos, detente y revisa los archivos afectados: tu copia difiere de la base revisada. No es necesario resetear ni borrar tu trabajo.

## Preparación de la base de datos

### H2

La secuencia se crea en `backend/src/main/resources/data.sql` durante la inicialización habitual del proyecto. La prueba de extremo a extremo verificó el arranque y la generación de tickets con ese mecanismo.

### Oracle / SQL Developer

Antes de arrancar la versión corregida, ejecuta con F5:

`backend/src/main/resources/db/oracle_ventas_ticket_sequence.sql`

Usa el propietario BOM_VENTAS o una cuenta autorizada a crear la secuencia, modificar la tabla y otorgar el permiso SELECT a BOMERP_APP. El script conserva una secuencia existente, crea la restricción única si falta y muestra su estado. Si existen tickets duplicados históricos, revisa esos datos antes de crear la restricción; el script no elimina ventas. No reinicies la secuencia en una base con tickets TKT-S registrados.

El script de Oracle está incluido, pero no se ejecutó contra una instancia Oracle en esta revisión.

## Comprobaciones realizadas

- Compilación de producción Angular: correcta; queda la advertencia existente sobre Leaflet/CommonJS.
- Pruebas frontend: **9 aprobadas**, en 4 archivos.
- Pruebas backend seleccionadas: **35 aprobadas**, sin fallos ni errores:
  - VentaServiceTest: 18.
  - VentaSecurityTest: 12.
  - VentaPersistenciaTest: 4.
  - VentaEndToEndTest: 1.
- Se verificaron accesos 401/403, permisos de gestor, roles provenientes de realm_access/resource_access, filtro por vendedor, solicitudes de anulación ajenas, cálculo de IGV, reporte vacío, no duplicación de delivery, restricciones UNIQUE y 100 números concurrentes sin duplicados.
- El flujo HTTP con base H2 verificó creación, reducción de stock, transacción financiera PAGADO, anulación, restauración del stock del catálogo y transacción ANULADO.
- Se compilaron las fuentes del backend y sus pruebas. No se ejecutó la batería completa de módulos ajenos a estos cambios.

Para repetir las pruebas con Java 21:

```powershell
cd backend
.\mvnw.cmd "-Dtest=VentaServiceTest,VentaSecurityTest,VentaPersistenciaTest,VentaEndToEndTest" test
cd ..\bomerp-frontend
npm ci
npm run build
npm test -- --watch=false
```

En el entorno de revisión se cargó explícitamente el agente de Mockito mediante `-DargLine=-javaagent:<ruta-al-jar-de-mockito-core>` porque ese entorno no permite su conexión dinámica a la JVM. No fue necesario modificar el POM ni las versiones del proyecto.

Para iniciar el proyecto, utiliza sus comandos habituales: `mvnw.cmd spring-boot:run` en backend y `npm start` en bomerp-frontend.

## Alcance

Los cambios se prepararon localmente y no se subieron a GitHub. No recalculan ventas históricas ni eliminan datos de demostración ya almacenados. Los reportes reflejan los registros existentes en la base, incluidos los datos semilla si se usan. El cálculo implementado mantiene la hipótesis del proyecto de precios finales gravados al 18 %; no introduce tratamiento de productos exonerados ni emisión tributaria electrónica.
