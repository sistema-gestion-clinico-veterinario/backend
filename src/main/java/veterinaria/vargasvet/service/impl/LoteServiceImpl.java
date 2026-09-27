package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Lote;
import veterinaria.vargasvet.domain.entity.Producto;
import veterinaria.vargasvet.dto.request.LoteRequest;
import veterinaria.vargasvet.dto.response.AlertaVencimientoResponse;
import veterinaria.vargasvet.dto.response.LoteResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.LoteRepository;
import veterinaria.vargasvet.repository.ProductoRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.LoteService;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoteServiceImpl implements LoteService {

    private final LoteRepository loteRepository;
    private final ProductoRepository productoRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public Page<LoteResponse> listar(Integer companyId, String search, int page, int size) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        return loteRepository.buscar(
                resolvedCompanyId,
                search != null ? search.trim() : null,
                PageRequest.of(page, size, Sort.by("fechaVencimiento").ascending())
        ).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoteResponse> listarPorProducto(Long productoId, int page, int size) {
        Producto producto = obtenerProductoConPermiso(productoId);
        return loteRepository.findByProductoId(
                producto.getId(),
                PageRequest.of(page, size, Sort.by("fechaVencimiento").ascending())
        ).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoteResponse> listarActivosPorProducto(Long productoId) {
        Producto producto = obtenerProductoConPermiso(productoId);
        return loteRepository.findByProductoIdAndActivoTrueOrderByFechaVencimientoAsc(producto.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LoteResponse crear(LoteRequest request) {
        Producto producto = obtenerProductoConPermiso(request.getProductoId());
        String numeroLote = request.getNumeroLote().trim();

        if (loteRepository.existsByProductoIdAndNumeroLoteIgnoreCase(producto.getId(), numeroLote)) {
            throw new IllegalArgumentException("Ya existe un lote con el número \"" + numeroLote + "\" para este producto");
        }

        Lote lote = new Lote();
        lote.setCompany(producto.getCompany());
        lote.setProducto(producto);
        lote.setNumeroLote(numeroLote);
        lote.setFechaVencimiento(request.getFechaVencimiento());
        lote.setFechaIngreso(request.getFechaIngreso());
        lote.setCantidad(request.getCantidad());
        lote.setCostoUnitario(request.getCostoUnitario());
        lote.setActivo(true);
        lote.setCreatedBy(SecurityUtils.getCurrentUserEmail());
        lote.setUpdatedBy(SecurityUtils.getCurrentUserEmail());

        Lote guardado = loteRepository.save(lote);
        auditLogService.log(producto.getCompany().getId(), "CREAR_LOTE", "Inventario",
                "Se creó el lote " + guardado.getNumeroLote() + " del producto " + producto.getNombre());

        return toResponse(guardado);
    }

    @Override
    @Transactional
    public LoteResponse actualizar(Long id, LoteRequest request) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado con ID: " + id));
        validarPermisoSobreLote(lote);
        String numeroLote = request.getNumeroLote().trim();

        if (loteRepository.existsByProductoIdAndNumeroLoteIgnoreCaseAndIdNot(lote.getProducto().getId(), numeroLote, id)) {
            throw new IllegalArgumentException("Ya existe un lote con el número \"" + numeroLote + "\" para este producto");
        }

        lote.setNumeroLote(numeroLote);
        lote.setFechaVencimiento(request.getFechaVencimiento());
        lote.setFechaIngreso(request.getFechaIngreso());
        lote.setCantidad(request.getCantidad());
        lote.setCostoUnitario(request.getCostoUnitario());
        lote.setUpdatedBy(SecurityUtils.getCurrentUserEmail());

        Lote guardado = loteRepository.save(lote);
        auditLogService.log(guardado.getCompany().getId(), "ACTUALIZAR_LOTE", "Inventario",
                "Se actualizó el lote " + guardado.getNumeroLote() + " del producto " + guardado.getProducto().getNombre());

        return toResponse(guardado);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado con ID: " + id));
        validarPermisoSobreLote(lote);
        lote.setActivo(false);
        lote.setUpdatedBy(SecurityUtils.getCurrentUserEmail());
        loteRepository.save(lote);

        auditLogService.log(lote.getCompany().getId(), "DESACTIVAR_LOTE", "Inventario",
                "Se desactivó el lote " + lote.getNumeroLote() + " del producto " + lote.getProducto().getNombre());
    }

    @Override
    @Transactional
    public LoteResponse toggleActivo(Long id) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado con ID: " + id));
        validarPermisoSobreLote(lote);
        lote.setActivo(!Boolean.TRUE.equals(lote.getActivo()));
        lote.setUpdatedBy(SecurityUtils.getCurrentUserEmail());

        Lote guardado = loteRepository.save(lote);
        auditLogService.log(guardado.getCompany().getId(),
                Boolean.TRUE.equals(guardado.getActivo()) ? "ACTIVAR_LOTE" : "DESACTIVAR_LOTE",
                "Inventario",
                (Boolean.TRUE.equals(guardado.getActivo()) ? "Se activó" : "Se desactivó") + " el lote " + guardado.getNumeroLote() + " del producto " + guardado.getProducto().getNombre());

        return toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public AlertaVencimientoResponse alertasVencimiento(Integer companyId, int diasPorVencer) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(diasPorVencer);

        List<LoteResponse> vencidos = loteRepository
                .findByCompanyIdAndActivoTrueAndFechaVencimientoBeforeOrderByFechaVencimientoAsc(resolvedCompanyId, hoy)
                .stream().map(this::toResponse).collect(Collectors.toList());
        List<LoteResponse> porVencer = loteRepository
                .findByCompanyIdAndActivoTrueAndFechaVencimientoBetweenOrderByFechaVencimientoAsc(resolvedCompanyId, hoy, limite)
                .stream().map(this::toResponse).collect(Collectors.toList());

        AlertaVencimientoResponse response = new AlertaVencimientoResponse();
        response.setVencidosCount(vencidos.size());
        response.setPorVencerCount(porVencer.size());
        response.setVencidos(vencidos);
        response.setPorVencer(porVencer);
        return response;
    }

    private Producto obtenerProductoConPermiso(Long productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + productoId));
        if (!SecurityUtils.isSuperAdmin()) {
            Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
            if (producto.getCompany() == null || !producto.getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso sobre este producto");
            }
        }
        return producto;
    }

    private void validarPermisoSobreLote(Lote lote) {
        if (!SecurityUtils.isSuperAdmin()) {
            Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
            if (lote.getCompany() == null || !lote.getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para modificar este lote");
            }
        }
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

    private LoteResponse toResponse(Lote l) {
        LoteResponse r = new LoteResponse();
        r.setId(l.getId());
        r.setNumeroLote(l.getNumeroLote());
        r.setFechaVencimiento(l.getFechaVencimiento());
        r.setFechaIngreso(l.getFechaIngreso());
        r.setCantidad(l.getCantidad());
        r.setCostoUnitario(l.getCostoUnitario());
        r.setActivo(l.getActivo());
        r.setCreatedAt(l.getCreatedAt());
        r.setCreatedBy(l.getCreatedBy());
        r.setUpdatedAt(l.getUpdatedAt());
        r.setUpdatedBy(l.getUpdatedBy());
        if (l.getCompany() != null) {
            r.setCompanyId(l.getCompany().getId());
            r.setCompanyName(l.getCompany().getName());
        }
        if (l.getProducto() != null) {
            r.setProductoId(l.getProducto().getId());
            r.setProductoNombre(l.getProducto().getNombre());
            r.setProductoSku(l.getProducto().getSku());
        }
        return r;
    }
}
