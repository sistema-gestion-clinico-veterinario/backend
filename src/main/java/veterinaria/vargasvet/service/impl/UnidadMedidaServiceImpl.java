package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.UnidadMedida;
import veterinaria.vargasvet.dto.request.UnidadMedidaRequest;
import veterinaria.vargasvet.dto.response.UnidadMedidaResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.ProductoRepository;
import veterinaria.vargasvet.repository.UnidadMedidaRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.UnidadMedidaService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UnidadMedidaServiceImpl implements UnidadMedidaService {

    private final UnidadMedidaRepository unidadMedidaRepository;
    private final CompanyRepository companyRepository;
    private final ProductoRepository productoRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public Page<UnidadMedidaResponse> listar(Integer companyId, String search, Boolean activo, int page, int size) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        Page<UnidadMedida> resultado = unidadMedidaRepository.buscar(
                resolvedCompanyId,
                normalizarVacio(search),
                activo,
                PageRequest.of(page, size, Sort.by("nombre").ascending())
        );
        Map<Long, Long> conteos = contarProductos(resolvedCompanyId, resultado.getContent());
        return resultado.map(unidad -> toResponse(unidad, conteos.getOrDefault(unidad.getId(), 0L)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnidadMedidaResponse> listarActivas(Integer companyId) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        List<UnidadMedida> unidades = unidadMedidaRepository.findByCompanyIdAndActivoTrue(resolvedCompanyId);
        Map<Long, Long> conteos = contarProductos(resolvedCompanyId, unidades);
        return unidades.stream()
                .map(unidad -> toResponse(unidad, conteos.getOrDefault(unidad.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UnidadMedidaResponse crear(UnidadMedidaRequest request) {
        String nombre = request.getNombre().trim();
        Integer resolvedCompanyId = resolverCompanyId(request.getCompanyId());
        Company company = companyRepository.findById(resolvedCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con ID: " + resolvedCompanyId));

        UnidadMedida unidad = new UnidadMedida();
        unidad.setCompany(company);
        unidad.setNombre(nombre);
        unidad.setDescripcion(normalizarVacio(request.getDescripcion()));
        unidad.setActivo(true);
        unidad.setCreatedBy(SecurityUtils.getCurrentUserEmail());
        unidad.setUpdatedBy(SecurityUtils.getCurrentUserEmail());

        UnidadMedida guardada = unidadMedidaRepository.save(unidad);
        auditLogService.log(resolvedCompanyId, "CREAR_UNIDAD_MEDIDA", "Inventario",
                "Se creó la unidad de medida " + guardada.getNombre());

        return toResponse(guardada, 0L);
    }

    @Override
    @Transactional
    public UnidadMedidaResponse actualizar(Long id, UnidadMedidaRequest request) {
        UnidadMedida unidad = unidadMedidaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad de medida no encontrada con ID: " + id));
        validarPermisoSobreUnidad(unidad);

        unidad.setNombre(request.getNombre().trim());
        unidad.setDescripcion(normalizarVacio(request.getDescripcion()));
        unidad.setUpdatedBy(SecurityUtils.getCurrentUserEmail());

        UnidadMedida guardada = unidadMedidaRepository.save(unidad);
        auditLogService.log(guardada.getCompany().getId(), "ACTUALIZAR_UNIDAD_MEDIDA", "Inventario",
                "Se actualizó la unidad de medida " + guardada.getNombre());

        return toResponse(guardada, productoRepository.countByCompanyIdAndUnidadMedidaId(
                guardada.getCompany().getId(), guardada.getId()));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        UnidadMedida unidad = unidadMedidaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad de medida no encontrada con ID: " + id));
        validarPermisoSobreUnidad(unidad);
        long productosAsociados = productoRepository.countByCompanyIdAndUnidadMedidaId(
                unidad.getCompany().getId(), unidad.getId());
        if (productosAsociados > 0) {
            throw new IllegalStateException("No se puede eliminar la unidad de medida porque está asociada a "
                    + productosAsociados + (productosAsociados == 1 ? " producto. " : " productos. ")
                    + "Puedes desactivarla para conservar el historial.");
        }
        unidadMedidaRepository.delete(unidad);

        auditLogService.log(unidad.getCompany().getId(), "ELIMINAR_UNIDAD_MEDIDA", "Inventario",
                "Se eliminó la unidad de medida " + unidad.getNombre());
    }

    @Override
    @Transactional
    public UnidadMedidaResponse toggleActivo(Long id) {
        UnidadMedida unidad = unidadMedidaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad de medida no encontrada con ID: " + id));
        validarPermisoSobreUnidad(unidad);
        unidad.setActivo(!Boolean.TRUE.equals(unidad.getActivo()));
        unidad.setUpdatedBy(SecurityUtils.getCurrentUserEmail());

        UnidadMedida guardada = unidadMedidaRepository.save(unidad);
        auditLogService.log(guardada.getCompany().getId(),
                Boolean.TRUE.equals(guardada.getActivo()) ? "ACTIVAR_UNIDAD_MEDIDA" : "DESACTIVAR_UNIDAD_MEDIDA",
                "Inventario",
                (Boolean.TRUE.equals(guardada.getActivo()) ? "Se activó" : "Se desactivó") + " la unidad de medida " + guardada.getNombre());

        return toResponse(guardada, productoRepository.countByCompanyIdAndUnidadMedidaId(
                guardada.getCompany().getId(), guardada.getId()));
    }

    private String normalizarVacio(String valor) {
        if (valor == null) return null;
        String trimmed = valor.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UnidadMedidaResponse toResponse(UnidadMedida u, long productosAsociados) {
        UnidadMedidaResponse r = new UnidadMedidaResponse();
        r.setId(u.getId());
        r.setNombre(u.getNombre());
        r.setDescripcion(u.getDescripcion());
        r.setActivo(u.getActivo());
        r.setProductosAsociados(productosAsociados);
        r.setCreatedAt(u.getCreatedAt());
        r.setCreatedBy(u.getCreatedBy());
        r.setUpdatedAt(u.getUpdatedAt());
        r.setUpdatedBy(u.getUpdatedBy());
        if (u.getCompany() != null) {
            r.setCompanyId(u.getCompany().getId());
            r.setCompanyName(u.getCompany().getName());
        }
        return r;
    }

    private Map<Long, Long> contarProductos(Integer companyId, List<UnidadMedida> unidades) {
        if (unidades.isEmpty()) return Map.of();
        List<Long> ids = unidades.stream().map(UnidadMedida::getId).toList();
        return productoRepository.countPorUnidadMedidaIds(companyId, ids).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
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

    private void validarPermisoSobreUnidad(UnidadMedida unidad) {
        if (!SecurityUtils.isSuperAdmin()) {
            Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
            if (unidad.getCompany() == null || !unidad.getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para modificar esta unidad de medida");
            }
        }
    }
}
