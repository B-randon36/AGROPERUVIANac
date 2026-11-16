# Seguridad e identidad

## Autenticación

La SPA contiene flujo Authorization Code con PKCE y un callback. El backend actúa como Resource Server para validar JWT del issuer configurado. Los roles se normalizan desde `realm_access` y `resource_access`.

## Permisos de ventas observados

| Operación | Política en SecurityConfig |
|---|---|
| Crear y consultar ventas | ADMIN, VENDEDOR o SUPERVISOR |
| Consultar venta individual | Además se aplica propiedad para vendedor restringido |
| Resumen de ventas | Roles anteriores y filtro de vendedor desde autenticación |
| Auditoría y omnicanal | ADMIN o SUPERVISOR |
| Solicitar anulación | Roles de ventas; propiedad comprobada en servicio |
| Aprobar/rechazar anulación | ADMIN o SUPERVISOR en métodos HTTP previstos |
| Borrado de ventas | DELETE bloqueado |
| Rutas de delivery | Autenticación general en la configuración actual |

Las reglas se evalúan en orden. Los guards y menús de Angular son controles de navegación complementarios.

## Hallazgos para cerrar antes de uso real

- La configuración declara públicos Actuator, consola H2 y rutas de demostración; revisar su exposición por ambiente.
- El código de reintento por `operacionId` requiere revisar propiedad y coincidencia del contenido antes de devolver una venta.
- La selección de sesión financiera tiene mecanismos de respaldo; comprobar autorización de propiedad y eliminar selecciones ambiguas para varias cajas.
- Las credenciales y tokens que existan en configuraciones deben extraerse a la configuración segura del entorno. Esta web no los reproduce.

## Evidencias necesarias

Capturar casos 401 y 403, acceso permitido para gestor y rechazo de venta ajena. Registrar endpoint, rol, dato utilizado, respuesta y commit. Un token de prueba no debe quedar visible en una captura publicada.

## Fuentes del proyecto

- [SecurityConfig.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/config/SecurityConfig.java)
- [VentaController.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/ventas/controller/VentaController.java)
- [auth-service.ts](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/bomerp-frontend/src/app/core/auth/auth-service.ts)
- [FinanzasApiImpl.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/finanzas/api/FinanzasApiImpl.java)
