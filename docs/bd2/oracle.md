# Preparación Oracle

## Qué existe

El repositorio contiene DDL, PL/SQL y un perfil de conexión Oracle. El Compose dispone un contenedor Oracle Free. La presencia de archivos no garantiza un orden de instalación consistente ni su compatibilidad completa con todas las entidades actuales.

| Archivo en `backend/src/main/resources/db` | Finalidad identificada |
|---|---|
| `schema-oracle.sql` | Tablas y restricciones de varios módulos |
| `oracle_ventas_pos.sql` | Objetos del proceso de ventas |
| `oracle_inventario_fefo.sql` | Inventario y despacho por lote |
| `oracle_compras_terceros.sql` | Compras y datos de terceros |
| `oracle_produccion_bom.sql` | Estructura de producción |
| `oracle_finanzas_plsql.sql` | Objetos financieros |
| `oracle_ventas_ticket_sequence.sql` | Secuencia de tickets, permiso y unicidad |
| `PROGRAMACION_PLSQL_BD2.sql` | Procedimientos, funciones y triggers de práctica |
| `EVIDENCIAS_PRUEBAS_PLSQL.sql` | Casos SQL para comprobar objetos |

## Procedimiento de preparación

1. Crear una base de laboratorio aislada y verificar el servicio de la PDB.
2. Revisar propietarios/esquemas y permisos requeridos por cada script.
3. Comparar las entidades actuales con el DDL: columnas de operación, caja, estados, versiones y tipos.
4. Elegir una versión de esquema consolidada; no ejecutar todos los scripts indiscriminadamente si redefinen objetos.
5. Preparar cuentas de aplicación con los permisos necesarios y configurar la conexión sin publicar claves.
6. Ejecutar la secuencia de tickets con una cuenta autorizada.
7. Verificar objetos inválidos, restricciones y grants.
8. Ejecutar una venta real desde la API y comprobar sus efectos en Oracle.

!!! warning "Integridad entre Java y PL/SQL"
    Revisar los triggers que descuentan stock: si Java y un trigger aplican el mismo descuento a una venta, el inventario podría modificarse dos veces. Documentar qué capa es responsable antes de habilitarlos juntos.

## Consultas de verificación

Con una cuenta que tenga visibilidad sobre los objetos:

```sql
SELECT owner, object_name, object_type, status
FROM all_objects
WHERE owner IN ('BOM_VENTAS', 'BOM_CATALOGO', 'BOM_COMPRAS', 'BOM_PRODUCCION')
  AND status <> 'VALID';

SELECT sequence_owner, sequence_name, increment_by
FROM all_sequences
WHERE sequence_owner = 'BOM_VENTAS'
  AND sequence_name = 'SEQ_TICKET_VENTA';
```

Estas consultas inspeccionan metadata. Su salida debe guardarse con fecha, entorno y usuario de ejecución; aquí no se atribuye un resultado aprobado.

## Fuentes del proyecto

- [schema-oracle.sql](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/resources/db/schema-oracle.sql)
- [oracle_ventas_ticket_sequence.sql](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/resources/db/oracle_ventas_ticket_sequence.sql)
- [PROGRAMACION_PLSQL_BD2.sql](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/resources/db/PROGRAMACION_PLSQL_BD2.sql)
- [application-dev.yml](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/resources/application-dev.yml)
