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
import veterinaria.vargasvet.domain.enums.TipoMovimiento;
import veterinaria.vargasvet.dto.request.MovimientoEgresoRequest;
import veterinaria.vargasvet.dto.response.MovimientoCajaResponse;
import veterinaria.vargasvet.dto.response.ResumenCajaResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.MovimientoCajaRepository;
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

    @Override
    @Transactional
    public void registrarIngresoPorCita(Cita cita, Integer companyId) {
        if (companyId == null || cita.getMontoPagado() == null
                || cita.getMontoPagado().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        MovimientoCaja m = new MovimientoCaja();
        m.setTipo(TipoMovimiento.INGRESO);
        m.setConcepto(ConceptoMovimiento.PAGO_CITA);
        m.setMonto(cita.getMontoPagado());
        m.setCitaId(cita.getId());
        m.setDescripcion("Pago cita #" + cita.getId() + " - " + cita.getMascota().getNombreCompleto());
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

        BigDecimal montoDevuelto = cita.getMontoPagado();

        MovimientoCaja m = new MovimientoCaja();
        m.setTipo(TipoMovimiento.DEVOLUCION);
        m.setConcepto(ConceptoMovimiento.CANCELACION_DEVOLUCION);
        m.setMonto(montoDevuelto);
        m.setCitaId(citaId);
        m.setDescripcion("Devolución cita cancelada #" + citaId + " - " + cita.getMascota().getNombreCompleto());
        m.setRegistradoPor(SecurityUtils.getCurrentUserEmail());
        m.setCompanyId(companyId);
        MovimientoCaja saved = movimientoRepo.save(m);

        cita.setMontoPagado(BigDecimal.ZERO);
        citaRepository.save(cita);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public MovimientoCajaResponse registrarEgreso(MovimientoEgresoRequest request) {
        MovimientoCaja m = new MovimientoCaja();
        m.setTipo(TipoMovimiento.EGRESO);
        m.setConcepto(request.getConcepto() != null ? request.getConcepto() : ConceptoMovimiento.GASTO_OPERATIVO);
        m.setMonto(request.getMonto());
        m.setDescripcion(request.getDescripcion());
        m.setRegistradoPor(SecurityUtils.getCurrentUserEmail());
        m.setCompanyId(request.getCompanyId());
        return toResponse(movimientoRepo.save(m));
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenCajaResponse getResumen(Integer companyId, LocalDate desde, LocalDate hasta) {
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
        r.setCitaId(m.getCitaId());
        r.setDescripcion(m.getDescripcion());
        r.setFecha(m.getFecha());
        r.setRegistradoPor(m.getRegistradoPor());
        r.setCompanyId(m.getCompanyId());
        return r;
    }
}
