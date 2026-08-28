package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Purchase;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.MetodoPago;
import veterinaria.vargasvet.domain.enums.PaymentStatus;
import veterinaria.vargasvet.domain.enums.TipoPurchase;
import veterinaria.vargasvet.dto.request.PagoRequest;
import veterinaria.vargasvet.dto.response.PagoResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.PurchaseRepository;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CajaService;
import veterinaria.vargasvet.service.PagoService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import veterinaria.vargasvet.dto.response.PagoListResponse;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SecurityUtils;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements PagoService {

    private final CitaRepository citaRepository;
    private final PurchaseRepository purchaseRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditLogService auditLogService;
    private final CajaService cajaService;

    @Override
    @Transactional
    public PagoResponse registrar(PagoRequest request) {
        Cita cita = findAccessibleCita(request.getCitaId(), true);

        if (cita.getEstado() == EstadoCita.CANCELADA || cita.getEstado() == EstadoCita.NO_ASISTIO) {
            throw new IllegalArgumentException("No se puede registrar un pago para una cita con estado: " + cita.getEstado());
        }

        BigDecimal total = cita.getTotalServicio() != null ? cita.getTotalServicio() : BigDecimal.ZERO;
        BigDecimal pagadoActual = cita.getMontoPagado() != null ? cita.getMontoPagado() : BigDecimal.ZERO;
        BigDecimal saldo = total.subtract(pagadoActual);
        if (saldo.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cuenta de la cita ya se encuentra pagada");
        }

        BigDecimal montoAplicado = request.getMonto() != null ? request.getMonto() : saldo;
        if (montoAplicado.compareTo(BigDecimal.ZERO) <= 0 || montoAplicado.compareTo(saldo) > 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero y no superar el saldo pendiente de S/ " + saldo);
        }

        BigDecimal montoRecibido = null;
        BigDecimal cambio = null;
        PaymentStatus estado = montoAplicado.compareTo(saldo) == 0 ? PaymentStatus.PAID : PaymentStatus.PENDING;
        String mercadoPagoId = null;
        String mpStatus = null;

        if (request.getMetodoPago() == MetodoPago.EFECTIVO) {
            if (request.getMontoRecibido() == null) {
                throw new IllegalArgumentException("El monto recibido es obligatorio para pagos en efectivo");
            }
            montoRecibido = request.getMontoRecibido();
            if (montoRecibido.compareTo(montoAplicado) < 0) {
                throw new IllegalArgumentException("El monto recibido no cubre el importe a aplicar");
            }
            if (montoRecibido.compareTo(new BigDecimal("10000.00")) > 0) {
                throw new IllegalArgumentException("El monto recibido no puede superar S/ 10,000.00");
            }
            cambio = montoRecibido.subtract(montoAplicado);
            if (cambio.compareTo(new BigDecimal("1000.00")) > 0) {
                throw new IllegalArgumentException("El vuelto no puede superar S/ 1,000.00; verifique el monto recibido");
            }
        }

        Purchase pago = new Purchase();
        pago.setCita(cita);
        pago.setMetodoPago(request.getMetodoPago());
        pago.setTotal(montoAplicado);
        pago.setMontoRecibido(montoRecibido);
        pago.setPaymentStatus(estado);
        pago.setTipoPurchase(TipoPurchase.SERVICIO_CITA);
        pago.setMercadoPagoId(mercadoPagoId);
        pago.setMpStatus(mpStatus);
        pago.setCreatedAt(veterinaria.vargasvet.util.AppClock.now());

        Purchase savedPago = purchaseRepository.save(pago);

        cita.setMontoPagado(pagadoActual.add(montoAplicado));
        citaRepository.save(cita);

        auditLogService.log(
            "REGISTRAR_PAGO",
            "Facturación",
            "Se registró un pago de S/ " + montoAplicado + " para la cita de la mascota " + cita.getMascota().getNombreCompleto() + " con método de pago: " + request.getMetodoPago()
        );

        Integer companyId = getCitaCompanyId(cita);
        cajaService.registrarIngresoPorCita(cita, companyId, montoAplicado, request.getMetodoPago());

        return toResponse(savedPago, cambio);
    }

    @Override
    @Transactional(readOnly = true)
    public PagoResponse obtenerPorCita(Long citaId) {
        findAccessibleCita(citaId, false);
        Purchase pago = purchaseRepository
                .findTopByCitaIdAndTipoPurchaseOrderByCreatedAtDesc(citaId, TipoPurchase.SERVICIO_CITA)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró un pago para la cita con ID: " + citaId));

        BigDecimal cambio = null;
        if (pago.getMontoRecibido() != null && pago.getTotal() != null
                && pago.getMontoRecibido().compareTo(pago.getTotal()) > 0) {
            cambio = pago.getMontoRecibido().subtract(pago.getTotal());
        }
        return toResponse(pago, cambio);
    }

    @Override
    @Transactional
    public Page<PagoListResponse> listarTodos(int page, int size, Integer companyId) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Integer resolvedCompanyId = SecurityUtils.isSuperAdmin()
                ? companyId
                : SecurityUtils.getCurrentCompanyId();

        if (resolvedCompanyId != null) {
            return purchaseRepository.findByCompanyId(resolvedCompanyId, TipoPurchase.SERVICIO_CITA, pageable)
                    .map(this::toListResponse);
        }

        return purchaseRepository.findAllByTipoPurchaseOrderByCreatedAtDesc(TipoPurchase.SERVICIO_CITA, pageable)
                .map(this::toListResponse);
    }

    @Override
    @Transactional
    public Page<PagoListResponse> listarHistorialPorEmpresa(int page, int size, Integer companyId) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Integer resolvedCompanyId = SecurityUtils.isSuperAdmin()
                ? companyId
                : SecurityUtils.getCurrentCompanyId();

        if (resolvedCompanyId != null) {
            return purchaseRepository.findByCompanyId(resolvedCompanyId, TipoPurchase.SERVICIO_CITA, pageable)
                    .map(this::toListResponse);
        }

        return purchaseRepository.findAllByTipoPurchaseOrderByCreatedAtDesc(TipoPurchase.SERVICIO_CITA, pageable)
                .map(this::toListResponse);
    }

    private PagoListResponse toListResponseFromCita(Cita cita) {
        PagoListResponse r = new PagoListResponse();
        r.setCitaId(cita.getId());
        r.setNumeroCita(asegurarNumeroCita(cita));
        r.setId(cita.getId());

        Purchase purchase = null;
        if (cita.getPagos() != null && !cita.getPagos().isEmpty()) {
            purchase = cita.getPagos().get(0);
        }

        if (purchase != null) {
            r.setMetodoPago(purchase.getMetodoPago());
            r.setMonto(purchase.getTotal());
            r.setMontoRecibido(purchase.getMontoRecibido());
            r.setFechaPago(purchase.getCreatedAt());
            r.setEstado(purchase.getPaymentStatus());

            BigDecimal cambio = null;
            if (purchase.getMontoRecibido() != null && purchase.getTotal() != null
                    && purchase.getMontoRecibido().compareTo(purchase.getTotal()) > 0) {
                cambio = purchase.getMontoRecibido().subtract(purchase.getTotal());
            }
            r.setCambio(cambio);
        } else {
            r.setMonto(cita.getTotalServicio());
            r.setEstado(computePaymentStatus(cita));
        }

        r.setEstadoCita(cita.getEstado() != null ? cita.getEstado().name() : null);

        if (cita.getMascota() != null) {
            r.setMascotaNombre(cita.getMascota().getNombreCompleto());
            if (cita.getMascota().getApoderado() != null
                    && cita.getMascota().getApoderado().getUser() != null) {
                var apUser = cita.getMascota().getApoderado().getUser();
                r.setClienteNombre(
                        (apUser.getNombre() != null ? apUser.getNombre() : "")
                        + " " + (apUser.getApellido() != null ? apUser.getApellido() : "")
                );
            }
        }
        if (cita.getServicio() != null) {
            r.setServicioNombre(cita.getServicio().getNombre());
        }
        if (cita.getEmpleado() != null && cita.getEmpleado().getUser() != null) {
            var vetUser = cita.getEmpleado().getUser();
            r.setVeterinarioNombre(
                    (vetUser.getNombre() != null ? vetUser.getNombre() : "")
                    + " " + (vetUser.getApellido() != null ? vetUser.getApellido() : "")
            );
        }
        return r;
    }

    private PaymentStatus computePaymentStatus(Cita cita) {
        BigDecimal total = cita.getTotalServicio();
        BigDecimal pagado = cita.getMontoPagado();
        if (total != null && pagado != null && pagado.compareTo(total) >= 0) {
            return PaymentStatus.PAID;
        }
        return PaymentStatus.PENDING;
    }

    @Override
    @Transactional
    public Page<PagoListResponse> listarMisPagos(int page, int size) {
        String email = SecurityUtils.getCurrentUserEmail();
        veterinaria.vargasvet.domain.entity.Usuario user = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email));
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return purchaseRepository.findByApoderadoUserId(user.getId(), TipoPurchase.SERVICIO_CITA, pageable)
                .map(this::toListResponse);
    }

    private PagoListResponse toListResponse(Purchase p) {
        PagoListResponse r = new PagoListResponse();
        r.setId(p.getId());
        r.setMetodoPago(p.getMetodoPago());
        r.setMonto(p.getTotal());
        r.setMontoRecibido(p.getMontoRecibido());
        r.setFechaPago(p.getCreatedAt());
        r.setEstado(p.getPaymentStatus());

        BigDecimal cambio = null;
        if (p.getMontoRecibido() != null && p.getTotal() != null
                && p.getMontoRecibido().compareTo(p.getTotal()) > 0) {
            cambio = p.getMontoRecibido().subtract(p.getTotal());
        }
        r.setCambio(cambio);

        if (p.getCita() != null) {
            r.setCitaId(p.getCita().getId());
            r.setNumeroCita(asegurarNumeroCita(p.getCita()));
            r.setEstadoCita(p.getCita().getEstado() != null ? p.getCita().getEstado().name() : null);
            if (p.getCita().getMascota() != null) {
                r.setMascotaNombre(p.getCita().getMascota().getNombreCompleto());
                if (p.getCita().getMascota().getApoderado() != null
                        && p.getCita().getMascota().getApoderado().getUser() != null) {
                    veterinaria.vargasvet.domain.entity.Usuario apUser = p.getCita().getMascota().getApoderado().getUser();
                    r.setClienteNombre(
                            (apUser.getNombre() != null ? apUser.getNombre() : "")
                            + " " + (apUser.getApellido() != null ? apUser.getApellido() : "")
                    );
                }
            }
            if (p.getCita().getServicio() != null) {
                r.setServicioNombre(p.getCita().getServicio().getNombre());
            }
            if (p.getCita().getEmpleado() != null && p.getCita().getEmpleado().getUser() != null) {
                veterinaria.vargasvet.domain.entity.Usuario vetUser = p.getCita().getEmpleado().getUser();
                r.setVeterinarioNombre(
                        (vetUser.getNombre() != null ? vetUser.getNombre() : "")
                        + " " + (vetUser.getApellido() != null ? vetUser.getApellido() : "")
                );
            }
        } else if (p.getUser() != null) {
            // Fallback for non-cita purchases (e.g., virtual store, future item types)
            r.setClienteNombre(
                    (p.getUser().getNombre() != null ? p.getUser().getNombre() : "")
                    + " " + (p.getUser().getApellido() != null ? p.getUser().getApellido() : "")
            );
        }
        return r;
    }

    private Integer getCitaCompanyId(Cita cita) {
        try {
            if (cita.getMascota() != null && cita.getMascota().getApoderado() != null
                    && cita.getMascota().getApoderado().getUser() != null
                    && cita.getMascota().getApoderado().getUser().getCompany() != null) {
                return cita.getMascota().getApoderado().getUser().getCompany().getId();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Cita findAccessibleCita(Long citaId, boolean forUpdate) {
        if (SecurityUtils.isSuperAdmin()) {
            return (forUpdate ? citaRepository.findByIdForUpdate(citaId) : citaRepository.findById(citaId))
                    .orElseThrow(() -> citaNotFound(citaId));
        }

        Integer companyId = SecurityUtils.getCurrentCompanyId();
        if (companyId == null) {
            throw new AccessDeniedException("El usuario no tiene una empresa asignada");
        }

        return (forUpdate
                ? citaRepository.findByIdAndCompanyIdForUpdate(citaId, companyId)
                : citaRepository.findByIdAndCompanyId(citaId, companyId))
                .orElseThrow(() -> citaNotFound(citaId));
    }

    private ResourceNotFoundException citaNotFound(Long citaId) {
        return new ResourceNotFoundException("Cita no encontrada con ID: " + citaId);
    }

    private String asegurarNumeroCita(Cita cita) {
        if (cita.getNumeroCita() == null || cita.getNumeroCita().isBlank()) {
            String fecha = cita.getFechaHoraInicio() != null
                    ? cita.getFechaHoraInicio().toLocalDate().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
                    : java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
            cita.setNumeroCita("CIT-" + fecha + "-" + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase());
            citaRepository.save(cita);
        }
        return cita.getNumeroCita();
    }

    private PagoResponse toResponse(Purchase pago, BigDecimal cambio) {
        PagoResponse response = new PagoResponse();
        response.setId(pago.getId());
        response.setCitaId(pago.getCita().getId());
        response.setNumeroCita(asegurarNumeroCita(pago.getCita()));
        response.setMetodoPago(pago.getMetodoPago());
        response.setMonto(pago.getTotal());
        response.setMontoRecibido(pago.getMontoRecibido());
        response.setCambio(cambio);
        BigDecimal totalCuenta = pago.getCita().getTotalServicio() != null ? pago.getCita().getTotalServicio() : BigDecimal.ZERO;
        BigDecimal totalPagado = pago.getCita().getMontoPagado() != null ? pago.getCita().getMontoPagado() : BigDecimal.ZERO;
        response.setSaldoPendiente(totalCuenta.subtract(totalPagado).max(BigDecimal.ZERO));
        response.setFechaPago(pago.getCreatedAt());
        response.setEstado(pago.getPaymentStatus());
        response.setMercadoPagoId(pago.getMercadoPagoId());
        response.setMpStatus(pago.getMpStatus());
        return response;
    }
}
