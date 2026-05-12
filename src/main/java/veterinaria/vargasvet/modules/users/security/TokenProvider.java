package veterinaria.vargasvet.modules.users.security;

import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

@Component
@RequiredArgsConstructor
public class TokenProvider {

    @Value("${jwt.validity-in-seconds:259200}")
    private long jwtValidityInSeconds;

    @Value("${jwt.private-key:classpath:keys/private_key.pem}")
    private String privateKeyPath;

    @Value("${jwt.public-key:classpath:keys/public_key.pem}")
    private String publicKeyPath;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    private final ResourceLoader resourceLoader;

    @PostConstruct
    public void init() {
        try {
            this.privateKey = loadPrivateKey(privateKeyPath);
            this.publicKey = loadPublicKey(publicKeyPath);
        } catch (Exception e) {
            System.err.println("Error cargando claves RSA: " + e.getMessage());
        }
    }

    public String createToken(String email, List<String> roles, Integer companyId, List<String> permissions) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + (jwtValidityInSeconds * 1000));

        JwtBuilder builder = Jwts.builder()
                .subject(email)
                .claim("roles", roles)
                .claim("companyId", companyId)
                .claim("permissions", permissions)
                .issuedAt(now)
                .expiration(validity)
                .signWith(privateKey);

        return builder.compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        List<GrantedAuthority> authorities = new ArrayList<>();
        List<?> roles = claims.get("roles", List.class);
        if (roles != null) {
            roles.stream()
                    .map(Object::toString)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }

        List<?> rawPermissions = claims.get("permissions", List.class);
        if (rawPermissions != null) {
            rawPermissions.stream()
                    .map(Object::toString)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }

        String email = claims.getSubject();
        Integer companyId = claims.get("companyId", Integer.class);

        UsuarioPrincipal principal = new UsuarioPrincipal(
                null,
                email,
                "",
                authorities,
                companyId
        );

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    private PrivateKey loadPrivateKey(String resourcePath) throws Exception {
        Resource resource = resourceLoader.getResource(resourcePath);
        byte[] keyBytes = readAllBytes(resource);
        String key = new String(keyBytes)
                .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    private PublicKey loadPublicKey(String resourcePath) throws Exception {
        Resource resource = resourceLoader.getResource(resourcePath);
        byte[] keyBytes = readAllBytes(resource);
        String key = new String(keyBytes)
                .replaceAll("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    private byte[] readAllBytes(Resource resource) throws IOException {
        try (InputStream is = resource.getInputStream()) {
            return is.readAllBytes();
        }
    }
}
