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

    @Override
    @Transactional
    public void registrarIngresoPorCita(Cita cita, Integer companyId, BigDecimal monto, MetodoPago metodoPago) {
        if (companyId == null || monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        requireSesionAbierta(companyId);
        MovimientoCaja m = new MovimientoCaja();
        m.setTipo(TipoMovimiento.INGRESO);
        m.setConcepto(ConceptoMovimiento.PAGO_CITA);
        m.setMonto(monto);
        m.setMetodoPago(metodoPago);
        m.setCitaId(cita.getId());
        m.setDescripcion("Pago " + cita.getNumeroCita() + " - " + cita.getMascota().getNombreCompleto());
        m.setRegistradoPor(SecurityUtils.getCurrentUserEmail());
        m.setCompanyId(companyId);
        movimientoRepo.save(m);
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
        requireSesionAbierta(companyId);

        BigDecimal montoDevuelto = cita.getMontoPagado();

        MovimientoCaja m = new MovimientoCaja();
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

        return toResponse(saved);
    }

    @Override
    @Transactional
    public MovimientoCajaResponse registrarEgreso(MovimientoEgresoRequest request) {
        validarCompanyId(request.getCompanyId());
        requireSesionAbierta(request.getCompanyId());
        MovimientoCaja m = new MovimientoCaja();
        m.setTipo(TipoMovimiento.EGRESO);
        m.setConcepto(request.getConcepto() != null ? request.getConcepto() : ConceptoMovimiento.GASTO_OPERATIVO);
        m.setMonto(request.getMonto());
        m.setMetodoPago(MetodoPago.EFECTIVO);
        m.setDescripcion(request.getDescripcion());
        m.setRegistradoPor(SecurityUtils.getCurrentUserEmail());
        m.setCompanyId(request.getCompanyId());
        return toResponse(movimientoRepo.save(m));
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

        ResumenCajaResponse r = new ResumenCajaResponse();
        r.setTotalIngresos(ingresos);
        r.setTotalEgresos(egresos);
        r.setTotalDevoluciones(devoluciones);
        r.setSaldo(ingresos.subtract(egresos).subtract(devoluciones));
        return r;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MovimientoCajaResponse> listar(Integer companyId, LocalDate desde, LocalDate hasta, int page, int size) {
        validarCompanyId(companyId);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
        if (desde != null && hasta != null) {
            return movimientoRepo.findByCompanyIdAndFechaBetweenOrderByFechaDesc(
                    companyId,
                    desde.atStartOfDay(),
                    hasta.atTime(LocalTime.MAX),
                    pageable)
                    .map(this::toResponse);
        }
        return movimientoRepo.findByCompanyIdOrderByFechaDesc(companyId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SesionCajaResponse obtenerSesionActual(Integer companyId) {
        validarCompanyId(companyId);
        return sesionCajaRepository.findFirstByCompanyIdAndEstadoOrderByAbiertaAtDesc(companyId, EstadoSesionCaja.ABIERTA)
                .map(this::actualizarCalculo)
                .map(this::toSesionResponse)
                .orElse(null);
    }

    @Override
    @Transactional
    public SesionCajaResponse abrirCaja(AperturaCajaRequest request) {
        validarCompanyId(request.getCompanyId());
        if (sesionCajaRepository.findFirstByCompanyIdAndEstadoOrderByAbiertaAtDesc(
                request.getCompanyId(), EstadoSesionCaja.ABIERTA).isPresent()) {
            throw new IllegalArgumentException("La caja de esta sede ya se encuentra abierta");
        }
        SesionCaja sesion = new SesionCaja();
        sesion.setCompanyId(request.getCompanyId());
        sesion.setEstado(EstadoSesionCaja.ABIERTA);
        sesion.setMontoApertura(request.getMontoApertura());
        sesion.setEfectivoEsperado(request.getMontoApertura());
        sesion.setAbiertaAt(veterinaria.vargasvet.util.AppClock.now());
        sesion.setAbiertaPor(SecurityUtils.getCurrentUserEmail());
        return toSesionResponse(sesionCajaRepository.save(sesion));
    }

    @Override
    @Transactional
    public SesionCajaResponse arquearCaja(ArqueoCajaRequest request) {
        validarCompanyId(request.getCompanyId());
        SesionCaja sesion = requireSesionAbierta(request.getCompanyId());
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
        SesionCaja sesion = requireSesionAbierta(request.getCompanyId());
        actualizarCalculo(sesion);
        sesion.setEfectivoContado(request.getEfectivoContado());
        sesion.setDiferencia(request.getEfectivoContado().subtract(sesion.getEfectivoEsperado()));
        sesion.setObservaciones(request.getObservaciones());
        sesion.setEstado(EstadoSesionCaja.CERRADA);
        sesion.setCerradaAt(veterinaria.vargasvet.util.AppClock.now());
        sesion.setCerradaPor(SecurityUtils.getCurrentUserEmail());
        return toSesionResponse(sesionCajaRepository.save(sesion));
    }

    private SesionCaja requireSesionAbierta(Integer companyId) {
        return sesionCajaRepository.findFirstByCompanyIdAndEstadoOrderByAbiertaAtDesc(companyId, EstadoSesionCaja.ABIERTA)
                .orElseThrow(() -> new IllegalArgumentException("Debe abrir la caja de la sede antes de registrar movimientos"));
    }

    private SesionCaja actualizarCalculo(SesionCaja sesion) {
        LocalDateTime hasta = veterinaria.vargasvet.util.AppClock.now();
        BigDecimal ingresos = movimientoRepo.sumByTipoAndMetodo(
                sesion.getCompanyId(), TipoMovimiento.INGRESO, MetodoPago.EFECTIVO, sesion.getAbiertaAt(), hasta);
        BigDecimal egresos = movimientoRepo.sumByTipoAndMetodo(
                sesion.getCompanyId(), TipoMovimiento.EGRESO, MetodoPago.EFECTIVO, sesion.getAbiertaAt(), hasta);
        BigDecimal devoluciones = movimientoRepo.sumByTipoAndMetodo(
                sesion.getCompanyId(), TipoMovimiento.DEVOLUCION, MetodoPago.EFECTIVO, sesion.getAbiertaAt(), hasta);
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
        r.setCerradaPor(sesion.getCerradaPor());
        r.setObservaciones(sesion.getObservaciones());
        return r;
    }

    private Integer getCitaCompanyId(Cita cita) {
        if (cita.getMascota() != null && cita.getMascota().getApoderado() != null
                && cita.getMascota().getApoderado().getUser() != null
                && cita.getMascota().getApoderado().getUser().getCompany() != null) {
            return cita.getMascota().getApoderado().getUser().getCompany().getId();
        }
        return null;
    }

    private MovimientoCajaResponse toResponse(MovimientoCaja m) {
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
        r.setCompanyId(m.getCompanyId());
        return r;
    }
}
