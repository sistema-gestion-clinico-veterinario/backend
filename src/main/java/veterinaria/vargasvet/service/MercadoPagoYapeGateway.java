package veterinaria.vargasvet.service;

import java.math.BigDecimal;

public interface MercadoPagoYapeGateway {

    YapePaymentResult createPayment(BigDecimal total, Long phoneNumber, Integer otp, String payerEmail);

    record YapePaymentResult(String id, String status, String statusDetail) {
    }
}
