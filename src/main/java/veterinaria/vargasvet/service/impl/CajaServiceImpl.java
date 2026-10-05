package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.MovimientoCaja;
import veterinaria.vargasvet.domain.enums.ConceptoMovimiento;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.PaymentStatus;
import veterinaria.vargasvet.domain.enums.TipoMovimiento;
import veterinaria.vargasvet.domain.enums.TipoPurchase;
import veterinaria.vargasvet.domain.enums.MetodoPago;
import veterinaria.vargasvet.domain.enums.EstadoSesionCaja;
import veterinaria.vargasvet.domain.entity.SesionCaja;
import veterinaria.vargasvet.dto.request.AperturaCajaRequest;
import veterinaria.vargasvet.dto.request.ArqueoCajaRequest;
import veterinaria.vargasvet.dto.response.SesionCajaResponse;
import veterinaria.vargasvet.repository.SesionCajaRepository;
import veterinaria.vargasvet.dto.request.MovimientoEgresoRequest;
import veterinaria.vargasvet.dto.response.MovimientoCajaResponse;
import veterinaria.vargasvet.dto.response.ResumenCajaResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.MovimientoCajaRepository;
import veterinaria.vargasvet.repository.PurchaseRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CajaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class CajaServiceImpl implements CajaService {

    private final MovimientoCajaRepository movimientoRepo;
    private final CitaRepository citaRepository;
    private final PurchaseRepository purchaseRepository;
    private final SesionCajaRepository sesionCajaRepository;
    private final AuditLogService auditLogService;
    private final veterinaria.vargasvet.repository.UsuarioRepository usuarioRepository;
    private final veterinaria.vargasvet.service.PuntoCobroService puntoCobroService;
    private final veterinaria.vargasvet.repository.CajaRepository cajaRepository;

    @Override
    @Transactional
    public void registrarIngresoPorCita(Cita cita, Integer companyId, BigDecimal monto, MetodoPago metodoPago) {
        if (companyId == null || monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        SesionCaja sesion = sesionDeEsteEquipo(companyId);
        MovimientoCaja m = new MovimientoCaja();
        m.setSesionCajaId(sesion.getId());
        m.setTipo(TipoMovimiento.INGRESO);
        m.setConcepto(ConceptoMovimiento.PAGO_CITA);
        m.setMonto(monto);
        m.setMetodoPago(metodoPago);
        m.setCitaId(cita.getId());
        m.setDescripcion("Pago " + cita.getNumeroCita() + " - " + cita.getMascota().getNombreCompleto());
        m.setRegistradoPor(SecurityUtils.getCurrentUserEmail());
        m.setCompanyId(companyId);
        movimientoRepo.save(m);

        auditLogService.log(companyId, "REGISTRAR_INGRESO_CAJA", "Caja",
            "Se registró un ingreso de S/ " + monto + " (" + metodoPago + ") por la cita "
                + cita.getNumeroCita() + " de la mascota " + cita.getMascota().getNombreCompleto());
    }

    @Override
    @Transactional
    public void registrarIngresoPorVentaLibre(Integer companyId, BigDecimal monto, MetodoPago metodoPago, String descripcion) {
        if (companyId == null || monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        SesionCaja sesion = sesionDeEsteEquipo(companyId);
        MovimientoCaja m = new MovimientoCaja();
        m.setSesionCajaId(sesion.getId());
        m.setTipo(TipoMovimiento.INGRESO);
        m.setConcepto(ConceptoMovimiento.VENTA_PRODUCTO);
        m.setMonto(monto);
        m.setMetodoPago(metodoPago);
        m.setCitaId(null);
        m.setDescripcion(descripcion);
        m.setRegistradoPor(SecurityUtils.getCurrentUserEmail());
        m.setCompanyId(companyId);
        movimientoRepo.save(m);

        auditLogService.log(companyId, "REGISTRAR_INGRESO_CAJA", "Caja",
            "Se registró un ingreso de S/ " + monto + " (" + metodoPago + ") por venta de productos: " + descripcion);
    }

    @Override
    @Transactional
    public MovimientoCajaResponse registrarDevolucion(Long citaId) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada: " + citaId));

        if (cita.getEstado() != EstadoCita.CANCELADA) {
            throw new IllegalArgumentException("Solo se puede registrar devolución para citas canceladas");
        }
        if (cita.getMontoPagado() == null || cita.getMontoPagado().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cita no tiene monto pagado para devolver");
        }
        if (movimientoRepo.existsByCitaIdAndTipo(citaId, TipoMovimiento.DEVOLUCION)) {
            throw new IllegalArgumentException("Ya se registró una devolución para esta cita");
        }

        Integer companyId = getCitaCompanyId(cita);
        if (companyId == null) {
            throw new IllegalArgumentException("No se pudo determinar la empresa de la cita");
        }
        // La cita se busca por ID global sin filtro de empresa (arriba) - sin esto, cualquier
        // usuario podria pasar el citaId de OTRA empresa y mutar su caja/pagos.
        validarCompanyId(companyId);
        SesionCaja sesion = sesionDeEsteEquipo(companyId);

        BigDecimal montoDevuelto = cita.getMontoPagado();

        MovimientoCaja m = new MovimientoCaja();
        m.setSesionCajaId(sesion.getId());
        m.setTipo(TipoMovimiento.DEVOLUCION);
        m.setConcepto(ConceptoMovimiento.CANCELACION_DEVOLUCION);
        m.setMonto(montoDevuelto);
        purchaseRepository.findTopByCitaIdAndTipoPurchaseOrderByCreatedAtDesc(citaId, TipoPurchase.SERVICIO_CITA)
                .map(p -> p.getMetodoPago())
                .ifPresent(m::setMetodoPago);
        m.setCitaId(citaId);
        m.setDescripcion("Devolución " + cita.getNumeroCita() + " - " + cita.getMascota().getNombreCompleto());
        m.setRegistradoPor(SecurityUtils.getCurrentUserEmail());
        m.setCompanyId(companyId);
        MovimientoCaja saved = movimientoRepo.save(m);

        cita.setMontoPagado(BigDecimal.ZERO);
        citaRepository.save(cita);

        purchaseRepository.findByCitaIdAndTipoPurchaseAndPaymentStatusNot(
                        citaId, TipoPurchase.SERVICIO_CITA, PaymentStatus.REFUNDED)
                .forEach(p -> p.setPaymentStatus(PaymentStatus.REFUNDED));

        auditLogService.log(companyId, "REGISTRAR_DEVOLUCION_CAJA", "Caja",
            "Se registró una devolución de S/ " + montoDevuelto + " por la cita "
                + cita.getNumeroCita() + " de la mascota " + cita.getMascota().getNombreCompleto());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public MovimientoCajaResponse registrarEgreso(MovimientoEgresoRequest request) {
        validarCompanyId(request.getCompanyId());
        SesionCaja sesion = sesionDeEsteEquipo(request.getCompanyId());
        MovimientoCaja m = new MovimientoCaja();
        m.setSesionCajaId(sesion.getId());
        m.setTipo(TipoMovimiento.EGRESO);
        m.setConcepto(request.getConcepto() != null ? request.getConcepto() : ConceptoMovimiento.GASTO_OPERATIVO);
        m.setMonto(request.getMonto());
        m.setMetodoPago(MetodoPago.EFECTIVO);
        m.setDescripcion(request.getDescripcion());
        m.setRegistradoPor(SecurityUtils.getCurrentUserEmail());
        m.setCompanyId(request.getCompanyId());
        MovimientoCajaResponse response = toResponse(movimientoRepo.save(m));

        auditLogService.log(request.getCompanyId(), "REGISTRAR_EGRESO_CAJA", "Caja",
            "Se registró un egreso de S/ " + request.getMonto() + " (" + m.getConcepto() + "): "
                + request.getDescripcion());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenCajaResponse getResumen(Integer companyId, LocalDate desde, LocalDate hasta) {
        validarCompanyId(companyId);
        LocalDateTime ini = (desde != null ? desde : LocalDate.now().withDayOfMonth(1)).atStartOfDay();
        LocalDateTime fin = (hasta != null ? hasta : LocalDate.now()).atTime(LocalTime.MAX);

        BigDecimal ingresos    = movimientoRepo.sumByTipo(companyId, TipoMovimiento.INGRESO, ini, fin);
        BigDecimal egresos     = movimientoRepo.sumByTipo(companyId, TipoMovimiento.EGRESO, ini, fin);
        BigDecimal devoluciones = movimientoRepo.sumByTipo(companyId, TipoMovimiento.DEVOLUCION, ini, fin);
        BigDecimal ingresosCitas = movimientoRepo.sumByTipoAndConcepto(
                companyId, TipoMovimiento.INGRESO, ConceptoMovimiento.PAGO_CITA, ini, fin);
        BigDecimal ingresosProductos = movimientoRepo.sumByTipoAndConcepto(
                companyId, TipoMovimiento.INGRESO, ConceptoMovimiento.VENTA_PRODUCTO, ini, fin);

        ResumenCajaResponse r = new ResumenCajaResponse();
        r.setTotalIngresos(ingresos);
        r.setTotalEgresos(egresos);
        r.setTotalDevoluciones(devoluciones);
        r.setIngresosCitas(ingresosCitas);
        r.setIngresosProductos(ingresosProductos);
        r.setSaldo(ingresos.subtract(egresos).subtract(devoluciones));
        return r;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MovimientoCajaResponse> listar(Integer companyId, LocalDate desde, LocalDate hasta, int page, int size) {
        validarCompanyId(companyId);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
        java.util.Map<String, String> nombres = new java.util.HashMap<>();
        if (desde != null && hasta != null) {
            return movimientoRepo.findByCompanyIdAndFechaBetweenOrderByFechaDesc(
                    companyId,
                    desde.atStartOfDay(),
                    hasta.atTime(LocalTime.MAX),
                    pageable)
                    .map(movimiento -> toResponse(movimiento, nombres));
        }
        return movimientoRepo.findByCompanyIdOrderByFechaDesc(companyId, pageable)
                .map(movimiento -> toResponse(movimiento, nombres));
    }

    @Override
    @Transactional
    public SesionCajaResponse obtenerSesionActual(Integer companyId) {
        validarCompanyId(companyId);
        veterinaria.vargasvet.service.PuntoCobroService.Resolucion resolucion = puntoCobroService.resolver(companyId);
        if (resolucion.caja() == null) {
            return null;
        }
        return sesionCajaRepository.findFirstByCajaIdAndEstado(resolucion.caja().getId(), EstadoSesionCaja.ABIERTA)
                .map(this::actualizarCalculo)
                .map(this::toSesionResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SesionCajaResponse> listarSesiones(Integer companyId, int page, int size) {
        validarCompanyId(companyId);
        int safePage = Math.max(0, page);
        int safeSize = Math.min(50, Math.max(1, size));
        java.util.Map<String, String> nombres = new java.util.HashMap<>();
        return sesionCajaRepository.findByCompanyIdOrderByAbiertaAtDesc(
                companyId, PageRequest.of(safePage, safeSize)).map(sesion -> toSesionResponse(sesion, nombres));
    }

    @Override
    @Transactional
    public SesionCajaResponse abrirCaja(AperturaCajaRequest request) {
        validarCompanyId(request.getCompanyId());
        veterinaria.vargasvet.domain.entity.Caja caja = puntoCobroService.resolverParaOperar(request.getCompanyId());
        if (sesionCajaRepository.findFirstByCajaIdAndEstado(caja.getId(), EstadoSesionCaja.ABIERTA).isPresent()) {
            throw new IllegalArgumentException("La caja de «" + caja.getNombre() + "» ya se encuentra abierta");
        }
        Integer usuarioId = SecurityUtils.getCurrentUserId();
        sesionCajaRepository.findFirstByAbiertaPorUsuarioIdAndEstado(usuarioId, EstadoSesionCaja.ABIERTA)
                .ifPresent(otra -> {
                    throw new IllegalArgumentException("Ya tienes una caja abierta"
                            + nombreDeLaCaja(otra) + ". Ciérrala antes de abrir otra.");
                });
        SesionCaja sesion = new SesionCaja();
        sesion.setCompanyId(request.getCompanyId());
        sesion.setCajaId(caja.getId());
        sesion.setEstado(EstadoSesionCaja.ABIERTA);
        sesion.setMontoApertura(request.getMontoApertura());
        sesion.setEfectivoEsperado(request.getMontoApertura());
        sesion.setAbiertaAt(veterinaria.vargasvet.util.AppClock.now());
        sesion.setAbiertaPor(SecurityUtils.getCurrentUserEmail());
        sesion.setAbiertaPorUsuarioId(usuarioId);
        SesionCaja guardada;
        try {
            guardada = sesionCajaRepository.saveAndFlush(sesion);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            String causa = String.valueOf(e.getMostSpecificCause().getMessage());
            throw new IllegalArgumentException(causa.contains("uq_sesion_caja_abierta_por_persona")
                    ? "Ya tienes una caja abierta. Ciérrala antes de abrir otra."
                    : "La caja de «" + caja.getNombre() + "» ya se encuentra abierta");
        }
        SesionCajaResponse response = toSesionResponse(guardada);

        auditLogService.log(request.getCompanyId(), "ABRIR_CAJA", "Caja",
            "Se abrió la caja de «" + caja.getNombre() + "» con un monto de apertura de S/ " + request.getMontoApertura());

        return response;
    }

    @Override
    @Transactional
    public SesionCajaResponse arquearCaja(ArqueoCajaRequest request) {
        validarCompanyId(request.getCompanyId());
        SesionCaja sesion = sesionParaOperar(request);
        actualizarCalculo(sesion);
        sesion.setEfectivoContado(request.getEfectivoContado());
        sesion.setDiferencia(request.getEfectivoContado().subtract(sesion.getEfectivoEsperado()));
        sesion.setObservaciones(request.getObservaciones());
        return toSesionResponse(sesionCajaRepository.save(sesion));
    }

    @Override
    @Transactional
    public SesionCajaResponse cerrarCaja(ArqueoCajaRequest request) {
        validarCompanyId(request.getCompanyId());
        SesionCaja sesion = sesionParaOperar(request);
        actualizarCalculo(sesion);
        sesion.setEfectivoContado(request.getEfectivoContado());
        sesion.setDiferencia(request.getEfectivoContado().subtract(sesion.getEfectivoEsperado()));
        sesion.setObservaciones(request.getObservaciones());
        sesion.setEstado(EstadoSesionCaja.CERRADA);
        sesion.setCerradaAt(veterinaria.vargasvet.util.AppClock.now());
        sesion.setCerradaPor(SecurityUtils.getCurrentUserEmail());
        sesion.setCerradaPorUsuarioId(SecurityUtils.getCurrentUserId());
        SesionCajaResponse response = toSesionResponse(sesionCajaRepository.save(sesion));

        auditLogService.log(request.getCompanyId(), "CERRAR_CAJA", "Caja",
            "Se cerró la caja. Efectivo esperado: S/ " + sesion.getEfectivoEsperado()
                + ", efectivo contado: S/ " + sesion.getEfectivoContado()
                + ", diferencia: S/ " + sesion.getDiferencia());

        return response;
    }

    private SesionCaja sesionDeEsteEquipo(Integer companyId) {
        veterinaria.vargasvet.domain.entity.Caja caja = puntoCobroService.resolverParaOperar(companyId);
        return sesionCajaRepository.findFirstByCajaIdAndEstado(caja.getId(), EstadoSesionCaja.ABIERTA)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Debe abrir la caja de «" + caja.getNombre() + "» antes de registrar movimientos"));
    }

    private SesionCaja sesionParaOperar(ArqueoCajaRequest request) {
        if (request.getSesionId() == null) {
            return sesionDeEsteEquipo(request.getCompanyId());
        }
        if (!SecurityUtils.isAdmin() && !SecurityUtils.isSuperAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Solo un administrador puede cerrar la caja de otra persona");
        }
        return sesionCajaRepository.findByIdAndCompanyId(request.getSesionId(), request.getCompanyId())
                .filter(sesion -> sesion.getEstado() == EstadoSesionCaja.ABIERTA)
                .orElseThrow(() -> new IllegalArgumentException("Esa caja ya no está abierta"));
    }

    private String nombreDeLaCaja(SesionCaja sesion) {
        return sesion.getCajaId() == null ? "" : cajaRepository.findById(sesion.getCajaId())
                .map(caja -> " en «" + caja.getNombre() + "»").orElse("");
    }

    private SesionCaja actualizarCalculo(SesionCaja sesion) {
        BigDecimal ingresos = movimientoRepo.sumBySesionAndTipoAndMetodo(sesion.getId(), TipoMovimiento.INGRESO, MetodoPago.EFECTIVO);
        BigDecimal egresos = movimientoRepo.sumBySesionAndTipoAndMetodo(sesion.getId(), TipoMovimiento.EGRESO, MetodoPago.EFECTIVO);
        BigDecimal devoluciones = movimientoRepo.sumBySesionAndTipoAndMetodo(sesion.getId(), TipoMovimiento.DEVOLUCION, MetodoPago.EFECTIVO);
        sesion.setEfectivoEsperado(sesion.getMontoApertura().add(ingresos).subtract(egresos).subtract(devoluciones));
        if (sesion.getEfectivoContado() != null) {
            sesion.setDiferencia(sesion.getEfectivoContado().subtract(sesion.getEfectivoEsperado()));
        }
        return sesion;
    }

    private void validarCompanyId(Integer companyId) {
        if (companyId == null) throw new IllegalArgumentException("Debe seleccionar una sede");
        if (!SecurityUtils.isSuperAdmin()) {
            Integer current = SecurityUtils.getCurrentCompanyId();
            if (current == null || !current.equals(companyId)) {
                throw new IllegalArgumentException("No puede operar la caja de otra sede");
            }
        }
    }

    private SesionCajaResponse toSesionResponse(SesionCaja sesion) {
        return toSesionResponse(sesion, new java.util.HashMap<>());
    }

    private SesionCajaResponse toSesionResponse(SesionCaja sesion, java.util.Map<String, String> nombres) {
        SesionCajaResponse r = new SesionCajaResponse();
        r.setId(sesion.getId());
        r.setCompanyId(sesion.getCompanyId());
        r.setEstado(sesion.getEstado());
        r.setMontoApertura(sesion.getMontoApertura());
        r.setEfectivoEsperado(sesion.getEfectivoEsperado());
        r.setEfectivoContado(sesion.getEfectivoContado());
        r.setDiferencia(sesion.getDiferencia());
        r.setAbiertaAt(sesion.getAbiertaAt());
        r.setCerradaAt(sesion.getCerradaAt());
        r.setAbiertaPor(sesion.getAbiertaPor());
        r.setAbiertaPorNombre(nombreDe(sesion.getAbiertaPorUsuarioId(), sesion.getAbiertaPor(), nombres));
        r.setCerradaPor(sesion.getCerradaPor());
        r.setCerradaPorNombre(nombreDe(sesion.getCerradaPorUsuarioId(), sesion.getCerradaPor(), nombres));
        r.setObservaciones(sesion.getObservaciones());
        r.setCajaNombre(sesion.getCajaId() == null ? null : nombres.computeIfAbsent("caja:" + sesion.getCajaId(),
                k -> cajaRepository.findById(sesion.getCajaId()).map(veterinaria.vargasvet.domain.entity.Caja::getNombre).orElse("")));
        if (r.getCajaNombre() != null && r.getCajaNombre().isEmpty()) r.setCajaNombre(null);
        return r;
    }

    private String nombreDe(Integer usuarioId, String correo, java.util.Map<String, String> nombres) {
        return nombreDe(usuarioId, correo, null, nombres);
    }

    private String nombreDe(Integer usuarioId, String correo, Integer companyId, java.util.Map<String, String> nombres) {
        if (usuarioId == null && (correo == null || correo.isBlank())) return null;
        String clave = usuarioId != null ? "id:" + usuarioId
                : "correo:" + correo.toLowerCase(java.util.Locale.ROOT) + ":" + companyId;
        String nombre = nombres.computeIfAbsent(clave, k -> {
            veterinaria.vargasvet.domain.entity.Usuario usuario = usuarioId != null
                    ? usuarioRepository.findById(usuarioId).orElse(null)
                    : usuarioRepository.findAllByEmailIgnoreCase(correo).stream()
                            .filter(u -> companyId == null || u.getCompany() == null || companyId.equals(u.getCompany().getId()))
                            .findFirst().orElse(null);
            if (usuario == null) return "";
            return ((usuario.getNombre() == null ? "" : usuario.getNombre()) + " "
                    + (usuario.getApellido() == null ? "" : usuario.getApellido())).trim();
        });
        return nombre.isEmpty() ? null : nombre;
    }

    private Integer getCitaCompanyId(Cita cita) {
        if (cita.getMascota() != null && cita.getMascota().getApoderado() != null
                && cita.getMascota().getApoderado().getUser() != null
                && cita.getMascota().getApoderado().getCompany() != null) {
            return cita.getMascota().getApoderado().getCompany().getId();
        }
        return null;
    }

    private MovimientoCajaResponse toResponse(MovimientoCaja m) {
        return toResponse(m, new java.util.HashMap<>());
    }

    private String puntoDeLaSesion(Long sesionId, java.util.Map<String, String> nombres) {
        if (sesionId == null) return null;
        String nombre = nombres.computeIfAbsent("sesion:" + sesionId, k -> sesionCajaRepository.findById(sesionId)
                .map(SesionCaja::getCajaId)
                .flatMap(cajaRepository::findById)
                .map(veterinaria.vargasvet.domain.entity.Caja::getNombre)
                .orElse(""));
        return nombre.isEmpty() ? null : nombre;
    }

    private MovimientoCajaResponse toResponse(MovimientoCaja m, java.util.Map<String, String> nombres) {
        MovimientoCajaResponse r = new MovimientoCajaResponse();
        r.setId(m.getId());
        r.setTipo(m.getTipo());
        r.setConcepto(m.getConcepto());
        r.setMonto(m.getMonto());
        r.setMetodoPago(m.getMetodoPago());
        r.setCitaId(m.getCitaId());
        r.setDescripcion(m.getDescripcion());
        r.setFecha(m.getFecha());
        r.setRegistradoPor(m.getRegistradoPor());
        r.setRegistradoPorNombre(nombreDe(null, m.getRegistradoPor(), m.getCompanyId(), nombres));
        r.setPuntoCobro(puntoDeLaSesion(m.getSesionCajaId(), nombres));
        r.setCompanyId(m.getCompanyId());
        return r;
    }
}
