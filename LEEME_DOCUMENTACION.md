# Documentación web AGROPERUVIAN

Este paquete contiene una documentación navegable de 27 páginas creada con MkDocs Material, similar en organización al portal académico BOM ERP. Sus fuentes corresponden a la rama ventas_avance, commit 41bd74a.

## Ver la web ya compilada

1. Descomprime el ZIP.
2. Abre esta carpeta en Visual Studio Code.
3. En la terminal, ejecuta:

```powershell
python -m http.server 8000 --directory site
```

Abre http://localhost:8000. Mantén abierta la terminal; Ctrl+C detiene el servidor. Usar el servidor local permite que funcione la búsqueda. También puedes abrir site/index.html directamente, pero la búsqueda puede fallar por restricciones del navegador con archivos locales.

En Windows, si no existe el comando python, prueba py en su lugar.

## Editar y reconstruir

```powershell
python -m venv .venv-docs
.\.venv-docs\Scripts\python.exe -m pip install -r requirements-docs.txt
.\.venv-docs\Scripts\python.exe -m mkdocs serve
```

Abre http://127.0.0.1:8000. Edita las páginas de docs/ en VS Code y guarda; MkDocs actualiza la vista. No edites site/: son archivos generados.

Para reconstruir la web y comprobar enlaces:

```powershell
.\.venv-docs\Scripts\python.exe -m mkdocs build --strict
```

## Integrar en tu proyecto

Copia docs/, mkdocs.yml y requirements-docs.txt a la raíz del repositorio de AGROPERUVIAN. Conserva o combina cualquier documentación existente; no reemplaces archivos que ya hayas trabajado sin revisarlos. La documentación no requiere modificar backend ni bomerp-frontend.

No copies .venv-docs/ ni el resultado site/ a la historia de código salvo que tu estrategia de publicación lo requiera. El paquete no incluye un flujo de publicación automática.

## Contenido

- Inicio y guía de lectura.
- Proyecto: problema, alcance, equipo, módulos, reglas, limitaciones y fuentes.
- Instalación y uso.
- ADS: contexto y contenedores C4, componentes, dominio, UML, patrones y ADR.
- BD2: inventario de entidades, scripts Oracle y operación.
- LP2: backend, frontend, catálogo de rutas HTTP, seguridad y despliegue.
- Evidencias: plan de pruebas, trazabilidad, checklist y sustentación.

## Alcance de la revisión

El build de documentación se ejecutó en modo estricto. Se comprobaron referencias internas y archivos enlazados del commit de origen. No se ejecutó la suite actual del ERP, no se restauró Oracle y no se atribuye aprobación a evidencias pendientes.

La captura automática de una vista de navegador no pudo realizarse por una restricción del entorno de previsualización. El diseño usa el tema Material adaptable y diagramas SVG; abre la web local para revisar su apariencia en tu pantalla.

Los comentarios y diferencias del código se distinguen de decisiones aprobadas por el equipo. Las fuentes de configuraciones se enlazan, pero no se reproducen sus contraseñas ni tokens. Revisa las páginas de pendientes antes de presentar el cierre académico.

Las fuentes se preparan en la rama docs/sitio-documentacion-agroperuvian, con una solicitud de cambios hacia ventas_avance. GitHub Pages todavía requiere configuración del propietario.
