package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.CategoriaProducto;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.dto.request.CategoriaProductoRequest;
import veterinaria.vargasvet.dto.response.CategoriaProductoResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CategoriaProductoRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CategoriaProductoService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaProductoServiceImpl implements CategoriaProductoService {

    private final CategoriaProductoRepository categoriaProductoRepository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public Page<CategoriaProductoResponse> listar(Integer companyId, int page, int size) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        return categoriaProductoRepository.findByCompanyId(
                resolvedCompanyId,
                PageRequest.of(page, size, Sort.by("nombre").ascending())
        ).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaProductoResponse> listarActivas(Integer companyId) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        return categoriaProductoRepository.findByCompanyIdAndActivoTrue(resolvedCompanyId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoriaProductoResponse crear(CategoriaProductoRequest request) {
        String nombre = request.getNombre().trim();
        Integer resolvedCompanyId = resolverCompanyId(request.getCompanyId());
        Company company = companyRepository.findById(resolvedCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con ID: " + resolvedCompanyId));

        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setCompany(company);
        categoria.setNombre(nombre);
        categoria.setDescripcion(normalizarVacio(request.getDescripcion()));
        categoria.setActivo(true);
        categoria.setCreatedBy(SecurityUtils.getCurrentUserEmail());
        categoria.setUpdatedBy(SecurityUtils.getCurrentUserEmail());

        CategoriaProducto guardada = categoriaProductoRepository.save(categoria);
        auditLogService.log(resolvedCompanyId, "CREAR_CATEGORIA_PRODUCTO", "Inventario",
                "Se creó la categoría de producto " + guardada.getNombre());

        return toResponse(guardada);
    }

    @Override
    @Transactional
    public CategoriaProductoResponse actualizar(Long id, CategoriaProductoRequest request) {
        CategoriaProducto categoria = categoriaProductoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));
        validarPermisoSobreCategoria(categoria);

        categoria.setNombre(request.getNombre().trim());
        categoria.setDescripcion(normalizarVacio(request.getDescripcion()));
        categoria.setUpdatedBy(SecurityUtils.getCurrentUserEmail());

        CategoriaProducto guardada = categoriaProductoRepository.save(categoria);
        auditLogService.log(guardada.getCompany().getId(), "ACTUALIZAR_CATEGORIA_PRODUCTO", "Inventario",
                "Se actualizó la categoría de producto " + guardada.getNombre());

        return toResponse(guardada);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        CategoriaProducto categoria = categoriaProductoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));
        validarPermisoSobreCategoria(categoria);
        categoria.setActivo(false);
        categoria.setUpdatedBy(SecurityUtils.getCurrentUserEmail());
        categoriaProductoRepository.save(categoria);

        auditLogService.log(categoria.getCompany().getId(), "DESACTIVAR_CATEGORIA_PRODUCTO", "Inventario",
                "Se desactivó la categoría de producto " + categoria.getNombre());
    }

    @Override
    @Transactional
    public CategoriaProductoResponse toggleActivo(Long id) {
        CategoriaProducto categoria = categoriaProductoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));
        validarPermisoSobreCategoria(categoria);
        categoria.setActivo(!Boolean.TRUE.equals(categoria.getActivo()));
        categoria.setUpdatedBy(SecurityUtils.getCurrentUserEmail());

        CategoriaProducto guardada = categoriaProductoRepository.save(categoria);
        auditLogService.log(guardada.getCompany().getId(),
                Boolean.TRUE.equals(guardada.getActivo()) ? "ACTIVAR_CATEGORIA_PRODUCTO" : "DESACTIVAR_CATEGORIA_PRODUCTO",
                "Inventario",
                (Boolean.TRUE.equals(guardada.getActivo()) ? "Se activó" : "Se desactivó") + " la categoría de producto " + guardada.getNombre());

        return toResponse(guardada);
    }

    private String normalizarVacio(String valor) {
        if (valor == null) return null;
        String trimmed = valor.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private CategoriaProductoResponse toResponse(CategoriaProducto c) {
        CategoriaProductoResponse r = new CategoriaProductoResponse();
        r.setId(c.getId());
        r.setNombre(c.getNombre());
        r.setDescripcion(c.getDescripcion());
        r.setActivo(c.getActivo());
        r.setCreatedAt(c.getCreatedAt());
        r.setCreatedBy(c.getCreatedBy());
        r.setUpdatedAt(c.getUpdatedAt());
        r.setUpdatedBy(c.getUpdatedBy());
        if (c.getCompany() != null) {
            r.setCompanyId(c.getCompany().getId());
            r.setCompanyName(c.getCompany().getName());
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

    private void validarPermisoSobreCategoria(CategoriaProducto categoria) {
        if (!SecurityUtils.isSuperAdmin()) {
            Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
            if (categoria.getCompany() == null || !categoria.getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para modificar esta categoría");
            }
        }
    }
}
