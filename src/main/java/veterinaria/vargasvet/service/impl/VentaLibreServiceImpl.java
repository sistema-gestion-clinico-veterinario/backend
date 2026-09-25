package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Producto;
import veterinaria.vargasvet.domain.entity.Purchase;
import veterinaria.vargasvet.domain.entity.VentaLibreDetalle;
import veterinaria.vargasvet.domain.enums.MetodoPago;
import veterinaria.vargasvet.domain.enums.PaymentStatus;
import veterinaria.vargasvet.domain.enums.TipoPurchase;
import veterinaria.vargasvet.dto.request.VentaLibreItemRequest;
import veterinaria.vargasvet.dto.request.VentaLibreRequest;
import veterinaria.vargasvet.dto.response.VentaLibreDetalleResponse;
import veterinaria.vargasvet.dto.response.VentaLibreResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.ProductoRepository;
import veterinaria.vargasvet.repository.PurchaseRepository;
import veterinaria.vargasvet.repository.VentaLibreDetalleRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.VentaLibreService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VentaLibreServiceImpl implements VentaLibreService {

    private final PurchaseRepository purchaseRepository;
    private final VentaLibreDetalleRepository ventaLibreDetalleRepository;
    private final ProductoRepository productoRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public VentaLibreResponse registrar(VentaLibreRequest request) {
        Integer companyId = resolverCompanyId(request.getCompanyId());
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con ID: " + companyId));

        String clienteNombre = null;
        Apoderado apoderado = null;
        if (request.getApoderadoId() != null) {
            apoderado = apoderadoRepository.findByIdAndCompanyId(request.getApoderadoId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado en esta empresa"));
            clienteNombre = apoderado.getUser().getNombre() + " " + apoderado.getUser().getApellido();
        } else if (request.getClienteNombre() != null && !request.getClienteNombre().isBlank()) {
            clienteNombre = request.getClienteNombre().trim();
        }

        List<VentaLibreDetalle> detalles = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (VentaLibreItemRequest item : request.getItems()) {
            Producto producto = productoRepository.findById(item.getProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + item.getProductoId()));
            if (producto.getCompany() == null || !producto.getCompany().getId().equals(companyId)) {
                throw new IllegalArgumentException("El producto «" + producto.getNombre() + "» no pertenece a esta empresa");
            }
            if (!Boolean.TRUE.equals(producto.getActivo())) {
                throw new IllegalArgumentException("El producto «" + producto.getNombre() + "» no está disponible");
            }

            int filasActualizadas = productoRepository.descontarStock(producto.getId(), item.getCantidad());
            if (filasActualizadas == 0) {
                throw new IllegalArgumentException("Stock insuficiente para «" + producto.getNombre() + "»");
            }

            BigDecimal subtotal = producto.getPrecio()
                    .multiply(BigDecimal.valueOf(item.getCantidad()))
                    .setScale(2, RoundingMode.HALF_UP);
            total = total.add(subtotal);

            VentaLibreDetalle detalle = new VentaLibreDetalle();
            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(subtotal);
            detalles.add(detalle);
        }

        BigDecimal cambio = null;
        if (request.getMetodoPago() == MetodoPago.EFECTIVO) {
            if (request.getMontoRecibido().compareTo(total) < 0) {
                throw new IllegalArgumentException("El monto recibido no cubre el total de la venta");
            }
            cambio = request.getMontoRecibido().subtract(total);
        }

        Purchase purchase = new Purchase();
        purchase.setCompany(company);
        purchase.setCita(null);
        purchase.setTipoPurchase(TipoPurchase.TIENDA);
        purchase.setCliente(apoderado != null ? apoderado.getUser() : null);
        purchase.setClienteNombreWalkin(apoderado == null ? clienteNombre : null);
        purchase.setMetodoPago(request.getMetodoPago());
        purchase.setMontoRecibido(request.getMontoRecibido());
        purchase.setTotal(total);
        purchase.setPaymentStatus(PaymentStatus.PAID);
        purchase.setNumeroVenta(generarNumeroVenta());
        purchase.setCreatedAt(veterinaria.vargasvet.util.AppClock.now());
        purchase = purchaseRepository.save(purchase);

        for (VentaLibreDetalle detalle : detalles) {
            detalle.setPurchase(purchase);
            ventaLibreDetalleRepository.save(detalle);
        }

        auditLogService.log(
                companyId,
                "REGISTRAR_VENTA_LIBRE",
                "Facturación",
                "Se registró una venta directa de S/ " + total + (clienteNombre != null ? " a " + clienteNombre : " a un cliente de mostrador")
        );

        return toResponse(purchase, detalles, clienteNombre, cambio);
    }

    private String generarNumeroVenta() {
        String fecha = veterinaria.vargasvet.util.AppClock.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "VL-" + fecha + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private VentaLibreResponse toResponse(Purchase purchase, List<VentaLibreDetalle> detalles, String clienteNombre, BigDecimal cambio) {
        VentaLibreResponse response = new VentaLibreResponse();
        response.setId(purchase.getId());
        response.setNumeroVenta(purchase.getNumeroVenta());
        response.setClienteNombre(clienteNombre);
        response.setTotal(purchase.getTotal());
        response.setMetodoPago(purchase.getMetodoPago());
        response.setMontoRecibido(purchase.getMontoRecibido());
        response.setCambio(cambio);
        response.setFecha(purchase.getCreatedAt());
        response.setItems(detalles.stream().map(d -> {
            VentaLibreDetalleResponse item = new VentaLibreDetalleResponse();
            item.setProductoId(d.getProducto().getId());
            item.setProductoNombre(d.getProducto().getNombre());
            item.setCantidad(d.getCantidad());
            item.setPrecioUnitario(d.getPrecioUnitario());
            item.setSubtotal(d.getSubtotal());
            return item;
        }).toList());
        return response;
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
