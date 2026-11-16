# Publicación en GitHub Pages

## Estado

El repositorio no tenía Pages activado al preparar esta documentación. Subir las fuentes a una rama no activa el alojamiento web. La cuenta conectada puede escribir código, pero no tiene permisos de administración para cambiar Pages.

## Publicar después de revisar la documentación

1. Incorporar la solicitud de documentación en la rama `ventas_avance` y obtener esa versión en VS Code.
2. Desde la raíz del proyecto, preparar el entorno de documentación siguiendo `LEEME_DOCUMENTACION.md`.
3. Comprobar la compilación:

```powershell
.\.venv-docs\Scripts\python.exe -m mkdocs build --strict
```

4. Con una cuenta que tenga permiso de escritura y una sesión Git configurada, publicar la salida estática en `gh-pages`:

```powershell
.\.venv-docs\Scripts\python.exe -m mkdocs gh-deploy
```

Este comando publica mediante Git: ejecutarlo cuando se haya decidido publicar, no como requisito para consultar localmente. Conserva el historial de `gh-pages` si ya existe; revisar la estrategia antes de combinarla con otro sitio.

5. El propietario abre **Settings → Pages**, selecciona **Deploy from a branch**, rama **gh-pages**, carpeta **/(root)** y guarda.
6. Esperar la finalización de Pages y abrir el enlace que GitHub muestre en esa pantalla.
7. Revisar portada, búsqueda, diagramas y enlaces desde la URL pública.

El sitio expone documentación del proyecto. No ejecuta la aplicación Spring Boot, la SPA del ERP, Oracle ni Keycloak. No se necesita publicar credenciales del entorno para servir estas páginas.

## Mantener el sitio

Actualizar Markdown, reconstruir en modo estricto y publicar de nuevo cuando corresponda. Conservar en las páginas el commit del código descrito y las evidencias pendientes.

## Referencia técnica

[Guía oficial de despliegue de MkDocs](https://www.mkdocs.org/user-guide/deploying-your-docs/).
