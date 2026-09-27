package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.AjusteStock;
import veterinaria.vargasvet.domain.entity.Producto;
import veterinaria.vargasvet.dto.request.AjusteStockRequest;
import veterinaria.vargasvet.dto.response.AjusteStockResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.AjusteStockRepository;
import veterinaria.vargasvet.repository.ProductoRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AjusteStockService;
import veterinaria.vargasvet.service.AuditLogService;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AjusteStockServiceImpl implements AjusteStockService {

    private final AjusteStockRepository ajusteStockRepository;
    private final ProductoRepository productoRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public AjusteStockResponse ajustar(AjusteStockRequest request) {
        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + request.getProductoId()));
        validarPermisoSobreProducto(producto);

        int stockAnterior = producto.getStock() != null ? producto.getStock() : 0;
        int stockNuevo = request.getStockNuevo();
        int diferencia = stockNuevo - stockAnterior;

        producto.setStock(stockNuevo);
        producto.setUpdatedBy(SecurityUtils.getCurrentUserEmail());
        productoRepository.save(producto);

        AjusteStock ajuste = new AjusteStock();
        ajuste.setCompany(producto.getCompany());
        ajuste.setProducto(producto);
        ajuste.setStockAnterior(stockAnterior);
        ajuste.setStockNuevo(stockNuevo);
        ajuste.setDiferencia(diferencia);
        ajuste.setMotivo(request.getMotivo());
        ajuste.setObservaciones(request.getObservaciones() != null && !request.getObservaciones().trim().isEmpty()
                ? request.getObservaciones().trim() : null);
        ajuste.setCreatedBy(SecurityUtils.getCurrentUserEmail());

        AjusteStock guardado = ajusteStockRepository.save(ajuste);

        auditLogService.log(producto.getCompany().getId(), "AJUSTAR_STOCK_PRODUCTO", "Inventario",
                "Se ajustó el stock de " + producto.getNombre() + " (" + producto.getSku() + ") de "
                        + stockAnterior + " a " + stockNuevo + " (" + (diferencia >= 0 ? "+" : "") + diferencia
                        + "). Motivo: " + motivoLabel(request.getMotivo())
                        + (ajuste.getObservaciones() != null ? ". " + ajuste.getObservaciones() : ""));

        return toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AjusteStockResponse> listarPorProducto(Long productoId, int page, int size) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + productoId));
        validarPermisoSobreProducto(producto);
        return ajusteStockRepository.findByProductoIdOrderByCreatedAtDesc(productoId, PageRequest.of(page, size, Sort.unsorted()))
                .map(this::toResponse);
    }

    private String motivoLabel(veterinaria.vargasvet.domain.enums.MotivoAjusteStock motivo) {
        return switch (motivo) {
            case CONTEO_FISICO -> "Conteo físico";
            case MERMA -> "Merma";
            case VENCIMIENTO -> "Vencimiento";
            case DEVOLUCION -> "Devolución";
            case CORRECCION -> "Corrección";
            case OTRO -> "Otro";
        };
    }

    private void validarPermisoSobreProducto(Producto producto) {
        if (!SecurityUtils.isSuperAdmin()) {
            Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
            if (producto.getCompany() == null || !producto.getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso sobre este producto");
            }
        }
    }

    private AjusteStockResponse toResponse(AjusteStock a) {
        AjusteStockResponse r = new AjusteStockResponse();
        r.setId(a.getId());
        r.setStockAnterior(a.getStockAnterior());
        r.setStockNuevo(a.getStockNuevo());
        r.setDiferencia(a.getDiferencia());
        r.setMotivo(a.getMotivo());
        r.setObservaciones(a.getObservaciones());
        r.setCreatedAt(a.getCreatedAt());
        r.setCreatedBy(a.getCreatedBy());
        if (a.getProducto() != null) {
            r.setProductoId(a.getProducto().getId());
            r.setProductoNombre(a.getProducto().getNombre());
            r.setProductoSku(a.getProducto().getSku());
        }
        return r;
    }
}
