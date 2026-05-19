package veterinaria.vargasvet.service.impl;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
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
import veterinaria.vargasvet.service.PagoService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements PagoService {

    private final CitaRepository citaRepository;
    private final PurchaseRepository purchaseRepository;
    private final RestTemplate restTemplate;

    @Value("${mercadopago.public-key}")
    private String mpPublicKey;

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
        String mercadoPagoId = null;
        String mpStatus = null;

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

        } else if (request.getMetodoPago() == MetodoPago.YAPE) {
            if (request.getYapePhoneNumber() == null) {
                throw new IllegalArgumentException("El número de teléfono Yape es obligatorio");
            }
            if (request.getYapeOtp() == null) {
                throw new IllegalArgumentException("El código OTP de Yape es obligatorio");
            }
            if (request.getPayerEmail() == null || request.getPayerEmail().isBlank()) {
                throw new IllegalArgumentException("El email del pagador es obligatorio para pagos con Yape");
            }

            // Paso 1: obtener token Yape desde MercadoPago (sin CORS — llamada server-side)
            String yapeMpToken = obtenerTokenYape(request.getYapePhoneNumber(), request.getYapeOtp());

            // Paso 2: crear el pago con el token obtenido
            try {
                PaymentClient client = new PaymentClient();
                PaymentCreateRequest mpRequest = PaymentCreateRequest.builder()
                        .transactionAmount(total)
                        .token(yapeMpToken)
                        .installments(1)
                        .paymentMethodId("yape")
                        .description("Pago de servicio veterinario - VargasVet")
                        .payer(PaymentPayerRequest.builder()
                                .email(request.getPayerEmail())
                                .build())
                        .build();

                Payment mpPayment = client.create(mpRequest);
                mercadoPagoId = String.valueOf(mpPayment.getId());
                mpStatus = mpPayment.getStatus();

                if (!"approved".equals(mpStatus)) {
                    throw new IllegalArgumentException(
                        "Pago Yape rechazado: " + mpPayment.getStatusDetail()
                    );
                }
                estado = PaymentStatus.PAID;

            } catch (MPApiException e) {
                throw new RuntimeException("Error de MercadoPago al crear pago: " + e.getApiResponse().getContent());
            } catch (MPException e) {
                throw new RuntimeException("Error al conectar con MercadoPago: " + e.getMessage());
            }
        }

        Purchase pago = new Purchase();
        pago.setCita(cita);
        pago.setMetodoPago(request.getMetodoPago());
        pago.setTotal(total);
        pago.setMontoRecibido(montoRecibido);
        pago.setPaymentStatus(estado);
        pago.setTipoPurchase(TipoPurchase.SERVICIO_CITA);
        pago.setMercadoPagoId(mercadoPagoId);
        pago.setMpStatus(mpStatus);
        pago.setCreatedAt(LocalDateTime.now());

        Purchase savedPago = purchaseRepository.save(pago);

        cita.setMontoPagado(montoRecibido != null ? montoRecibido : total);
        citaRepository.save(cita);

        auditLogService.log(
            "REGISTRAR_PAGO",
            "Facturación",
            "Se registró un pago por un monto total de S/ " + total + " para la cita de la mascota " + cita.getMascota().getNombreCompleto() + " con método de pago: " + request.getMetodoPago()
        );

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

    /**
     * Llama a la API de MercadoPago Yape para obtener el token de pago.
     * Se ejecuta server-side para evitar restricciones CORS del browser.
     */
    private String obtenerTokenYape(Long phoneNumber, Integer otp) {
        String url = "https://api.mercadopago.com/platforms/pci/yape/v1/payment?public_key=" + mpPublicKey;

        // MP espera phoneNumber y otp como strings (ver cURL oficial de MP)
        String requestId = UUID.randomUUID().toString();
        String jsonBody = "{\"phoneNumber\":\"" + phoneNumber
                + "\",\"otp\":\"" + otp
                + "\",\"requestId\":\"" + requestId + "\"}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map<?, ?> responseBody = response.getBody();
            if (responseBody == null || responseBody.get("id") == null) {
                throw new IllegalArgumentException("Respuesta inválida de MercadoPago Yape");
            }
            return (String) responseBody.get("id");
        } catch (HttpClientErrorException e) {
            // Exponer el error real de MP para diagnóstico
            throw new IllegalArgumentException(
                "MP Yape error " + e.getStatusCode() + ": " + e.getResponseBodyAsString()
            );
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al conectar con MercadoPago Yape: " + e.getMessage());
        }
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
        response.setMercadoPagoId(pago.getMercadoPagoId());
        response.setMpStatus(pago.getMpStatus());
        return response;
    }
}
