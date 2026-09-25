package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.CategoriaProducto;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Producto;
import veterinaria.vargasvet.dto.request.ProductoRequest;
import veterinaria.vargasvet.dto.response.ProductoResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CategoriaProductoRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.ProductoRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.ProductoService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CompanyRepository companyRepository;
    private final CategoriaProductoRepository categoriaProductoRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductoResponse> listar(Integer companyId, int page, int size) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        return productoRepository.findByCompanyId(
                resolvedCompanyId,
                PageRequest.of(page, size, Sort.by("nombre").ascending())
        ).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarActivos(Integer companyId) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        return productoRepository.findByCompanyIdAndActivoTrue(resolvedCompanyId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        String nombre = request.getNombre().trim();
        Integer resolvedCompanyId = resolverCompanyId(request.getCompanyId());
        Company company = companyRepository.findById(resolvedCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con ID: " + resolvedCompanyId));
        CategoriaProducto categoria = obtenerCategoriaDeLaEmpresa(request.getCategoriaId(), resolvedCompanyId);

        Producto producto = new Producto();
        producto.setCompany(company);
        producto.setNombre(nombre);
        producto.setCategoria(categoria);
        producto.setPrecio(request.getPrecio());
        producto.setStock(request.getStock() != null ? request.getStock() : 0);
        producto.setDescripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null);
        producto.setImagenUrl(request.getImagenUrl());
        producto.setActivo(true);

        return toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
        validarPermisoSobreProducto(producto);
        CategoriaProducto categoria = obtenerCategoriaDeLaEmpresa(request.getCategoriaId(), producto.getCompany().getId());

        producto.setNombre(request.getNombre().trim());
        producto.setCategoria(categoria);
        producto.setPrecio(request.getPrecio());
        if (request.getStock() != null) {
            producto.setStock(request.getStock());
        }
        producto.setDescripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null);
        producto.setImagenUrl(request.getImagenUrl());

        return toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
        validarPermisoSobreProducto(producto);
        producto.setActivo(false);
        productoRepository.save(producto);
    }

    @Override
    @Transactional
    public ProductoResponse toggleActivo(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
        validarPermisoSobreProducto(producto);
        producto.setActivo(!Boolean.TRUE.equals(producto.getActivo()));
        return toResponse(productoRepository.save(producto));
    }

    private CategoriaProducto obtenerCategoriaDeLaEmpresa(Long categoriaId, Integer companyId) {
        CategoriaProducto categoria = categoriaProductoRepository.findById(categoriaId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + categoriaId));
        if (categoria.getCompany() == null || !categoria.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("La categoría no pertenece a esta empresa");
        }
        return categoria;
    }

    private ProductoResponse toResponse(Producto p) {
        ProductoResponse r = new ProductoResponse();
        r.setId(p.getId());
        r.setNombre(p.getNombre());
        r.setPrecio(p.getPrecio());
        r.setStock(p.getStock());
        r.setDescripcion(p.getDescripcion());
        r.setImagenUrl(p.getImagenUrl());
        r.setActivo(p.getActivo());
        if (p.getCompany() != null) {
            r.setCompanyId(p.getCompany().getId());
            r.setCompanyName(p.getCompany().getName());
        }
        if (p.getCategoria() != null) {
            r.setCategoriaId(p.getCategoria().getId());
            r.setCategoriaNombre(p.getCategoria().getNombre());
        }
        return r;
    }

    private Integer resolverCompanyId(Integer companyIdParam) {
        if (SecurityUtils.isSuperAdmin()) {
            if (companyIdParam == null) {
                throw new IllegalArgumentException("El parámetro companyId es requerido para SUPER_ADMIN");
            }
            return companyIdParam;
        }
        return SecurityUtils.getCurrentCompanyId();
    }

    private void validarPermisoSobreProducto(Producto producto) {
        if (!SecurityUtils.isSuperAdmin()) {
            Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
            if (producto.getCompany() == null || !producto.getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para modificar este producto");
            }
        }
    }
}
