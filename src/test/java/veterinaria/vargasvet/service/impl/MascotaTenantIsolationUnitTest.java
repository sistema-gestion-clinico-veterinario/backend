package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.mapper.MascotaMapper;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.HistoriaClinicaRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.RazaRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.util.BusinessValidator;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MascotaTenantIsolationUnitTest {

    @Mock MascotaRepository mascotaRepository;
    @Mock ApoderadoRepository apoderadoRepository;
    @Mock CitaRepository citaRepository;
    @Mock HistoriaClinicaRepository historiaClinicaRepository;
    @Mock MascotaMapper mascotaMapper;
    @Mock BusinessValidator businessValidator;
    @Mock AuditLogService auditLogService;
    @Mock RazaRepository razaRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void obtenerPorId_noConsultaFueraDelTenantAutenticado() {
        authenticateTenant(7);
        when(mascotaRepository.findByIdAndCompanyId(81L, 7)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service().obtenerPorId(81L));

        verify(mascotaRepository, never()).findById(81L);
    }

    @Test
    void obtenerPorUuid_noConsultaFueraDelTenantAutenticado() {
        authenticateTenant(7);
        when(mascotaRepository.findByUuidAndCompanyId("uuid-ajeno", 7)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service().obtenerPorUuid("uuid-ajeno"));

        verify(mascotaRepository, never()).findByUuid("uuid-ajeno");
    }

    private MascotaServiceImpl service() {
        return new MascotaServiceImpl(
                mascotaRepository,
                apoderadoRepository,
                citaRepository,
                historiaClinicaRepository,
                mascotaMapper,
                businessValidator,
                auditLogService,
                razaRepository
        );
    }

    private void authenticateTenant(Integer companyId) {
        UsuarioPrincipal principal = new UsuarioPrincipal(
                15, "empleado@test.local", "", List.of(), companyId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
