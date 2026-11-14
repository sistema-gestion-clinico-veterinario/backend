package veterinaria.vargasvet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.service.ConsentimientoDatosService;
import veterinaria.vargasvet.service.LegalDocumentService;
import veterinaria.vargasvet.service.SessionActivityService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Si cambian los permisos de un rol, el token que circula con la versión anterior deja de autenticar. */
@ExtendWith(MockitoExtension.class)
class JWTFilterVersionDePermisosTest {

    private static final String ACCESS_TOKEN = "token";
    private static final long VERSION_DEL_TOKEN = 4L;

    @Mock TokenProvider tokenProvider;
    @Mock UsuarioRepository usuarioRepository;
    @Mock UsuarioPorRolRepository usuarioPorRolRepository;
    @Mock UsuarioEmpresaCredencialRepository credencialRepository;
    @Mock FilterChain chain;

    private JWTFilter filter;

    @BeforeEach
    void preparar() {
        filter = new JWTFilter(tokenProvider, usuarioRepository, usuarioPorRolRepository, credencialRepository,
                mock(ApoderadoRepository.class), mock(ConsentimientoDatosService.class),
                mock(LegalDocumentService.class), mock(SessionActivityService.class));
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void siSeSubeLaVersionDeLosPermisosDelRolElTokenAnteriorNoAutenticaYNoSeConsultaNadaMas() throws Exception {
        dadoUnTokenConVersion(VERSION_DEL_TOKEN, VERSION_DEL_TOKEN + 1);

        filter.doFilter(solicitud(), mock(HttpServletResponse.class), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(any(), any());
        verifyNoInteractions(usuarioRepository, credencialRepository);
    }

    private void dadoUnTokenConVersion(long versionDelToken, long versionActualDelRol) {
        Role rol = new Role();
        rol.setId(1);
        rol.setPurpose(RolePurpose.CUSTOM);
        rol.setScope(RoleScope.STAFF);
        rol.setPermissionVersion(versionActualDelRol);
        UsuarioPorRol asignacion = new UsuarioPorRol();
        asignacion.setRol(rol);

        UsuarioPrincipal principal = new UsuarioPrincipal(
                42, "ana@example.test", "", List.of(), 7, 1, RoleScope.STAFF, RolePurpose.CUSTOM, versionDelToken);
        when(tokenProvider.getAuthentication(ACCESS_TOKEN))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        when(usuarioPorRolRepository.findActiveAssignmentByUsuarioIdAndRoleIdAndCompanyId(42, 1, 7))
                .thenReturn(Optional.of(asignacion));
    }

    private HttpServletRequest solicitud() {
        HttpServletRequest solicitud = mock(HttpServletRequest.class);
        when(solicitud.getMethod()).thenReturn("GET");
        when(solicitud.getServletPath()).thenReturn("/mascotas");
        when(solicitud.getCookies()).thenReturn(new Cookie[]{new Cookie("access_token", ACCESS_TOKEN)});
        return solicitud;
    }
}
