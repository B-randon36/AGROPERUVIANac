# Backup, recuperación y monitoreo

## Estado de las evidencias

La guía académica pide demostrar operación y recuperación Oracle. Esta entrega no ejecuta RMAN, Data Pump ni una restauración. La siguiente matriz organiza el trabajo pendiente y evita confundir un script con una recuperación comprobada.

| Evidencia | Procedimiento que debe ejecutarse | Resultado a conservar |
|---|---|---|
| Seguridad de base | Revisar cuentas, roles y grants efectivos | Consultas de privilegios y conexión restringida |
| Auditoría | Registrar y consultar una operación identificada | Actor, fecha, entidad, acción y cambio |
| Optimización | Comparar un plan antes y después del ajuste | SQL, volumen, plan, tiempo y explicación |
| Backup | Crear respaldo con la herramienta del alcance | Log de respaldo, identificador y destino |
| Recovery | Restaurar en una base de ensayo aislada | Log y comprobación de datos recuperados |
| Monitoreo | Observar sesión, consulta o alerta | Consulta/panel, momento y acción tomada |

## Ejercicio de continuidad propuesto

Registrar una venta identificable, respaldar la base de ensayo, introducir un cambio controlado y recuperar en una instancia aislada. Verificar cabecera, detalles, movimiento de inventario y referencia financiera. Documentar cuánto dato puede perderse (RPO) y cuánto tarda en recuperarse el servicio (RTO), con valores medidos.

El procedimiento concreto depende de la versión Oracle, el modo ARCHIVELOG, rutas y privilegios. Debe prepararlo el responsable de BD2; no se incluyen comandos destructivos genéricos para la base activa.

## Monitoreo de aplicación

`logback-spring.xml` configura consola y archivo con `traceId`. Es un patrón textual; no se describe como JSON estructurado. El Compose incluye Loki, Promtail y Grafana. Adjuntar evidencia de ingestión y correlación antes de afirmar observabilidad operativa completa.

Actuator requiere revisar exposición y autorización por ambiente. La presencia de un endpoint de salud no demuestra que todas las dependencias estén disponibles.

## Fuentes del proyecto

- [logback-spring.xml](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/resources/logback-spring.xml)
- [compose-dev.yml](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/compose-dev.yml)
- [application.yml](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/resources/application.yml)
