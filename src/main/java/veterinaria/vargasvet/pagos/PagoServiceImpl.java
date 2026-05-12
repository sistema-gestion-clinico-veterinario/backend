package veterinaria.vargasvet.pagos;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.citas.Cita;
import veterinaria.vargasvet.pagos.Purchase;
import veterinaria.vargasvet.shared.EstadoCita;
import veterinaria.vargasvet.shared.MetodoPago;
import veterinaria.vargasvet.shared.PaymentStatus;
import veterinaria.vargasvet.shared.TipoPurchase;
import veterinaria.vargasvet.pagos.PagoRequest;
import veterinaria.vargasvet.pagos.PagoResponse;
import veterinaria.vargasvet.shared.ResourceNotFoundException;
import veterinaria.vargasvet.citas.CitaRepository;
import veterinaria.vargasvet.pagos.PurchaseRepository;
import veterinaria.vargasvet.pagos.PagoService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements PagoService {

    private final CitaRepository citaRepository;
    private final PurchaseRepository purchaseRepository;

    @Override
    @Transactional
    public PagoResponse registrar(PagoRequest request) {
        Cita cita = citaRepository.findById(request.getCitaId())
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + request.getCitaId()));

        if (cita.getEstado() == EstadoCita.CANCELADA || cita.getEstado() == EstadoCita.NO_ASISTIO) {
            throw new IllegalArgumentException("No se puede registrar un pago para una cita con estado: " + cita.getEstado());
        }

        if (purchaseRepository.existsByCitaIdAndTipoPurchase(cita.getId(), TipoPurchase.SERVICIO_CITA)) {
            throw new IllegalArgumentException("La cita ya tiene un pago registrado");
        }

        BigDecimal total = cita.getTotalServicio() != null ? cita.getTotalServicio() : BigDecimal.ZERO;
        BigDecimal mitad = total.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);

        BigDecimal montoRecibido = null;
        BigDecimal cambio = null;
        PaymentStatus estado = PaymentStatus.PAID;

        if (request.getMetodoPago() == MetodoPago.EFECTIVO) {
            if (request.getMontoRecibido() == null) {
                throw new IllegalArgumentException("El monto recibido es obligatorio para pagos en efectivo");
            }
            if (request.getMontoRecibido().compareTo(mitad) < 0) {
                throw new IllegalArgumentException(
                    "El monto mínimo aceptado es S/ " + mitad + " (50% del total de S/ " + total + ")"
                );
            }
            montoRecibido = request.getMontoRecibido();
            if (montoRecibido.compareTo(total) >= 0) {
                cambio = montoRecibido.subtract(total);
            } else {
                estado = PaymentStatus.PENDING;
            }
        }

        Purchase pago = new Purchase();
        pago.setCita(cita);
        pago.setMetodoPago(request.getMetodoPago());
        pago.setTotal(total);
        pago.setMontoRecibido(montoRecibido);
        pago.setPaymentStatus(estado);
        pago.setTipoPurchase(TipoPurchase.SERVICIO_CITA);
        pago.setCreatedAt(LocalDateTime.now());

        Purchase savedPago = purchaseRepository.save(pago);

        cita.setMontoPagado(montoRecibido != null ? montoRecibido : total);
        citaRepository.save(cita);

        return toResponse(savedPago, cambio);
    }

    @Override
    @Transactional(readOnly = true)
    public PagoResponse obtenerPorCita(Long citaId) {
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

    private PagoResponse toResponse(Purchase pago, BigDecimal cambio) {
        PagoResponse response = new PagoResponse();
        response.setId(pago.getId());
        response.setCitaId(pago.getCita().getId());
        response.setMetodoPago(pago.getMetodoPago());
        response.setMonto(pago.getTotal());
        response.setMontoRecibido(pago.getMontoRecibido());
        response.setCambio(cambio);
        response.setFechaPago(pago.getCreatedAt());
        response.setEstado(pago.getPaymentStatus());
        return response;
    }
}
