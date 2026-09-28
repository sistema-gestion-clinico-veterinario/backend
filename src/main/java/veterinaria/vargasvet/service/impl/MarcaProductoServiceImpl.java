package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.MarcaProducto;
import veterinaria.vargasvet.dto.request.MarcaProductoRequest;
import veterinaria.vargasvet.dto.response.MarcaProductoResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.MarcaProductoRepository;
import veterinaria.vargasvet.repository.ProductoRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.MarcaProductoService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarcaProductoServiceImpl implements MarcaProductoService {
    private final MarcaProductoRepository marcaProductoRepository;
    private final CompanyRepository companyRepository;
    private final ProductoRepository productoRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public Page<MarcaProductoResponse> listar(Integer companyId, String search, Boolean activo, int page, int size) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        Page<MarcaProducto> resultado = marcaProductoRepository.buscar(
                resolvedCompanyId, normalizarVacio(search), activo,
                PageRequest.of(page, size, Sort.by("nombre").ascending()));
        Map<Long, Long> conteos = contarProductos(resolvedCompanyId, resultado.getContent());
        return resultado.map(marca -> toResponse(marca, conteos.getOrDefault(marca.getId(), 0L)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarcaProductoResponse> listarActivas(Integer companyId) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        List<MarcaProducto> marcas = marcaProductoRepository.findByCompanyIdAndActivoTrueOrderByNombreAsc(resolvedCompanyId);
        Map<Long, Long> conteos = contarProductos(resolvedCompanyId, marcas);
        return marcas.stream()
                .map(marca -> toResponse(marca, conteos.getOrDefault(marca.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional
    public MarcaProductoResponse crear(MarcaProductoRequest request) {
        Integer companyId = resolverCompanyId(request.getCompanyId());
        String nombre = request.getNombre().trim();
        if (marcaProductoRepository.existsByCompanyIdAndNombreIgnoreCase(companyId, nombre)) {
            throw new IllegalArgumentException("Ya existe una marca con ese nombre en la empresa");
        }
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con ID: " + companyId));
        MarcaProducto marca = new MarcaProducto();
        marca.setCompany(company);
        marca.setNombre(nombre);
        marca.setDescripcion(normalizarVacio(request.getDescripcion()));
        marca.setActivo(true);
        marca.setCreatedBy(SecurityUtils.getCurrentUserEmail());
        marca.setUpdatedBy(SecurityUtils.getCurrentUserEmail());
        MarcaProducto guardada = marcaProductoRepository.save(marca);
        auditLogService.log(companyId, "CREAR_MARCA_PRODUCTO", "Inventario",
                "Se creó la marca de producto " + guardada.getNombre());
        return toResponse(guardada, 0L);
    }

    @Override
    @Transactional
    public MarcaProductoResponse actualizar(Long id, MarcaProductoRequest request) {
        MarcaProducto marca = obtenerAccesible(id);
        String nombreAnterior = marca.getNombre();
        String nombreNuevo = request.getNombre().trim();
        Integer companyId = marca.getCompany().getId();
        if (marcaProductoRepository.existsByCompanyIdAndNombreIgnoreCaseAndIdNot(companyId, nombreNuevo, id)) {
            throw new IllegalArgumentException("Ya existe una marca con ese nombre en la empresa");
        }
        marca.setNombre(nombreNuevo);
        marca.setDescripcion(normalizarVacio(request.getDescripcion()));
        marca.setUpdatedBy(SecurityUtils.getCurrentUserEmail());
        MarcaProducto guardada = marcaProductoRepository.save(marca);
        auditLogService.log(companyId, "ACTUALIZAR_MARCA_PRODUCTO", "Inventario",
                "Se actualizó la marca de producto " + nombreAnterior + " a " + guardada.getNombre());
        return toResponse(guardada, productoRepository.countByCompanyIdAndMarcaProductoId(companyId, id));
    }

    @Override
    @Transactional
    public MarcaProductoResponse toggleActivo(Long id) {
        MarcaProducto marca = obtenerAccesible(id);
        marca.setActivo(!Boolean.TRUE.equals(marca.getActivo()));
        marca.setUpdatedBy(SecurityUtils.getCurrentUserEmail());
        MarcaProducto guardada = marcaProductoRepository.save(marca);
        auditLogService.log(guardada.getCompany().getId(),
                Boolean.TRUE.equals(guardada.getActivo()) ? "ACTIVAR_MARCA_PRODUCTO" : "DESACTIVAR_MARCA_PRODUCTO",
                "Inventario",
                (Boolean.TRUE.equals(guardada.getActivo()) ? "Se activó" : "Se desactivó")
                        + " la marca de producto " + guardada.getNombre());
        return toResponse(guardada, productoRepository.countByCompanyIdAndMarcaProductoId(
                guardada.getCompany().getId(), guardada.getId()));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        MarcaProducto marca = obtenerAccesible(id);
        Integer companyId = marca.getCompany().getId();
        long asociados = productoRepository.countByCompanyIdAndMarcaProductoId(companyId, id);
        if (asociados > 0) {
            throw new IllegalStateException("No se puede eliminar la marca porque está asociada a " + asociados
                    + (asociados == 1 ? " producto. " : " productos. ")
                    + "Puedes desactivarla para conservar el historial.");
        }
        marcaProductoRepository.delete(marca);
        auditLogService.log(companyId, "ELIMINAR_MARCA_PRODUCTO", "Inventario",
                "Se eliminó la marca de producto " + marca.getNombre());
    }

    private MarcaProducto obtenerAccesible(Long id) {
        MarcaProducto marca = marcaProductoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada con ID: " + id));
        if (!SecurityUtils.isSuperAdmin()) {
            Integer companyId = SecurityUtils.getCurrentCompanyId();
            if (marca.getCompany() == null || !marca.getCompany().getId().equals(companyId)) {
                throw new IllegalArgumentException("No tienes permiso para modificar esta marca");
            }
        }
        return marca;
    }

    private Map<Long, Long> contarProductos(Integer companyId, List<MarcaProducto> marcas) {
        if (marcas.isEmpty()) return Map.of();
        List<Long> ids = marcas.stream().map(MarcaProducto::getId).toList();
        return productoRepository.countPorMarcaProductoIds(companyId, ids).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    private MarcaProductoResponse toResponse(MarcaProducto marca, long productosAsociados) {
        MarcaProductoResponse response = new MarcaProductoResponse();
        response.setId(marca.getId());
        response.setNombre(marca.getNombre());
        response.setDescripcion(marca.getDescripcion());
        response.setActivo(marca.getActivo());
        response.setProductosAsociados(productosAsociados);
        response.setCreatedAt(marca.getCreatedAt());
        response.setCreatedBy(marca.getCreatedBy());
        response.setUpdatedAt(marca.getUpdatedAt());
        response.setUpdatedBy(marca.getUpdatedBy());
        if (marca.getCompany() != null) {
            response.setCompanyId(marca.getCompany().getId());
            response.setCompanyName(marca.getCompany().getName());
        }
        return response;
    }

    private String normalizarVacio(String valor) {
        if (valor == null) return null;
        String trimmed = valor.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
}
