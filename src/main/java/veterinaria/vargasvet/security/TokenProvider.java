package veterinaria.vargasvet.security;

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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TokenProvider {

    @Value("${jwt.validity-in-seconds:1800}") // 30 minutos
    private long jwtValidityInSeconds;

    @Value("${jwt.refresh-validity-in-seconds:604800}") // 7 días
    private long refreshTokenValidityInSeconds;

    @Value("${jwt.private-key:classpath:keys/private_key.pem}")
    private String privateKeyPath;

    @Value("${jwt.public-key:classpath:keys/public_key.pem}")
    private String publicKeyPath;

    @Value("${jwt.issuer:systemvet-api}")
    private String issuer;

    @Value("${jwt.audience:systemvet-web}")
    private String audience;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    private final ResourceLoader resourceLoader;

    @PostConstruct
    public void init() {
        try {
            this.privateKey = loadPrivateKey(privateKeyPath);
            this.publicKey = loadPublicKey(publicKeyPath);
        } catch (Exception e) {
            throw new IllegalStateException("Error cargando claves RSA: " + e.getMessage(), e);
        }
    }

    public String createToken(Integer userId, String email, List<String> roles,
                              List<String> permissions, Integer companyId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + (jwtValidityInSeconds * 1000));

        return Jwts.builder()
                .subject(email)
                .issuer(issuer)
                .audience().add(audience).and()
                .id(UUID.randomUUID().toString())
                .claim("token_type", "access")
                .claim("userId", userId)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .claim("companyId", companyId)
                .issuedAt(now)
                .expiration(validity)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public String createRefreshToken(String email) {
        return createRefreshToken(email, null);
    }

    public String createRefreshToken(String email, String activeRole) {
        return createRefreshToken(email, activeRole, UUID.randomUUID().toString());
    }

    public String createRefreshToken(String email, String activeRole, String familyId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + (refreshTokenValidityInSeconds * 1000));

        JwtBuilder builder = Jwts.builder()
                .subject(email)
                .issuer(issuer)
                .audience().add(audience).and()
                .id(UUID.randomUUID().toString())
                .claim("token_type", "refresh")
                .claim("familyId", familyId)
                .issuedAt(now)
                .expiration(validity)
                .signWith(privateKey, Jwts.SIG.RS256);

        if (activeRole != null && !activeRole.isBlank()) {
            builder.claim("activeRole", activeRole);
        }

        return builder.compact();
    }

    public Optional<String> getActiveRoleFromRefreshToken(String token) {
        try {
            Claims claims = parseRefreshClaims(token);
            return Optional.ofNullable(claims.get("activeRole", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Optional<String> getEmailFromToken(String token) {
        try {
            Claims claims = parseRefreshClaims(token);
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public boolean validateToken(String token) {
        try {
            parseAccessClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseAccessClaims(token);

        return authenticationFromClaims(token, claims);
    }

    public IssuedRealtimeTicket createRealtimeTicket(Integer userId, String email,
                                                      List<String> authorities, Integer companyId) {
        Instant issuedAt = veterinaria.vargasvet.util.AppClock.instantNow();
        Instant expiresAt = issuedAt.plusSeconds(60);
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .subject(email)
                .issuer(issuer)
                .audience().add(audience).and()
                .id(jti)
                .claim("token_type", "realtime")
                .claim("userId", userId)
                .claim("roles", authorities.stream().filter(a -> a.startsWith("ROLE_")).toList())
                .claim("permissions", authorities.stream().filter(a -> !a.startsWith("ROLE_")).toList())
                .claim("companyId", companyId)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
        return new IssuedRealtimeTicket(token, jti, issuedAt, expiresAt);
    }

    public RealtimeTicketDetails getRealtimeTicketDetails(String ticket) {
        Claims claims = parser().require("token_type", "realtime").build()
                .parseSignedClaims(ticket).getPayload();
        if (claims.getId() == null || claims.getExpiration() == null) {
            throw new MalformedJwtException("Ticket realtime incompleto");
        }
        return new RealtimeTicketDetails(
                authenticationFromClaims(ticket, claims),
                claims.getId(),
                claims.getExpiration().toInstant());
    }

    private Authentication authenticationFromClaims(String token, Claims claims) {

        List<GrantedAuthority> authorities = new ArrayList<>();
        List<?> roles = claims.get("roles", List.class);
        if (roles != null) {
            roles.stream()
                    .map(Object::toString)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }
        List<?> permissions = claims.get("permissions", List.class);
        if (permissions != null) {
            permissions.stream()
                    .map(Object::toString)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }

        String email = claims.getSubject();
        Integer companyId = claims.get("companyId", Integer.class);

        Integer userId = claims.get("userId", Integer.class);
        if (userId == null || email == null || email.isBlank()) {
            throw new MalformedJwtException("Token sin identidad de usuario");
        }

        UsuarioPrincipal principal = new UsuarioPrincipal(
                userId,
                email,
                "",
                authorities,
                companyId
        );

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    public RefreshTokenDetails getRefreshTokenDetails(String token) {
        Claims claims = parseRefreshClaims(token);
        return new RefreshTokenDetails(
                claims.getSubject(),
                claims.getId(),
                claims.get("familyId", String.class),
                claims.get("activeRole", String.class));
    }

    private Claims parseAccessClaims(String token) {
        return parser().require("token_type", "access").build()
                .parseSignedClaims(token).getPayload();
    }

    private Claims parseRefreshClaims(String token) {
        return parser().require("token_type", "refresh").build()
                .parseSignedClaims(token).getPayload();
    }

    private JwtParserBuilder parser() {
        return Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(issuer)
                .requireAudience(audience);
    }

    public record RefreshTokenDetails(String email, String jti, String familyId, String activeRole) {}
    public record IssuedRealtimeTicket(String token, String jti, Instant issuedAt, Instant expiresAt) {}
    public record RealtimeTicketDetails(Authentication authentication, String jti, Instant expiresAt) {}

    private PrivateKey loadPrivateKey(String resourcePath) throws Exception {
        String key = loadPemContent(resourcePath)
                .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    private PublicKey loadPublicKey(String resourcePath) throws Exception {
        String key = loadPemContent(resourcePath)
                .replaceAll("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    private String loadPemContent(String value) throws IOException {
        String normalized = value.replace("\\n", "\n").trim();
        if (normalized.startsWith("-----BEGIN")) {
            return normalized;
        }

        Resource resource = resourceLoader.getResource(value);
        return new String(readAllBytes(resource), StandardCharsets.UTF_8);
    }

    private byte[] readAllBytes(Resource resource) throws IOException {
        try (InputStream is = resource.getInputStream()) {
            return is.readAllBytes();
        }
    }
}
