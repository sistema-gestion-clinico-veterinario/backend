package veterinaria.vargasvet.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolePermissionEvaluatorSolicitudTest {

    @Mock private PermisosDelRol permisosDelRol;
    @Mock private UsuarioPorRolRepository usuarioPorRolRepository;

    @InjectMocks private RolePermissionEvaluator evaluator;

    @AfterEach
    void limpiarSolicitud() {
        RequestContextHolder.resetRequestAttributes();
    }

    private UsuarioPorRol asignacionDeAdministradorDePlataforma() {
        Role rol = new Role();
        rol.setPurpose(RolePurpose.PLATFORM_ADMIN);
        UsuarioPorRol asignacion = new UsuarioPorRol();
        asignacion.setRol(rol);
        return asignacion;
    }

    @Test
    void variasComprobacionesDeUnaMismaSolicitudReutilizanLaAsignacionYaCargada() {
        MockHttpServletRequest solicitud = new MockHttpServletRequest();
        solicitud.setAttribute(RolePermissionEvaluator.ASIGNACION_ACTIVA_ATTR,
                new RolePermissionEvaluator.AsignacionDeSolicitud(1, 2, 3, asignacionDeAdministradorDePlataforma()));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(solicitud));

        assertThat(evaluator.can(1, 2, 3, "VISTA_CITAS", "LEER")).isTrue();
        assertThat(evaluator.can(1, 2, 3, "VISTA_HISTORIAS", "ESCRIBIR")).isTrue();

        verify(usuarioPorRolRepository, never())
                .findActiveAssignmentByUsuarioIdAndRoleIdAndCompanyId(any(), any(), any());
    }

    @Test
    void sinSolicitudActivaConsultaLaBaseDeDatosComoAntes() {
        when(usuarioPorRolRepository.findActiveAssignmentByUsuarioIdAndRoleIdAndCompanyId(1, 2, 3))
                .thenReturn(Optional.of(asignacionDeAdministradorDePlataforma()));

        assertThat(evaluator.can(1, 2, 3, "VISTA_CITAS", "LEER")).isTrue();

        verify(usuarioPorRolRepository, times(1))
                .findActiveAssignmentByUsuarioIdAndRoleIdAndCompanyId(1, 2, 3);
    }

    @Test
    void siLaAsignacionGuardadaEsDeOtroUsuarioOEmpresaNoSeReutiliza() {
        MockHttpServletRequest solicitud = new MockHttpServletRequest();
        solicitud.setAttribute(RolePermissionEvaluator.ASIGNACION_ACTIVA_ATTR,
                new RolePermissionEvaluator.AsignacionDeSolicitud(1, 2, 3, asignacionDeAdministradorDePlataforma()));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(solicitud));
        when(usuarioPorRolRepository.findActiveAssignmentByUsuarioIdAndRoleIdAndCompanyId(9, 2, 3))
                .thenReturn(Optional.empty());

        assertThat(evaluator.can(9, 2, 3, "VISTA_CITAS", "LEER")).isFalse();

        verify(usuarioPorRolRepository, times(1))
                .findActiveAssignmentByUsuarioIdAndRoleIdAndCompanyId(9, 2, 3);
    }
}
