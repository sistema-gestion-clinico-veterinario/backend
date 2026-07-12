package veterinaria.vargasvet.e2e;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import veterinaria.vargasvet.service.MercadoPagoYapeGateway;
import veterinaria.vargasvet.security.RateLimitFilter;

import java.io.IOException;

@Configuration
@Profile("e2e")
public class E2eTestConfiguration {

    @Bean
    RateLimitFilter rateLimitFilter() {
        return new RateLimitFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain chain
            ) throws ServletException, IOException {
                chain.doFilter(request, response);
            }
        };
    }

    @Bean
    @Primary
    MercadoPagoYapeGateway e2eMercadoPagoYapeGateway() {
        return (total, phoneNumber, otp, payerEmail) -> {
            if (Long.valueOf(111111111L).equals(phoneNumber) && Integer.valueOf(123456).equals(otp)) {
                return new MercadoPagoYapeGateway.YapePaymentResult("e2e-mp-approved", "approved", null);
            }
            if (Long.valueOf(111111113L).equals(phoneNumber) && Integer.valueOf(123456).equals(otp)) {
                return new MercadoPagoYapeGateway.YapePaymentResult(
                        null,
                        "rejected",
                        "cc_rejected_insufficient_amount"
                );
            }
            return new MercadoPagoYapeGateway.YapePaymentResult(null, "rejected", "cc_rejected_other_reason");
        };
    }
}
