# Plan de Pruebas y Evidencias

## Resultado Verificado de la Suite Automatizada

El proyecto cuenta con una batería automatizada ejecutada sobre **Spring Boot Test + JUnit 5 + Mockito + Spring Security Test + H2** con resultado **100% exitoso**:

| Métrica | Resultado Obtenido |
|---|---|
| **Total de Pruebas Backend** | **105 pruebas ejecutadas** (`Tests run: 105`) |
| **Fallos / Errores** | `0` fallos, `0` errores (`100%` aprobadas) |
| **Suite Crítica de Ventas & Caja** | `44/44` pruebas pasando (`VentaServiceTest`: 18, `VentaSecurityTest`: 12, `VentaPersistenciaTest`: 4, `VentaEndToEndTest`: 3, `SesionCajaServiceTest`: 7) |
| **Arquitectura Modular** | `ModularityTests` (`ApplicationModules.verify()`) pasando sin violaciones |
| **Validación en Vivo (E2E Manual)** | Keycloak (`:38080`), Backend Spring Boot (`:8080`) y Frontend Angular (`:4200`) operativos |

---

## Casos de Prueba Cubiertos

| ID | Escenario | Resultado Verificado | Clase de Prueba |
|---|---|---|---|
| **CP-01** | Venta válida POS | Cabecera, detalles, ticket único y totales coherentes | `VentaServiceTest` |
| **CP-02** | Desglose tributario IGV 18% | Precio final S/ 118 → Base S/ 100.00 + IGV S/ 18.00 (`BigDecimal`) | `VentaServiceTest` |
| **CP-03** | Stock insuficiente | Rechazo inmediato con rollback transaccional sin efectos parciales | `VentaServiceTest` |
| **CP-04** | Segregación por vendedor (`RN-VEN-08`) | Vendedor solo accede/solicita anulación sobre sus propias ventas | `VentaSecurityTest` |
| **CP-05** | Control de acceso OIDC (`401`/`403`) | Rechazo a peticiones anónimas o roles no autorizados | `VentaSecurityTest` |
| **CP-06** | Concurrencia e idempotencia (`RN-VEN-04`/`06`) | Numeración única secuencial y reutilización segura por `operacionId` | `VentaPersistenciaTest` |
| **CP-07** | Anulación completa (`RN-VEN-07`) | Cambio de estado a `ANULADA`, extorno en caja/CxC y auditoría | `VentaEndToEndTest` |
| **CP-08** | Reversión multilote FEFO (`RN-VEN-09`) | Devolución exacta a los lotes originales descontados por vencimiento | `VentaServiceTest` |
| **CP-09** | Apertura duplicada de caja (`RN-FIN-02`) | Bloqueo de doble turno simultáneo por usuario | `SesionCajaServiceTest` |
| **CP-10** | Límites Spring Modulith | Cero dependencias circulares o acoplamiento ilegal entre módulos | `ModularityTests` |

---

## Comandos para Reproducir las Pruebas

### Backend (Java 21 + Maven Wrapper)
```powershell
cd backend
.\mvnw.cmd test
```

### Frontend (Angular 22 + Vitest/Karma)
```powershell
cd bomerp-frontend
npm ci
npm test -- --watch=false
npm run build
```

---

## Datos Semilla Integrados para Demostración

El inicializador `VentaDataInitializer` carga automáticamente **10 ventas realistas** al iniciar el perfil `h2`, cubriendo los canales POS, Delivery y Tienda Virtual, todos los estados (`REGISTRADA`, `SOLICITADA_ANULACION`, `ANULADA`), y los métodos de pago (`EFECTIVO`, `YAPE`, `PLIN`, `TARJETA`, `TRANSFERENCIA`, `CREDITO`) vinculados con los usuarios de Keycloak (`vendedor1`, `supervisor`, `admin`).

## Fuentes del Proyecto

- [VentaServiceTest.java](https://github.com/B-randon36/AGROPERUVIANac/blob/main/backend/src/test/java/pe/edu/upeu/bomerp/ventas/VentaServiceTest.java)
- [VentaSecurityTest.java](https://github.com/B-randon36/AGROPERUVIANac/blob/main/backend/src/test/java/pe/edu/upeu/bomerp/ventas/VentaSecurityTest.java)
- [VentaPersistenciaTest.java](https://github.com/B-randon36/AGROPERUVIANac/blob/main/backend/src/test/java/pe/edu/upeu/bomerp/ventas/VentaPersistenciaTest.java)
- [VentaEndToEndTest.java](https://github.com/B-randon36/AGROPERUVIANac/blob/main/backend/src/test/java/pe/edu/upeu/bomerp/ventas/VentaEndToEndTest.java)
- [ModularityTests.java](https://github.com/B-randon36/AGROPERUVIANac/blob/main/backend/src/test/java/pe/edu/upeu/bomerp/ModularityTests.java)
