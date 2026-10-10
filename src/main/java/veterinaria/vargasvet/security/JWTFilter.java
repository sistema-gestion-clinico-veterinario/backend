package veterinaria.vargasvet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;
import veterinaria.vargasvet.repository.RefreshTokenRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.AudienciaAvisoPrivacidad;
import veterinaria.vargasvet.service.ConsentimientoDatosService;
import veterinaria.vargasvet.service.LegalDocumentService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JWTFilter extends GenericFilterBean {

    public static final String COMPANY_HEADER = "X-Company-Id";
    private static final int HTTP_PRECONDITION_REQUIRED = 428;
    private static final String PRIVACY_NOTICE_REQUIRED = "PRIVACY_NOTICE_REQUIRED";
    private static final String TERMS_NOT_ACCEPTED = "TERMS_NOT_ACCEPTED";
    private final TokenProvider tokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioPorRolRepository usuarioPorRolRepository;
    private final UsuarioEmpresaCredencialRepository credencialRepository;
    private final ConsentimientoDatosService consentimientoDatosService;
    private final LegalDocumentService legalDocumentService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        if (isPublicAuthEndpoint(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        String token = resolveToken(httpRequest);

        if (token != null) {
            try {
                Authentication authentication = tokenProvider.getAuthentication(token);

                UsuarioPrincipal principal = authentication.getPrincipal() instanceof UsuarioPrincipal value
                        ? value : null;
                if (principal == null || principal.getActiveRoleId() == null) {
                    throw new org.springframework.security.authentication.BadCredentialsException(
                            "La sesión no contiene un rol activo válido");
                }

                var activeAssignment = usuarioPorRolRepository
                        .findActiveAssignmentByUsuarioIdAndRoleId(principal.getId(), principal.getActiveRoleId())
                        .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException(
                                "El rol de la sesión ya no está disponible"));
                if (activeAssignment.getRol().getPermissionVersion() != principal.getPermissionVersion()) {
                    throw new org.springframework.security.authentication.CredentialsExpiredException(
                            "Los permisos de la sesión cambiaron; actualice la sesión");
                }
                boolean esSuperAdmin = activeAssignment.getRol().getPurpose() == RolePurpose.PLATFORM_ADMIN;

                // Por id, no por email: el correo ya no identifica de forma unica una
                // cuenta (puede repetirse entre usuarios distintos desde esta migracion).
                var currentUser = usuarioRepository.findByIdWithCompany(principal.getId())
                        .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException(
                                "La cuenta de la sesión ya no existe"));
                // La credencial de la empresa de ESTA sesion, no un campo global - cada
                // empresa tiene su propia credentials_version.
                var credencial = (principal.getCompanyId() == null
                        ? credencialRepository.findByUsuarioIdAndCompanyIsNull(principal.getId())
                        : credencialRepository.findByUsuarioIdAndCompanyId(principal.getId(), principal.getCompanyId()))
                        .orElse(null);
                if (credencial == null || credencial.getCredentialsVersion() != principal.getCredentialsVersion()) {
                    throw new org.springframework.security.authentication.CredentialsExpiredException(
                            "La sesión fue invalidada por un evento de seguridad");
                }
                // Cerrar sesión (o que se revoque) invalida también el access token que ya circula:
                // sin esto seguiría sirviendo hasta que venza. Los tokens anteriores a este campo no
                // traen sesión y se aceptan hasta su vencimiento.
                if (principal.getSessionId() != null
                        && !refreshTokenRepository.existsByFamilyIdAndRevokedAtIsNull(principal.getSessionId())) {
                    throw new org.springframework.security.authentication.CredentialsExpiredException(
                            "La sesión fue cerrada");
                }
                boolean bloqueado = !currentUser.isActivo()
                        || (!esSuperAdmin && currentUser.getCompany() != null
                        && !currentUser.getCompany().isActivo());

                if (bloqueado) {
                    SecurityContextHolder.clearContext();
                    HttpServletResponse httpResponse = (HttpServletResponse) response;
                    httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    httpResponse.setContentType("application/json");
                    httpResponse.getWriter().write("{\"error\":\"Acceso denegado. La empresa o el usuario está inactivo.\"}");
                    return;
                }

                String declaredCompany = httpRequest.getHeader(COMPANY_HEADER);
                if (declaredCompany != null) {
                    Integer declaredCompanyId = parseCompanyId(declaredCompany);
                    if (declaredCompanyId == null) {
                        rejectInvalidCompany((HttpServletResponse) response);
                        return;
                    }
                    if (principal.getCompanyId() != null
                            && !declaredCompanyId.equals(principal.getCompanyId())) {
                        rejectCompanyMismatch((HttpServletResponse) response);
                        return;
                    }
                    if (principal.getCompanyId() == null) {
                        if (!esSuperAdmin) {
                            rejectCompanyMismatch((HttpServletResponse) response);
                            return;
                        }
                        ActiveCompanyContext.set(httpRequest, declaredCompanyId);
                    }
                }

                String declaredSlug = SessionCookies.slugOf(httpRequest);
                if (declaredSlug != null && principal.getCompanyId() != null) {
                    veterinaria.vargasvet.domain.entity.Company empresaDeLaSesion = activeAssignment.getCompany() != null
                            ? activeAssignment.getCompany()
                            : activeAssignment.getRol().getCompany() != null
                                    ? activeAssignment.getRol().getCompany()
                                    : currentUser.getCompany();
                    if (empresaDeLaSesion != null
                            && !java.util.Objects.equals(declaredSlug, SessionCookies.sanitize(empresaDeLaSesion.getSlug()))) {
                        rejectCompanyMismatch((HttpServletResponse) response);
                        return;
                    }
                }

                AudienciaAvisoPrivacidad audiencia = audienciaPara(activeAssignment.getRol().getPurpose());
                if (debeRevisarAviso(httpRequest, principal, audiencia)) {
                    rejectPrivacyNotice((HttpServletResponse) response);
                    return;
                }

                if (!isLegalExemptEndpoint(httpRequest) && legalDocumentService.isPastGracePeriod(principal.getId())) {
                    rejectLegalDocuments((HttpServletResponse) response);
                    return;
                }

                boolean declaraSlugClinica = SessionCookies.slugOf(httpRequest) != null;
                if (declaraSlugClinica && principal.getCompanyId() == null) {
                    SecurityContextHolder.clearContext();
                } else {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }

    private AudienciaAvisoPrivacidad audienciaPara(RolePurpose rolePurpose) {
        return rolePurpose == RolePurpose.CLIENT_PORTAL
                ? AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS
                : AudienciaAvisoPrivacidad.TRABAJADORES_Y_USUARIOS;
    }

    private boolean debeRevisarAviso(HttpServletRequest request, UsuarioPrincipal principal,
                                     AudienciaAvisoPrivacidad audiencia) {
        return !isPrivacyNoticeExemptEndpoint(request)
                && consentimientoDatosService.requiereLecturaPersonal(
                        principal.getId(), principal.getCompanyId(), audiencia);
    }

    private void rejectPrivacyNotice(HttpServletResponse response) throws IOException {
        writeJsonError(response, HTTP_PRECONDITION_REQUIRED,
                "Debes revisar el aviso de privacidad vigente de la clínica antes de continuar.",
                PRIVACY_NOTICE_REQUIRED);
    }

    private void rejectLegalDocuments(HttpServletResponse response) throws IOException {
        writeJsonError(response, HttpServletResponse.SC_FORBIDDEN,
                "Debes revisar los documentos vigentes de SoftVet antes de continuar.",
                TERMS_NOT_ACCEPTED);
    }

    private void rejectCompanyMismatch(HttpServletResponse response) throws IOException {
        writeJsonError(response, HttpServletResponse.SC_CONFLICT,
                "La sesión abierta en este navegador pertenece a otra clínica.",
                "SESSION_COMPANY_MISMATCH");
    }

    private void rejectInvalidCompany(HttpServletResponse response) throws IOException {
        writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                "La empresa activa indicada no es válida.",
                "INVALID_ACTIVE_COMPANY");
    }

    private Integer parseCompanyId(String value) {
        try {
            int companyId = Integer.parseInt(value.trim());
            return companyId > 0 ? companyId : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void writeJsonError(HttpServletResponse response, int status, String message, String code)
            throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\",\"code\":\"" + code + "\"}");
    }

    private boolean isPublicAuthEndpoint(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();

        return path.equals("/auth/login")
                || path.equals("/auth/admin-login")
                || path.equals("/auth/refresh")
                || path.equals("/auth/logout")
                || path.equals("/auth/forgot-password")
                || path.equals("/auth/reset-password")
                || path.equals("/auth/email-change/confirm-current")
                || path.equals("/auth/email-change/cancel")
                || path.equals("/auth/email-change/confirm-new")
                || path.equals("/auth/validate-reset-token")
                || path.equals("/auth/setup-account")
                || path.equals("/auth/resend-verification")
                || path.equals("/auth/resend-verification-by-token")
                || path.startsWith("/setup/")
                || path.startsWith("/v3/api-docs/")
                || path.startsWith("/swagger-ui/")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/ws/")
                || path.equals("/error")
                || ("GET".equalsIgnoreCase(method) && path.startsWith("/media/"))
                || ("GET".equalsIgnoreCase(method) && path.startsWith("/company/branding/"));
    }

    private boolean isLegalExemptEndpoint(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/legal/")
                // El aviso de la clínica se muestra antes que los documentos de la plataforma.
                // Sin esta excepción un usuario con términos vencidos no podría consultar ni
                // reconocer el aviso y el frontend entraría directamente a /legal/accept.
                || path.startsWith("/privacidad/")
                || path.equals("/auth/logout")
                || path.equals("/auth/refresh");
    }

    private boolean isPrivacyNoticeExemptEndpoint(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();
        return ("GET".equalsIgnoreCase(method) && path.equals("/privacidad/aviso-vigente"))
                || ("GET".equalsIgnoreCase(method) && path.equals("/privacidad/mi-estado"))
                || ("POST".equalsIgnoreCase(method) && path.equals("/privacidad/enterado"))
                || path.equals("/auth/logout")
                || path.equals("/auth/refresh");
    }

    private String resolveToken(HttpServletRequest request) {
        // 1. La cookie de la clínica que declara la pestaña (o la anterior sin sufijo)
        java.util.Optional<String> delNavegador = SessionCookies.readAccess(request);
        if (delNavegador.isPresent()) {
            return delNavegador.get();
        }
        // 2. Fallback to Authorization: Bearer header
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
