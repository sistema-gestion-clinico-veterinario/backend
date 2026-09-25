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
import veterinaria.vargasvet.service.CategoriaProductoService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaProductoServiceImpl implements CategoriaProductoService {

    private final CategoriaProductoRepository categoriaProductoRepository;
    private final CompanyRepository companyRepository;

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
        categoria.setActivo(true);

        return toResponse(categoriaProductoRepository.save(categoria));
    }

    @Override
    @Transactional
    public CategoriaProductoResponse actualizar(Long id, CategoriaProductoRequest request) {
        CategoriaProducto categoria = categoriaProductoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));
        validarPermisoSobreCategoria(categoria);

        categoria.setNombre(request.getNombre().trim());

        return toResponse(categoriaProductoRepository.save(categoria));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        CategoriaProducto categoria = categoriaProductoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));
        validarPermisoSobreCategoria(categoria);
        categoria.setActivo(false);
        categoriaProductoRepository.save(categoria);
    }

    @Override
    @Transactional
    public CategoriaProductoResponse toggleActivo(Long id) {
        CategoriaProducto categoria = categoriaProductoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));
        validarPermisoSobreCategoria(categoria);
        categoria.setActivo(!Boolean.TRUE.equals(categoria.getActivo()));
        return toResponse(categoriaProductoRepository.save(categoria));
    }

    private CategoriaProductoResponse toResponse(CategoriaProducto c) {
        CategoriaProductoResponse r = new CategoriaProductoResponse();
        r.setId(c.getId());
        r.setNombre(c.getNombre());
        r.setActivo(c.getActivo());
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
