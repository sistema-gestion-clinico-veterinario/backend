package veterinaria.vargasvet.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class BackupReportTokenVerifier {

    private final byte[] expectedToken;

    public BackupReportTokenVerifier(@Value("${backup.reporting.token:}") String token) {
        this.expectedToken = token.getBytes(StandardCharsets.UTF_8);
    }

    public boolean isValid(String providedToken) {
        if (expectedToken.length < 32 || providedToken == null) return false;
        return MessageDigest.isEqual(expectedToken, providedToken.getBytes(StandardCharsets.UTF_8));
    }
}
