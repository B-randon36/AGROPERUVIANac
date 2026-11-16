# Arquitectura C4 y Despliegue

El sistema **GDMERP (Agro Peruvian Foods)** sigue una arquitectura **Monolito Modular** basada en **Spring Modulith**, desacoplada mediante interfaces de servicio internas (`FinanzasApi`), seguridad federada OIDC con **Keycloak** y una aplicación web **SPA en Angular**.

---

## C1 · Diagrama de Contexto del Sistema

La vista C1 muestra la interacción entre los actores del negocio agroindustrial, el núcleo del sistema **GDMERP** y los sistemas externos de identidad y consulta tributaria.

```mermaid
flowchart TB
    subgraph Actores["Actores del Negocio"]
        VENDEDOR["👤 Ejecutivo de Ventas / Cajero<br/>(Registra ventas POS, abre/cierra caja,<br/>emite tickets 80mm, solicita anulaciones)"]
        SUPERVISOR["🛡️ Supervisor / Administrador<br/>(Aprueba/rechaza anulaciones, gestiona<br/>catálogo, producción, compras y reportes)"]
        CLIENTE["🏪 Cliente Mayorista / Minorista<br/>(Compra en ventanilla o delivery,<br/>recibe comprobante y despacho)"]
    end

    GDMERP["🌿 Sistema ERP Agro Peruvian Foods<br/>(GDMERP)<br/>Gestión integral de Ventas POS, Caja,<br/>Inventario FEFO, Compras y Producción"]

    subgraph Externos["Sistemas Externos"]
        KEYCLOAK["🔐 Keycloak 26.8 (IdP OIDC)<br/>Autenticación JWT, SSO y roles<br/>(ADMIN, SUPERVISOR, VENDEDOR)"]
        SUNAT["🏛️ Proveedor Consulta DNI / RUC<br/>Validación de identidad y razón social<br/>para clientes y facturación"]
    end

    VENDEDOR -->|"Opera POS, cobra efectivo/Yape,<br/>imprime ticket (HTTPS)"| GDMERP
    SUPERVISOR -->|"Audita operaciones, gestiona lotes,<br/>aprueba anulaciones (HTTPS)"| GDMERP
    CLIENTE -.->|"Entrega datos DNI/RUC y pago;<br/>recibe ticket y productos"| VENDEDOR

    GDMERP -->|"Valida tokens JWT y roles (OIDC)"| KEYCLOAK
    VENDEDOR -->|"Inicia sesión (Authorization Code + PKCE)"| KEYCLOAK
    SUPERVISOR -->|"Inicia sesión (Authorization Code + PKCE)"| KEYCLOAK
    GDMERP -->|"Consulta datos fiscales por DNI/RUC (REST)"| SUNAT

    classDef core fill:#166534,stroke:#14532d,color:#ffffff,stroke-width:2px;
    classDef actor fill:#0f766e,stroke:#115e59,color:#ffffff;
    classDef ext fill:#334155,stroke:#1e293b,color:#ffffff;
    class GDMERP core;
    class VENDEDOR,SUPERVISOR,CLIENTE actor;
    class KEYCLOAK,SUNAT ext;
```

---

## C2 · Diagrama de Contenedores

La vista C2 detalla las unidades de ejecución desplegables del sistema, sus protocolos de comunicación y almacenamiento persistente.

```mermaid
flowchart LR
    USER["👤 Usuario<br/>(Navegador Web)"]

    subgraph Frontend["Contenedor Frontend"]
        SPA["🅰️ SPA Angular 22<br/>Puerto :4200<br/>Standalone Components, Signals,<br/>Calculadora POS, Modal Ticket 80mm"]
    end

    subgraph Backend["Contenedor Backend"]
        API["🍃 API REST Spring Boot 4.0.7<br/>Java 21 · Puerto :8080<br/>Spring Modulith + OAuth2 Resource Server<br/>Módulos: Ventas, Finanzas, Inventario,<br/>Producto, Compras, Producción, Auditoría"]
    end

    subgraph Auth["Infraestructura de Identidad (Docker)"]
        KC["🔐 Keycloak 26.8.0<br/>Puerto :38080<br/>Realm: bomerp"]
        KCDB[("🐘 PostgreSQL 17<br/>Puerto :35432<br/>Datos de Keycloak")]
    end

    subgraph Datos["Persistencia ERP"]
        DB[("🗄️ Base de Datos ERP<br/>H2 (Perfil dev/test)<br/>Oracle Database :1521 (Prod)")]
    end

    USER -->|"HTTPS / UI"| SPA
    USER -->|"Login OIDC"| KC
    SPA -->|"REST / JSON + Bearer JWT<br/>(CORS habilitado)"| API
    SPA -->|"PKCE Token Exchange"| KC
    API -->|"Validación de firma JWK"| KC
    KC -->|"JDBC"| KCDB
    API -->|"Spring Data JPA / Hibernate"| DB

    classDef web fill:#0284c7,stroke:#0369a1,color:#ffffff;
    classDef api fill:#15803d,stroke:#166534,color:#ffffff;
    classDef sec fill:#7c3aed,stroke:#5b21b6,color:#ffffff;
    classDef db fill:#b45309,stroke:#92400e,color:#ffffff;
    class SPA web;
    class API api;
    class KC sec;
    class KCDB,DB db;
```

---

## C3 · Diagrama de Componentes (Módulos Ventas ↔ Inventario ↔ Finanzas)

El núcleo comercial interactúa internamente dentro de la misma JVM mediante servicios e interfaces declaradas, garantizando atomicidad (`@Transactional`) sin latencia de red entre módulos.

```mermaid
flowchart TB
    CTRL["VentaController<br/>/api/v1/ventas<br/>Extrae 'sub' del JWT y delega"]
    SVC["VentaServiceImpl<br/>Orquestador transaccional<br/>(RN-VEN-01 a RN-VEN-10)"]
    TICKET["NumeroTicketService<br/>Generación única secuencial<br/>(RN-VEN-04)"]
    PROD["ProductoServiceImpl<br/>Catálogo, precios y stock global"]
    INV["InventarioServiceImpl<br/>Despacho automático FEFO<br/>y reversión por lote original"]
    FIN["FinanzasApiImpl<br/>Registro en SesionCaja activa<br/>y CuentaPorCobrar (crédito)"]
    AUDIT["AuditoriaService<br/>Trazabilidad de eventos"]
    REPO[("VentaRepository<br/>Persistencia JPA + Lock")]

    CTRL --> SVC
    SVC --> TICKET
    SVC --> PROD
    SVC --> INV
    SVC --> FIN
    SVC --> AUDIT
    SVC --> REPO

    classDef ctrl fill:#0369a1,stroke:#075985,color:#ffffff;
    classDef svc fill:#166534,stroke:#14532d,color:#ffffff;
    classDef mod fill:#0f766e,stroke:#115e59,color:#ffffff;
    classDef repo fill:#b45309,stroke:#92400e,color:#ffffff;
    class CTRL ctrl;
    class SVC svc;
    class TICKET,PROD,INV,FIN,AUDIT mod;
    class REPO repo;
```

---

## Puertos y Despliegue Validado

| Componente | Tecnología | Puerto | Rol en el Ecosistema |
|---|---|---|---|
| **Frontend Web** | Angular 22 (`npm start`) | `4200` | Interfaz POS, gestión de cajas, reportes y administración |
| **Backend ERP** | Spring Boot 4.0.7 / Java 21 | `8080` | API REST unificada, reglas de negocio y persistencia JPA |
| **Servidor OIDC** | Keycloak 26.8.0 (Docker) | `38080` | Emisión de tokens JWT y gestión de usuarios/roles (`bomerp`) |
| **BD Keycloak** | PostgreSQL 17 Alpine (Docker) | `35432` | Almacenamiento de usuarios, clientes y sesiones del IdP |
| **BD ERP (Dev/Test)** | H2 Database (embebida) | En memoria | Perfil `h2` para desarrollo ágil y suite de 105 pruebas |
| **BD ERP (Prod)** | Oracle Database | `1521` | Destino empresarial con secuencias dedicadas para tickets |
| **Observabilidad** | Grafana / Loki (Compose) | `33000` / `33100` | Recolección centralizada de logs y métricas de contenedores |

## Límites Modulares (Spring Modulith)

La estructura de paquetes respeta el principio de **Monolito Modular**: un único artefacto desplegable (`BomerpBackendApplication`) dividido en dominios autónomos (`ventas`, `finanzas`, `inventario`, `producto`, `compras`, `produccion`, `terceros`, `auditoria`). La comunicación entre módulos se realiza exclusivamente mediante interfaces públicas (como `FinanzasApi`), verificada automáticamente por la prueba de arquitectura `ModularityTests`.

## Fuentes del Proyecto

- [BomerpBackendApplication.java](https://github.com/B-randon36/AGROPERUVIANac/blob/main/backend/src/main/java/pe/edu/upeu/bomerp/BomerpBackendApplication.java)
- [VentaServiceImpl.java](https://github.com/B-randon36/AGROPERUVIANac/blob/main/backend/src/main/java/pe/edu/upeu/bomerp/ventas/service/VentaServiceImpl.java)
- [ModularityTests.java](https://github.com/B-randon36/AGROPERUVIANac/blob/main/backend/src/test/java/pe/edu/upeu/bomerp/ModularityTests.java)
- [compose-dev.yml](https://github.com/B-randon36/AGROPERUVIANac/blob/main/backend/compose-dev.yml)
