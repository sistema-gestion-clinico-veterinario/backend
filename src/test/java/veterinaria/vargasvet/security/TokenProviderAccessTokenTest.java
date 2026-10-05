package veterinaria.vargasvet.security;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La sesión viaja en una cookie y los navegadores descartan las de más de 4096 bytes: el token de acceso lleva solo la
 * identidad, el rol activo y la clínica. Los permisos se consultan en el servidor en cada solicitud.
 */
class TokenProviderAccessTokenTest {

    private TokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new TokenProvider(new DefaultResourceLoader());
        ReflectionTestUtils.setField(provider, "jwtValidityInSeconds", 1800L);
        ReflectionTestUtils.setField(provider, "refreshTokenValidityInSeconds", 604800L);
        ReflectionTestUtils.setField(provider, "privateKeyPath", "classpath:keys/e2e_private_key.pem");
        ReflectionTestUtils.setField(provider, "publicKeyPath", "classpath:keys/e2e_public_key.pem");
        ReflectionTestUtils.setField(provider, "issuer", "systemvet-api");
        ReflectionTestUtils.setField(provider, "audience", "systemvet-web");
        provider.init();
    }

    private String accessToken(Integer companyId, String... roles) {
        return provider.createToken(7, "ana@vargasvet.test", List.of(roles), companyId, 3,
                companyId == null ? RoleScope.PLATFORM : RoleScope.STAFF,
                companyId == null ? RolePurpose.PLATFORM_ADMIN : RolePurpose.COMPANY_ADMIN, 5L, 2L, "sesion-1");
    }

    private String payload(String token) {
        return new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
    }

    @Test
    void elTokenDeAccesoNoLlevaPermisosYCabeEnUnaCookie() {
        String token = accessToken(null, "ROLE_SUPER_ADMIN");

        assertThat(payload(token)).doesNotContain("permissions").doesNotContain("VISTA_");
        assertThat(token.length()).isLessThan(1500);
        assertThat(("access_token__una-clinica-con-un-slug-largo=" + token).length()).isLessThan(4096);
    }

    @Test
    void laSesionConservaIdentidadRolEmpresaYVersiones() {
        Authentication authentication = provider.getAuthentication(accessToken(9, "ROLE_ADMIN"));

        UsuarioPrincipal principal = (UsuarioPrincipal) authentication.getPrincipal();
        assertThat(principal.getId()).isEqualTo(7);
        assertThat(principal.getCompanyId()).isEqualTo(9);
        assertThat(principal.getActiveRoleId()).isEqualTo(3);
        assertThat(principal.getActiveRolePurpose()).isEqualTo(RolePurpose.COMPANY_ADMIN);
        assertThat(principal.getPermissionVersion()).isEqualTo(5L);
        assertThat(principal.getCredentialsVersion()).isEqualTo(2L);
        assertThat(principal.getSessionId()).isEqualTo("sesion-1");
        assertThat(authentication.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
    }

    @Test
    void unTokenAnteriorQueTodaviaTraeLaListaDePermisosSigueValidoYSeIgnora() {
        PrivateKey clave = (PrivateKey) ReflectionTestUtils.getField(provider, "privateKey");
        List<String> permisos = IntStream.range(0, 200).mapToObj(i -> "VISTA_" + i + ":LEER").toList();
        String anterior = Jwts.builder()
                .subject("ana@vargasvet.test")
                .issuer("systemvet-api")
                .audience().add("systemvet-web").and()
                .id(UUID.randomUUID().toString())
                .claim("token_type", "access")
                .claim("userId", 7)
                .claim("roles", List.of("ROLE_ADMIN"))
                .claim("permissions", permisos)
                .claim("companyId", 9)
                .claim("activeRoleId", 3)
                .claim("permissionVersion", 5L)
                .claim("credentialsVersion", 2L)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(clave, Jwts.SIG.RS256)
                .compact();

        Authentication authentication = provider.getAuthentication(anterior);

        assertThat(authentication.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
    }

    @Test
    void elTicketDeTiempoRealTampocoLlevaPermisos() {
        var ticket = provider.createRealtimeTicket(7, "ana@vargasvet.test",
                List.of("ROLE_ADMIN", "VISTA_CAJA:LEER"), 9, 3, RoleScope.STAFF, RolePurpose.COMPANY_ADMIN, 5L, "sesion-1");

        assertThat(payload(ticket.token())).doesNotContain("permissions").doesNotContain("VISTA_CAJA");
        assertThat(provider.getRealtimeTicketDetails(ticket.token()).authentication().getAuthorities())
                .extracting(Object::toString).containsExactly("ROLE_ADMIN");
    }
}
