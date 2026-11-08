package veterinaria.vargasvet.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.domain.enums.PaymentStatus;
import veterinaria.vargasvet.domain.enums.MetodoPago;
import veterinaria.vargasvet.dto.request.PagoRequest;
import veterinaria.vargasvet.dto.response.PagoListResponse;
import veterinaria.vargasvet.dto.response.PagoResponse;
import veterinaria.vargasvet.dto.request.ReceiptResultRequest;

import java.time.LocalDate;

public interface PagoService {
    default PagoResponse registrar(PagoRequest request) {
        return registrar(request, null);
    }
    PagoResponse registrar(PagoRequest request, String idempotencyKey);
    PagoResponse reportarComprobante(Long pagoId, ReceiptResultRequest request);
    PagoResponse obtenerPorCita(Long citaId);
    Page<PagoListResponse> listarTodos(int page, int size, Integer companyId);
    Page<PagoListResponse> listarMisPagos(int page, int size);
    Page<PagoListResponse> listarHistorialPorEmpresa(int page, int size, Integer companyId,
                                                      Integer clienteId, Long mascotaId,
                                                      LocalDate fechaDesde, LocalDate fechaHasta,
                                                      PaymentStatus estado, MetodoPago metodoPago);
}
