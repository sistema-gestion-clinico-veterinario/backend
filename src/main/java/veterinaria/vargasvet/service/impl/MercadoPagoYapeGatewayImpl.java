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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import veterinaria.vargasvet.service.MercadoPagoYapeGateway;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MercadoPagoYapeGatewayImpl implements MercadoPagoYapeGateway {

    private final RestTemplate restTemplate;

    @Value("${mercadopago.public-key}")
    private String mpPublicKey;

    @Override
    public YapePaymentResult createPayment(BigDecimal total, Long phoneNumber, Integer otp, String payerEmail) {
        String yapeMpToken = obtenerTokenYape(phoneNumber, otp);

        try {
            PaymentClient client = new PaymentClient();
            PaymentCreateRequest mpRequest = PaymentCreateRequest.builder()
                    .transactionAmount(total)
                    .token(yapeMpToken)
                    .installments(1)
                    .paymentMethodId("yape")
                    .description("Pago de servicio veterinario - VargasVet")
                    .payer(PaymentPayerRequest.builder()
                            .email(payerEmail)
                            .build())
                    .build();

            Payment mpPayment = client.create(mpRequest);
            return new YapePaymentResult(
                    String.valueOf(mpPayment.getId()),
                    mpPayment.getStatus(),
                    mpPayment.getStatusDetail()
            );
        } catch (MPApiException e) {
            throw new RuntimeException("Error de MercadoPago al crear pago: " + e.getApiResponse().getContent());
        } catch (MPException e) {
            throw new RuntimeException("Error al conectar con MercadoPago: " + e.getMessage());
        }
    }

    private String obtenerTokenYape(Long phoneNumber, Integer otp) {
        String url = "https://api.mercadopago.com/platforms/pci/yape/v1/payment?public_key=" + mpPublicKey;
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
                throw new IllegalArgumentException("Respuesta invalida de MercadoPago Yape");
            }
            return (String) responseBody.get("id");
        } catch (HttpClientErrorException e) {
            throw new IllegalArgumentException(
                    "MP Yape error " + e.getStatusCode() + ": " + e.getResponseBodyAsString()
            );
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al conectar con MercadoPago Yape: " + e.getMessage());
        }
    }
}
