package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.CategoriaProducto;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.MarcaProducto;
import veterinaria.vargasvet.domain.entity.Producto;
import veterinaria.vargasvet.domain.entity.UnidadMedida;
import veterinaria.vargasvet.dto.request.ProductoRequest;
import veterinaria.vargasvet.dto.response.CategoriaConteoResponse;
import veterinaria.vargasvet.dto.response.ProductoResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CategoriaProductoRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.MarcaProductoRepository;
import veterinaria.vargasvet.repository.ProductoRepository;
import veterinaria.vargasvet.repository.UnidadMedidaRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.ProductoService;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CompanyRepository companyRepository;
    private final CategoriaProductoRepository categoriaProductoRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final MarcaProductoRepository marcaProductoRepository;
    private final AuditLogService auditLogService;

    private static final String SKU_PREFIX = "PRD-";
    private static final String SKU_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int SKU_RANDOM_LENGTH = 12;
    private static final int SKU_GENERATION_ATTEMPTS = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    @Transactional(readOnly = true)
    public Page<ProductoResponse> buscar(Integer companyId, String search, Long categoriaId, Boolean activo, int page, int size) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        Page<ProductoResponse> pagina = productoRepository.buscar(
                resolvedCompanyId,
                search != null ? search.trim() : null,
                categoriaId,
                activo,
                PageRequest.of(page, size, Sort.by("nombre").ascending())
        ).map(this::toResponse);
        enriquecerConProximoVencimiento(pagina.getContent());
        return pagina;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse obtener(String sku, Integer companyId) {
        Producto producto = obtenerPorSku(sku, companyId);
        validarPermisoSobreProducto(producto);
        ProductoResponse response = toResponse(producto);
        enriquecerConProximoVencimiento(List.of(response));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarActivos(Integer companyId) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        List<ProductoResponse> responses = productoRepository.findByCompanyIdAndActivoTrue(resolvedCompanyId)
                .stream().map(this::toResponse).collect(Collectors.toList());
        enriquecerConProximoVencimiento(responses);
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaConteoResponse> conteoPorCategoria(Integer companyId) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        return productoRepository.countActivosPorCategoria(resolvedCompanyId).stream()
                .map(fila -> {
                    CategoriaConteoResponse r = new CategoriaConteoResponse();
                    r.setCategoriaId((Long) fila[0]);
                    r.setCategoriaNombre((String) fila[1]);
                    r.setCantidad((Long) fila[2]);
                    return r;
                })
                .collect(Collectors.toList());
    }

    private void enriquecerConProximoVencimiento(List<ProductoResponse> responses) {
        if (responses.isEmpty()) return;
        List<Long> ids = responses.stream().map(ProductoResponse::getId).collect(Collectors.toList());
        Map<Long, LocalDate> proximos = new HashMap<>();
        for (Object[] fila : productoRepository.findProximoVencimientoPorProducto(ids)) {
            proximos.put((Long) fila[0], (LocalDate) fila[1]);
        }
        responses.forEach(r -> r.setProximoVencimientoLote(proximos.get(r.getId())));
    }

    @Override
    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        String nombre = request.getNombre().trim();
        Integer resolvedCompanyId = resolverCompanyId(request.getCompanyId());
        Company company = companyRepository.findById(resolvedCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con ID: " + resolvedCompanyId));
        CategoriaProducto categoria = obtenerCategoriaDeLaEmpresa(request.getCategoriaId(), resolvedCompanyId);
        UnidadMedida unidadMedida = obtenerUnidadMedidaDeLaEmpresa(request.getUnidadMedidaId(), resolvedCompanyId);
        MarcaProducto marca = obtenerMarcaDeLaEmpresa(request.getMarcaId(), resolvedCompanyId);

        Producto producto = new Producto();
        producto.setCompany(company);
        producto.setNombre(nombre);
        producto.setCategoria(categoria);
        producto.setPrecio(request.getPrecio());
        producto.setCosto(request.getCosto());
        producto.setMarcaProducto(marca);
        producto.setMarca(marca.getNombre());
        producto.setStock(request.getStock() != null ? request.getStock() : 0);
        producto.setStockMinimo(request.getStockMinimo() != null ? request.getStockMinimo() : 0);
        producto.setDescripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null);
        producto.setImagenUrl(request.getImagenUrl());
        producto.setSku(generarSkuAleatorio(resolvedCompanyId));
        producto.setCodigoBarras(normalizarVacio(request.getCodigoBarras()));
        producto.setFechaVencimiento(request.getFechaVencimiento());
        producto.setRequiereReceta(Boolean.TRUE.equals(request.getRequiereReceta()));
        producto.setUnidadMedida(unidadMedida);
        producto.setActivo(true);
        producto.setCreatedBy(SecurityUtils.getCurrentUserEmail());
        producto.setUpdatedBy(SecurityUtils.getCurrentUserEmail());

        Producto guardado = productoRepository.save(producto);
        auditLogService.log(resolvedCompanyId, "CREAR_PRODUCTO", "Inventario",
                "Se creó el producto " + guardado.getNombre() + " (" + guardado.getSku()
                        + ") con la marca " + marca.getNombre());

        return toResponse(guardado);
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(String sku, ProductoRequest request) {
        Producto producto = obtenerPorSku(sku, request.getCompanyId());
        validarPermisoSobreProducto(producto);
        CategoriaProducto categoria = obtenerCategoriaDeLaEmpresa(request.getCategoriaId(), producto.getCompany().getId());
        UnidadMedida unidadMedida = obtenerUnidadMedidaDeLaEmpresa(request.getUnidadMedidaId(), producto.getCompany().getId());
        Long marcaActualId = producto.getMarcaProducto() != null ? producto.getMarcaProducto().getId() : null;
        MarcaProducto marca = obtenerMarcaDeLaEmpresa(
                request.getMarcaId(), producto.getCompany().getId(),
                marcaActualId != null && marcaActualId.equals(request.getMarcaId()));
        String marcaAnterior = producto.getMarcaProducto() != null
                ? producto.getMarcaProducto().getNombre() : producto.getMarca();

        producto.setNombre(request.getNombre().trim());
        producto.setCategoria(categoria);
        producto.setPrecio(request.getPrecio());
        producto.setCosto(request.getCosto());
        producto.setMarcaProducto(marca);
        producto.setMarca(marca.getNombre());
        if (request.getStock() != null) {
            producto.setStock(request.getStock());
        }
        if (request.getStockMinimo() != null) {
            producto.setStockMinimo(request.getStockMinimo());
        }
        producto.setDescripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null);
        producto.setImagenUrl(request.getImagenUrl());
        producto.setCodigoBarras(normalizarVacio(request.getCodigoBarras()));
        producto.setFechaVencimiento(request.getFechaVencimiento());
        producto.setRequiereReceta(Boolean.TRUE.equals(request.getRequiereReceta()));
        producto.setUnidadMedida(unidadMedida);
        producto.setUpdatedBy(SecurityUtils.getCurrentUserEmail());

        Producto guardado = productoRepository.save(producto);
        auditLogService.log(guardado.getCompany().getId(), "ACTUALIZAR_PRODUCTO", "Inventario",
                "Se actualizó el producto " + guardado.getNombre() + " (" + guardado.getSku()
                        + "). Marca: " + (marcaAnterior != null ? marcaAnterior : "sin marca")
                        + " → " + marca.getNombre());

        return toResponse(guardado);
    }

    @Override
    @Transactional
    public void eliminar(String sku, Integer companyId) {
        Producto producto = obtenerPorSku(sku, companyId);
        validarPermisoSobreProducto(producto);
        producto.setActivo(false);
        producto.setUpdatedBy(SecurityUtils.getCurrentUserEmail());
        productoRepository.save(producto);

        auditLogService.log(producto.getCompany().getId(), "DESACTIVAR_PRODUCTO", "Inventario",
                "Se desactivó el producto " + producto.getNombre() + " (" + producto.getSku() + ")");
    }

    @Override
    @Transactional
    public ProductoResponse toggleActivo(String sku, Integer companyId) {
        Producto producto = obtenerPorSku(sku, companyId);
        validarPermisoSobreProducto(producto);
        producto.setActivo(!Boolean.TRUE.equals(producto.getActivo()));
        producto.setUpdatedBy(SecurityUtils.getCurrentUserEmail());

        Producto guardado = productoRepository.save(producto);
        auditLogService.log(guardado.getCompany().getId(),
                Boolean.TRUE.equals(guardado.getActivo()) ? "ACTIVAR_PRODUCTO" : "DESACTIVAR_PRODUCTO",
                "Inventario",
                (Boolean.TRUE.equals(guardado.getActivo()) ? "Se activó" : "Se desactivó") + " el producto " + guardado.getNombre() + " (" + guardado.getSku() + ")");

        return toResponse(guardado);
    }

    private CategoriaProducto obtenerCategoriaDeLaEmpresa(Long categoriaId, Integer companyId) {
        CategoriaProducto categoria = categoriaProductoRepository.findById(categoriaId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + categoriaId));
        if (categoria.getCompany() == null || !categoria.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("La categoría no pertenece a esta empresa");
        }
        return categoria;
    }

    private UnidadMedida obtenerUnidadMedidaDeLaEmpresa(Long unidadMedidaId, Integer companyId) {
        if (unidadMedidaId == null) {
            return null;
        }
        UnidadMedida unidad = unidadMedidaRepository.findById(unidadMedidaId)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad de medida no encontrada con ID: " + unidadMedidaId));
        if (unidad.getCompany() == null || !unidad.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("La unidad de medida no pertenece a esta empresa");
        }
        return unidad;
    }

    private MarcaProducto obtenerMarcaDeLaEmpresa(Long marcaId, Integer companyId) {
        return obtenerMarcaDeLaEmpresa(marcaId, companyId, false);
    }

    private MarcaProducto obtenerMarcaDeLaEmpresa(Long marcaId, Integer companyId, boolean permitirInactivaExistente) {
        MarcaProducto marca = marcaProductoRepository.findById(marcaId)
                .orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada con ID: " + marcaId));
        if (marca.getCompany() == null || !marca.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("La marca no pertenece a esta empresa");
        }
        if (!permitirInactivaExistente && !Boolean.TRUE.equals(marca.getActivo())) {
            throw new IllegalArgumentException("La marca seleccionada está inactiva");
        }
        return marca;
    }

    private String generarSkuAleatorio(Integer companyId) {
        for (int intento = 0; intento < SKU_GENERATION_ATTEMPTS; intento++) {
            StringBuilder codigo = new StringBuilder(SKU_PREFIX);
            for (int i = 0; i < SKU_RANDOM_LENGTH; i++) {
                codigo.append(SKU_ALPHABET.charAt(SECURE_RANDOM.nextInt(SKU_ALPHABET.length())));
            }

            String sku = codigo.toString();
            if (!productoRepository.existsByCompanyIdAndSku(companyId, sku)) {
                return sku;
            }
        }

        throw new IllegalStateException("No se pudo generar un SKU único para el producto");
    }

    private String normalizarVacio(String valor) {
        if (valor == null) return null;
        String trimmed = valor.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ProductoResponse toResponse(Producto p) {
        ProductoResponse r = new ProductoResponse();
        r.setId(p.getId());
        r.setNombre(p.getNombre());
        r.setPrecio(p.getPrecio());
        r.setCosto(p.getCosto());
        if (p.getMarcaProducto() != null) {
            r.setMarcaId(p.getMarcaProducto().getId());
            r.setMarca(p.getMarcaProducto().getNombre());
        } else {
            r.setMarca(p.getMarca());
        }
        r.setStock(p.getStock());
        r.setStockMinimo(p.getStockMinimo());
        r.setDescripcion(p.getDescripcion());
        r.setImagenUrl(p.getImagenUrl());
        r.setSku(p.getSku());
        r.setCodigoBarras(p.getCodigoBarras());
        r.setFechaVencimiento(p.getFechaVencimiento());
        r.setRequiereReceta(p.getRequiereReceta());
        r.setActivo(p.getActivo());
        r.setCreatedAt(p.getCreatedAt());
        r.setCreatedBy(p.getCreatedBy());
        r.setUpdatedAt(p.getUpdatedAt());
        r.setUpdatedBy(p.getUpdatedBy());
        if (p.getCompany() != null) {
            r.setCompanyId(p.getCompany().getId());
            r.setCompanyName(p.getCompany().getName());
        }
        if (p.getCategoria() != null) {
            r.setCategoriaId(p.getCategoria().getId());
            r.setCategoriaNombre(p.getCategoria().getNombre());
        }
        if (p.getUnidadMedida() != null) {
            r.setUnidadMedidaId(p.getUnidadMedida().getId());
            r.setUnidadMedidaNombre(p.getUnidadMedida().getNombre());
        }
        return r;
    }

    private Producto obtenerPorSku(String sku, Integer companyId) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        return productoRepository.findByCompanyIdAndSku(resolvedCompanyId, sku)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
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
