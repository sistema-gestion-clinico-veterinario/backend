package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.DetalleCuentaCita;
import veterinaria.vargasvet.domain.enums.TipoDetalleCuenta;
import veterinaria.vargasvet.dto.request.DetalleCuentaRequest;
import veterinaria.vargasvet.dto.response.CuentaCitaResponse;
import veterinaria.vargasvet.dto.response.DetalleCuentaResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.DetalleCuentaCitaRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.CuentaCitaService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CuentaCitaServiceImpl implements CuentaCitaService {

    private final CitaRepository citaRepository;
    private final DetalleCuentaCitaRepository detalleRepository;

    @Override
    @Transactional
    public Page<CuentaCitaResponse> listarPendientes(Integer companyId, int page, int size) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        return citaRepository.findCuentasPendientes(resolvedCompanyId, PageRequest.of(page, size))
                .map(cita -> {
                    asegurarNumeroCita(cita);
                    return map(cita, detalleRepository.findByCitaIdOrderByCreatedAtAscIdAsc(cita.getId()));
                });
    }

    @Override
    @Transactional
    public CuentaCitaResponse obtener(Long citaId) {
        Cita cita = obtenerCitaAutorizada(citaId);
        asegurarNumeroCita(cita);
        asegurarServicioBase(cita);
        return map(cita, detalleRepository.findByCitaIdOrderByCreatedAtAscIdAsc(citaId));
    }

    @Override
    @Transactional
    public CuentaCitaResponse agregarDetalle(Long citaId, DetalleCuentaRequest request) {
        Cita cita = obtenerCitaAutorizada(citaId);
        validarEditable(cita);
        asegurarServicioBase(cita);

        BigDecimal precio = request.getPrecioUnitario().setScale(2, RoundingMode.HALF_UP);
        if (request.getTipo() == TipoDetalleCuenta.DESCUENTO) {
            if (precio.compareTo(BigDecimal.ZERO) >= 0) precio = precio.negate();
        } else if (precio.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Solo los descuentos pueden tener importe negativo");
        }
        if (precio.abs().compareTo(new BigDecimal("50000.00")) > 0) {
            throw new IllegalArgumentException("El precio unitario supera el máximo permitido");
        }

        DetalleCuentaCita detalle = new DetalleCuentaCita();
        detalle.setCita(cita);
        detalle.setTipo(request.getTipo());
        detalle.setDescripcion(request.getDescripcion().trim());
        detalle.setCantidad(request.getCantidad());
        detalle.setPrecioUnitario(precio);
        detalle.setSubtotal(precio.multiply(BigDecimal.valueOf(request.getCantidad())).setScale(2, RoundingMode.HALF_UP));
        detalle.setEsServicioBase(false);
        detalle.setRegistradoPor(SecurityUtils.getCurrentUserEmail());
        detalleRepository.save(detalle);

        recalcularTotal(cita);
        return map(cita, detalleRepository.findByCitaIdOrderByCreatedAtAscIdAsc(citaId));
    }

    @Override
    @Transactional
    public CuentaCitaResponse eliminarDetalle(Long citaId, Long detalleId) {
        Cita cita = obtenerCitaAutorizada(citaId);
        validarEditable(cita);
        DetalleCuentaCita detalle = detalleRepository.findById(detalleId)
                .filter(d -> d.getCita().getId().equals(citaId))
                .orElseThrow(() -> new ResourceNotFoundException("Concepto de cuenta no encontrado"));
        if (Boolean.TRUE.equals(detalle.getEsServicioBase())) {
            throw new IllegalArgumentException("El servicio principal de la cita no se puede eliminar");
        }
        detalleRepository.delete(detalle);
        detalleRepository.flush();
        recalcularTotal(cita);
        return map(cita, detalleRepository.findByCitaIdOrderByCreatedAtAscIdAsc(citaId));
    }

    @Override
    @Transactional
    public CuentaCitaResponse actualizarDetalle(Long citaId, Long detalleId, DetalleCuentaRequest request) {
        Cita cita = obtenerCitaAutorizada(citaId);
        validarEditable(cita);
        DetalleCuentaCita detalle = detalleRepository.findById(detalleId)
                .filter(d -> d.getCita().getId().equals(citaId))
                .orElseThrow(() -> new ResourceNotFoundException("Concepto de cuenta no encontrado"));
        if (Boolean.TRUE.equals(detalle.getEsServicioBase())) {
            throw new IllegalArgumentException("El servicio principal se actualiza desde la cita");
        }

        BigDecimal precio = request.getPrecioUnitario().setScale(2, RoundingMode.HALF_UP);
        if (request.getTipo() == TipoDetalleCuenta.DESCUENTO) {
            if (precio.compareTo(BigDecimal.ZERO) >= 0) precio = precio.negate();
        } else if (precio.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Solo los descuentos pueden tener importe negativo");
        }
        detalle.setTipo(request.getTipo());
        detalle.setDescripcion(request.getDescripcion().trim());
        detalle.setCantidad(request.getCantidad());
        detalle.setPrecioUnitario(precio);
        detalle.setSubtotal(precio.multiply(BigDecimal.valueOf(request.getCantidad())).setScale(2, RoundingMode.HALF_UP));
        detalleRepository.save(detalle);
        recalcularTotal(cita);
        return map(cita, detalleRepository.findByCitaIdOrderByCreatedAtAscIdAsc(citaId));
    }

    private void asegurarServicioBase(Cita cita) {
        if (detalleRepository.existsByCitaIdAndEsServicioBaseTrue(cita.getId())) return;
        BigDecimal precio = cita.getTotalServicio() != null ? cita.getTotalServicio() : BigDecimal.ZERO;
        if (precio.compareTo(BigDecimal.ZERO) <= 0 && cita.getServicio() == null) return;

        DetalleCuentaCita detalle = new DetalleCuentaCita();
        detalle.setCita(cita);
        detalle.setTipo(TipoDetalleCuenta.SERVICIO);
        detalle.setDescripcion(cita.getServicio() != null ? cita.getServicio().getNombre() : "Servicio veterinario");
        detalle.setCantidad(1);
        detalle.setPrecioUnitario(precio.setScale(2, RoundingMode.HALF_UP));
        detalle.setSubtotal(precio.setScale(2, RoundingMode.HALF_UP));
        detalle.setEsServicioBase(true);
        detalle.setRegistradoPor("SISTEMA");
        detalleRepository.save(detalle);
    }

    private void recalcularTotal(Cita cita) {
        BigDecimal total = detalleRepository.findByCitaIdOrderByCreatedAtAscIdAsc(cita.getId()).stream()
                .map(DetalleCuentaCita::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal pagado = cita.getMontoPagado() != null ? cita.getMontoPagado() : BigDecimal.ZERO;
        if (total.compareTo(pagado) < 0) {
            throw new IllegalArgumentException("El total de la cuenta no puede ser menor al monto ya pagado");
        }
        cita.setTotalServicio(total);
        citaRepository.save(cita);
    }

    private void validarEditable(Cita cita) {
        switch (cita.getEstado()) {
            case CANCELADA, ELIMINADA, NO_ASISTIO ->
                    throw new IllegalArgumentException("No se puede modificar la cuenta de una cita " + cita.getEstado());
            default -> { }
        }
    }

    private Cita obtenerCitaAutorizada(Long citaId) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada: " + citaId));
        Integer citaCompanyId = getCompanyId(cita);
        Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
        if (!SecurityUtils.isSuperAdmin() && (currentCompanyId == null || !currentCompanyId.equals(citaCompanyId))) {
            throw new IllegalArgumentException("La cita no pertenece a la sede activa");
        }
        return cita;
    }

    private Integer resolverCompanyId(Integer companyId) {
        if (SecurityUtils.isSuperAdmin()) {
            if (companyId == null) throw new IllegalArgumentException("Debe seleccionar una sede");
            return companyId;
        }
        Integer current = SecurityUtils.getCurrentCompanyId();
        if (current == null) throw new IllegalArgumentException("No se pudo determinar la sede activa");
        return current;
    }

    private Integer getCompanyId(Cita cita) {
        return cita.getMascota().getApoderado().getUser().getCompany().getId();
    }

    private CuentaCitaResponse map(Cita cita, List<DetalleCuentaCita> detalles) {
        BigDecimal total = cita.getTotalServicio() != null ? cita.getTotalServicio() : BigDecimal.ZERO;
        BigDecimal pagado = cita.getMontoPagado() != null ? cita.getMontoPagado() : BigDecimal.ZERO;
        BigDecimal saldo = total.subtract(pagado).max(BigDecimal.ZERO);

        CuentaCitaResponse response = new CuentaCitaResponse();
        response.setCitaId(cita.getId());
        response.setNumeroCita(cita.getNumeroCita());
        response.setMascotaNombre(cita.getMascota().getNombreCompleto());
        String nombre = cita.getMascota().getApoderado().getUser().getNombre();
        String apellido = cita.getMascota().getApoderado().getUser().getApellido();
        response.setApoderadoNombre(((nombre != null ? nombre : "") + " " + (apellido != null ? apellido : "")).trim());
        response.setServicioNombre(cita.getServicio() != null ? cita.getServicio().getNombre() : cita.getMotivoCita());
        response.setFechaAtencion(cita.getFechaHoraInicio());
        response.setEstadoCita(cita.getEstado());
        response.setCompanyId(getCompanyId(cita));
        response.setTotal(total);
        response.setMontoPagado(pagado);
        response.setSaldoPendiente(saldo);
        response.setEstadoPago(pagado.signum() == 0 ? "PENDIENTE" : saldo.signum() == 0 ? "PAGADA" : "PAGO_PARCIAL");
        response.setDetalles(detalles.stream().map(this::mapDetalle).toList());
        return response;
    }

    private void asegurarNumeroCita(Cita cita) {
        if (cita.getNumeroCita() != null && !cita.getNumeroCita().isBlank()) return;
        String fecha = cita.getFechaHoraInicio().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        cita.setNumeroCita("CIT-" + fecha + "-" + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        citaRepository.save(cita);
    }

    private DetalleCuentaResponse mapDetalle(DetalleCuentaCita detalle) {
        DetalleCuentaResponse response = new DetalleCuentaResponse();
        response.setId(detalle.getId());
        response.setTipo(detalle.getTipo());
        response.setDescripcion(detalle.getDescripcion());
        response.setCantidad(detalle.getCantidad());
        response.setPrecioUnitario(detalle.getPrecioUnitario());
        response.setSubtotal(detalle.getSubtotal());
        response.setEsServicioBase(detalle.getEsServicioBase());
        return response;
    }
}
