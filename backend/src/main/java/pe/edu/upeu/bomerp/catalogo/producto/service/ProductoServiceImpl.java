package pe.edu.upeu.bomerp.catalogo.producto.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.bomerp.catalogo.categoria.dto.CategoriaResumen;
import pe.edu.upeu.bomerp.catalogo.categoria.entity.Categoria;
import pe.edu.upeu.bomerp.catalogo.categoria.repository.CategoriaRepository;
import pe.edu.upeu.bomerp.catalogo.producto.dto.ProductoRequest;
import pe.edu.upeu.bomerp.catalogo.producto.dto.ProductoResponse;
import pe.edu.upeu.bomerp.catalogo.producto.entity.Producto;
import pe.edu.upeu.bomerp.catalogo.producto.repository.ProductoRepository;
import pe.edu.upeu.bomerp.exception.BusinessConflictException;
import pe.edu.upeu.bomerp.exception.ResourceNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(Long categoriaId) {
        List<Producto> productos = (categoriaId != null)
                ? productoRepository.findByCategoria_Id(categoriaId)
                : productoRepository.findAll();
        return productos.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
        return toResponse(producto);
    }

    @Override
    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Categoria categoria = null;
        if (request.getCategoriaId() != null) {
            categoria = categoriaRepository.findById(request.getCategoriaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + request.getCategoriaId()));
        }

        Producto producto = Producto.builder()
                .nombre(request.getNombre())
                .precio(request.getPrecio())
                .stock(request.getStock())
                .imagenUrl(request.getImagenUrl())
                .categoria(categoria)
                .build();

        return toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        if (request.getCategoriaId() != null) {
            Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + request.getCategoriaId()));
            producto.setCategoria(categoria);
        }

        producto.setNombre(request.getNombre());
        producto.setPrecio(request.getPrecio());
        producto.setStock(request.getStock());
        if (request.getImagenUrl() != null) {
            producto.setImagenUrl(request.getImagenUrl());
        }

        return toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!productoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Producto no encontrado con ID: " + id);
        }
        productoRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void descontarStock(Long id, Integer cantidad) {
        // [RN-VEN-01] Control Atómico de Existencias con Bloqueo Pesimista (SELECT FOR UPDATE):
        // Garantiza aislamiento serializable ante ventas concurrentes en mostrador o delivery.
        // Si el stock actual es insuficiente, aborta la transacción lanzando BusinessConflictException.
        Producto producto = productoRepository.findByIdForUpdate(id)
                .or(() -> productoRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        if (producto.getStock() < cantidad) {
            throw new BusinessConflictException("Stock insuficiente para el producto '" + producto.getNombre()
                    + "'. Stock disponible: " + producto.getStock() + ", solicitado: " + cantidad);
        }

        producto.setStock(producto.getStock() - cantidad);
        productoRepository.save(producto);
    }

    @Override
    @Transactional
    public void aumentarStock(Long id, Integer cantidad) {
        restaurarStock(id, cantidad);
    }

    @Override
    @Transactional
    public void restaurarStock(Long id, Integer cantidad) {
        // [RN-VEN-09] Restitución Atómica de Existencias por Anulación de Venta:
        // Suma de vuelta la cantidad exacta vendida al inventario general del producto en catálogo.
        Producto producto = productoRepository.findByIdForUpdate(id)
                .or(() -> productoRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        producto.setStock(producto.getStock() + cantidad);
        productoRepository.save(producto);
    }

    private ProductoResponse toResponse(Producto p) {
        CategoriaResumen cat = null;
        if (p.getCategoria() != null) {
            cat = new CategoriaResumen(p.getCategoria().getId(), p.getCategoria().getNombre());
        }
        return ProductoResponse.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .precio(p.getPrecio())
                .stock(p.getStock())
                .imagenUrl(p.getImagenUrl() != null && !p.getImagenUrl().isBlank()
                        ? p.getImagenUrl()
                        : "/assets/productos/producto-" + p.getId() + ".png")
                .categoria(cat)
                .build();
    }
}
