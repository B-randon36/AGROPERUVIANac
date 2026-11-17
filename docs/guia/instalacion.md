# Instalación y ejecución en VS Code

## Requisitos

| Herramienta | Uso |
|---|---|
| Git | Obtener el código |
| JDK 21 | Compilar y ejecutar el backend |
| Node.js y npm compatibles con Angular 22 | Ejecutar la SPA; revisar `engines` del CLI instalado |
| Docker con Compose | Keycloak; Oracle si se usará ese entorno |
| Python 3.10 o superior | Construir esta documentación |

Para comprobar Java utiliza `java -version`. El repositorio incorpora Maven Wrapper; no es obligatorio instalar Maven por separado. Las extensiones Java y Spring de VS Code facilitan la navegación, pero los comandos no dependen del editor.

## 1. Obtener la rama

```powershell
git clone https://github.com/B-randon36/AGROPERUVIANac.git
cd AGROPERUVIANac
code .
```

## 2. Iniciar la identidad de laboratorio

En una terminal de VS Code, desde la raíz:

```powershell
cd backend
docker compose -f compose-dev.yml up -d keycloak-db keycloak
```

El Compose importa `agroperuvianfoods-realm.json`. Verifica que el realm `bomerp`, el cliente `bomerp-frontend` y la redirección `http://localhost:4200/auth/callback` existan. Usa las cuentas del entorno entregadas por el responsable; esta documentación no reproduce sus claves.

## 3. Ejecutar el backend con H2

En la misma carpeta `backend`:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2"
```

| Recurso | Dirección local |
|---|---|
| API | `http://localhost:8080` |
| Swagger | `http://localhost:8080/swagger-ui.html` |
| Identidad | `http://localhost:38080/realms/bomerp` |

El perfil H2 carga datos de demostración y crea/reconstruye tablas. Sirve para laboratorio; no conserva una operación empresarial como lo haría una base persistente.

## 4. Ejecutar Angular

Abre otra terminal desde la raíz:

```powershell
cd bomerp-frontend
npm ci
npm start
```

Abre `http://localhost:4200`. `environment.ts` apunta a la API del puerto 8080 y a Keycloak. Verifica esos valores si cambias los puertos.

## 5. Comprobar el recorrido

Inicia sesión, abre o selecciona la caja según tu rol, consulta productos, registra una venta pequeña y verifica ticket, stock y movimiento financiero. No atribuyas una ejecución correcta solo a que ambas aplicaciones arrancaron.

## Oracle

Para iniciar únicamente el contenedor de Oracle:

```powershell
cd backend
docker compose -f compose-dev.yml up -d oracle
```

El contenedor no acredita que todos los esquemas de negocio estén creados. Revisa [preparación Oracle](../bd2/oracle.md) antes de activar el perfil `dev`.

## Problemas frecuentes

| Síntoma | Comprobación |
|---|---|
| Java no compila | JDK 21 y `JAVA_HOME` apuntando al mismo JDK |
| Redirección inválida | Cliente, issuer y URI de callback de Keycloak |
| 401 | Token ausente, vencido o issuer incorrecto |
| 403 | Rol o propiedad de la venta |
| Error de caja | Sesión abierta, identificador y usuario autorizados |
| Error Oracle de tabla o secuencia | Esquema, permisos y scripts ejecutados |
| Puerto ocupado | Proceso activo y puertos configurados en ambas capas |

## Fuentes del proyecto

- [compose-dev.yml](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/compose-dev.yml)
- [application-h2.yml](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/resources/application-h2.yml)
- [environment.ts](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/bomerp-frontend/src/environments/environment.ts)
