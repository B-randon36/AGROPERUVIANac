# Ambientes y preparación de despliegue

## Configuración observada

| Archivo | Característica | Interpretación |
|---|---|---|
| `application.yml` | Activa H2 e issuer local | Base de laboratorio |
| `application-h2.yml` | Memoria, semilla y create-drop | Demostración reiniciable |
| `application-dev.yml` | Conexión Oracle y ddl-auto update | Entorno de desarrollo que requiere esquemas y permisos |
| `application-prod.yml` | H2 en memoria y create-drop | El nombre prod no acredita persistencia productiva |
| `environment.prod.ts` | Direcciones localhost | Necesita revisar sustitución/configuración de Angular |

## Preparación necesaria para uso real

- Base persistente y migraciones versionadas; validar el esquema en lugar de reconstruirlo en arranque.
- Variables de entorno y gestión de secretos para cuentas y proveedores externos.
- API, issuer, callback y CORS coherentes con el dominio HTTPS final.
- Restricción de Actuator, consola H2 y rutas de demostración según ambiente.
- Copia de seguridad y recuperación ensayada.
- Evidencias de pruebas, volumen, rendimiento y restauración.

## GitHub Pages

GitHub Pages aloja esta documentación estática mediante el flujo automático `.github/workflows/deploy-docs.yml` al actualizar `docs/` o `mkdocs.yml` en `main` (compilación con `mkdocs build --strict` y despliegue a `gh-pages`). No ejecuta el backend Java ni aloja Oracle o Keycloak: publicar el portal documental no equivale a desplegar el ERP completo.

## Fuentes del proyecto

- [application-prod.yml](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/resources/application-prod.yml)
- [application-dev.yml](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/resources/application-dev.yml)
- [angular.json](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/bomerp-frontend/angular.json)
- [environment.prod.ts](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/bomerp-frontend/src/environments/environment.prod.ts)
