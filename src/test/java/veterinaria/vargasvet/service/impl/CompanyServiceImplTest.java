package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.dto.request.CompanyDTO;
import veterinaria.vargasvet.repository.CompanyOperatingHourRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CompanyRoleProvisioningService;
import veterinaria.vargasvet.util.BusinessValidator;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Cubre la asignacion del slug al crear una empresa - antes de este fix,
 * save() nunca asignaba slug y la columna es NOT NULL UNIQUE desde V66, por
 * lo que crear una empresa nueva fallaba en la base de datos. El slug lo
 * decide explicitamente quien crea la empresa - nunca se deriva del nombre
 * ni se le agrega un sufijo si ya esta en uso (se rechaza en su lugar), para
 * que los flujos de login de dos empresas nunca queden ambiguos o fusionados.
 */
@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @Mock CompanyRepository companyRepository;
    @Mock CompanyOperatingHourRepository companyOperatingHourRepository;
    @Mock BusinessValidator businessValidator;
    @Mock CompanyRoleProvisioningService companyRoleProvisioningService;
    @Mock AuditLogService auditLogService;

    @InjectMocks CompanyServiceImpl service;

    private CompanyDTO dtoValido() {
        CompanyDTO dto = new CompanyDTO();
        dto.setName("Clínica Veterinaria Vargas Vet");
        dto.setRuc("20999999999");
        dto.setAddress("Av. Principal 123");
        dto.setPhone("987654321");
        dto.setEmail("contacto@vargasvet.pe");
        return dto;
    }

    @Test
    void rechazaCrearUnaEmpresaSinSlug() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                    () -> service.save(dtoValido()));
        }

        verify(companyRepository, never()).save(any());
    }

    @Test
    void respetaElSlugElegidoExplicitamenteEnVezDeDerivarloDelNombre() {
        when(companyRepository.existsBySlug("mi-veterinaria")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> {
            Company c = inv.getArgument(0);
            c.setId(3);
            return c;
        });

        CompanyDTO dto = dtoValido();
        dto.setSlug("mi-veterinaria");

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserEmail).thenReturn("admin@vargasvet.pe");
            service.save(dto);
        }

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo("mi-veterinaria");
    }

    @Test
    void rechazaCrearConUnSlugExplicitoYaUsadoEnVezDeAgregarleUnSufijo() {
        when(companyRepository.existsBySlug("mi-veterinaria")).thenReturn(true);

        CompanyDTO dto = dtoValido();
        dto.setSlug("mi-veterinaria");

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                    () -> service.save(dto));
        }

        verify(companyRepository, never()).save(any());
    }

    @Test
    void rechazaActualizarConUnSlugYaUsadoPorOtraEmpresa() {
        Company existente = new Company();
        existente.setId(5);
        existente.setName("Otra Empresa");
        existente.setSlug("otra-empresa");
        existente.setActivo(true);
        when(companyRepository.findById(5)).thenReturn(Optional.of(existente));
        when(companyRepository.existsBySlugAndIdNot("empresa-ocupada", 5)).thenReturn(true);

        CompanyDTO dto = dtoValido();
        dto.setSlug("empresa-ocupada");

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                    () -> service.update(5, dto));
        }

        verify(companyRepository, never()).save(any());
    }

    private Company empresaExistente(String slug) {
        Company existente = new Company();
        existente.setId(5);
        existente.setName("Clínica Patitas");
        existente.setSlug(slug);
        existente.setActivo(true);
        when(companyRepository.findById(5)).thenReturn(Optional.of(existente));
        lenient().when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return existente;
    }

    private MockedStatic<SecurityUtils> comoAdministradorDeLaClinica() {
        MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class);
        security.when(SecurityUtils::isSuperAdmin).thenReturn(false);
        security.when(SecurityUtils::getCurrentCompanyId).thenReturn(5);
        return security;
    }

    @Test
    void elAdministradorDeLaClinicaNoPuedeCambiarLaUrlDeAcceso() {
        Company existente = empresaExistente("patitas");
        CompanyDTO dto = dtoValido();
        dto.setSlug("otra-url");

        try (MockedStatic<SecurityUtils> security = comoAdministradorDeLaClinica()) {
            org.junit.jupiter.api.Assertions.assertThrows(
                    org.springframework.security.access.AccessDeniedException.class, () -> service.update(5, dto));
        }

        assertThat(existente.getSlug()).isEqualTo("patitas");
        verify(companyRepository, never()).save(any());
    }

    @Test
    void elAdministradorDeLaClinicaTampocoPuedeHacerloPorLaRutaDeSuPropiaEmpresa() {
        Company existente = empresaExistente("patitas");
        CompanyDTO dto = dtoValido();
        dto.setSlug("otra-url");

        try (MockedStatic<SecurityUtils> security = comoAdministradorDeLaClinica()) {
            org.junit.jupiter.api.Assertions.assertThrows(
                    org.springframework.security.access.AccessDeniedException.class, () -> service.updateCompanyInfo(dto));
        }

        assertThat(existente.getSlug()).isEqualTo("patitas");
        verify(companyRepository, never()).save(any());
    }

    @Test
    void siLaClinicaEnviaSuMismaUrlLaEdicionDeSusDemasDatosSigueFuncionando() {
        Company existente = empresaExistente("patitas");
        CompanyDTO dto = dtoValido();
        dto.setSlug("patitas");

        try (MockedStatic<SecurityUtils> security = comoAdministradorDeLaClinica()) {
            service.update(5, dto);
        }

        assertThat(existente.getSlug()).isEqualTo("patitas");
        assertThat(existente.getName()).isEqualTo("Clínica Veterinaria Vargas Vet");
        verify(companyRepository).save(existente);
    }

    @Test
    void siLaClinicaNoEnviaUrlSeConservaLaActual() {
        Company existente = empresaExistente("patitas");

        try (MockedStatic<SecurityUtils> security = comoAdministradorDeLaClinica()) {
            service.update(5, dtoValido());
        }

        assertThat(existente.getSlug()).isEqualTo("patitas");
        verify(companyRepository).save(existente);
    }

    @Test
    void laMismaUrlEscritaConMayusculasOEspaciosNoCuentaComoUnCambio() {
        Company existente = empresaExistente("patitas");
        CompanyDTO dto = dtoValido();
        dto.setSlug("  Patitas ");

        try (MockedStatic<SecurityUtils> security = comoAdministradorDeLaClinica()) {
            service.update(5, dto);
        }

        assertThat(existente.getSlug()).isEqualTo("patitas");
    }

    @Test
    void elAdministradorDeLaPlataformaSiPuedeCambiarla() {
        Company existente = empresaExistente("patitas");
        when(companyRepository.existsBySlugAndIdNot("url-nueva", 5)).thenReturn(false);
        CompanyDTO dto = dtoValido();
        dto.setSlug("url-nueva");

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            service.update(5, dto);
        }

        assertThat(existente.getSlug()).isEqualTo("url-nueva");
        verify(companyRepository).save(existente);
    }
}
