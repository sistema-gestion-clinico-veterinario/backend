package veterinaria.vargasvet.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ClientIpResolver {

    private static final Pattern IPV4 = Pattern.compile(
            "^(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)){3}$");
    private static final Pattern IPV6 = Pattern.compile("^[0-9a-fA-F:]+:[0-9a-fA-F:]*$");

    private final int trustedProxyCount;

    public ClientIpResolver(@Value("${app.trusted-proxy-count:1}") int trustedProxyCount) {
        if (trustedProxyCount < 0) {
            throw new IllegalArgumentException("app.trusted-proxy-count no puede ser negativo");
        }
        this.trustedProxyCount = trustedProxyCount;
    }

    public String resolve(HttpServletRequest request) {
        if (request == null) return null;

        if (trustedProxyCount > 0) {
            String header = request.getHeader("X-Forwarded-For");
            String candidate = extractClientHop(header, trustedProxyCount);
            if (candidate != null && isValidIp(candidate)) {
                return candidate;
            }
        }

        return request.getRemoteAddr();
    }

    private String extractClientHop(String forwardedForHeader, int trustedProxyCount) {
        if (forwardedForHeader == null || forwardedForHeader.isBlank()) return null;

        List<String> hops = new ArrayList<>();
        for (String hop : forwardedForHeader.split(",")) {
            String trimmed = hop.trim();
            if (!trimmed.isEmpty()) hops.add(trimmed);
        }

        int clientIndex = hops.size() - trustedProxyCount - 1;
        if (clientIndex < 0) return null;

        return hops.get(clientIndex);
    }

    private boolean isValidIp(String value) {
        return IPV4.matcher(value).matches() || IPV6.matcher(value).matches();
    }
}
