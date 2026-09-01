package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.mapper.UserMapper;
import veterinaria.vargasvet.repository.*;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CompanyRoleProvisioningService;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.SessionSecurityService;
import veterinaria.vargasvet.util.BusinessValidator;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementTenantIsolationUnitTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock RoleRepository roleRepository;
    @Mock EmpleadoRepository empleadoRepository;
    @Mock EspecialidadRepository especialidadRepository;
    @Mock TipoEmpleadoRepository tipoEmpleadoRepository;
    @Mock CompanyRepository companyRepository;
    @Mock HorarioEmpleadoRepository horarioEmpleadoRepository;
    @Mock CompanyOperatingHourRepository companyOperatingHourRepository;
    @Mock CompanyExceptionRepository companyExceptionRepository;
    @Mock CitaRepository citaRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock UserMapper userMapper;
    @Mock EmailService emailService;
    @Mock BusinessValidator businessValidator;
    @Mock AuditLogService auditLogService;
    @Mock UsuarioPorRolRepository usuarioPorRolRepository;
    @Mock ApoderadoRepository apoderadoRepository;
    @Mock MascotaRepository mascotaRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock CompanyRoleProvisioningService companyRoleProvisioningService;
    @Mock SessionSecurityService sessionSecurityService;

    @InjectMocks EmpleadoServiceImpl empleadoService;
    @InjectMocks ApoderadoServiceImpl apoderadoService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void empleadoPorId_noConsultaFueraDeLaEmpresaAutenticada() {
        authenticateTenant(7);
        when(empleadoRepository.findByIdAndCompanyId(81L, 7)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> empleadoService.findById(81L));

        verify(empleadoRepository, never()).findById(81L);
    }

    @Test
    void clientePorId_noConsultaFueraDeLaEmpresaAutenticada() {
        authenticateTenant(7);
        when(apoderadoRepository.findByIdAndCompanyId(91L, 7)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> apoderadoService.findById(91L));

        verify(apoderadoRepository, never()).findById(91L);
    }

    @Test
    void superAdministrador_conservaBusquedaGlobalExplicita() {
        authenticatePlatformAdmin();
        when(empleadoRepository.findById(81L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> empleadoService.findById(81L));

        verify(empleadoRepository, never()).findByIdAndCompanyId(81L, null);
    }

    private void authenticateTenant(Integer companyId) {
        UsuarioPrincipal principal = new UsuarioPrincipal(
                15, "admin@empresa.test", "", List.of(), companyId);
        setAuthentication(principal);
    }

    private void authenticatePlatformAdmin() {
        UsuarioPrincipal principal = new UsuarioPrincipal(
                1, "superadmin@plataforma.test", "", List.of(), null,
                1, null, RolePurpose.PLATFORM_ADMIN, 0L);
        setAuthentication(principal);
    }

    private void setAuthentication(UsuarioPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
