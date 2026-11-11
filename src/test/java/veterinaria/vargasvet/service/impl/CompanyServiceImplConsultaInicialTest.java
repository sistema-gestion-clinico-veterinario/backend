package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.dto.request.CompanyDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CompanyOperatingHourRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CompanyRoleProvisioningService;
import veterinaria.vargasvet.service.PortalPropietarioPolicyService;
import veterinaria.vargasvet.util.BusinessValidator;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplConsultaInicialTest {

    @Mock CompanyRepository companyRepository;
    @Mock CompanyOperatingHourRepository companyOperatingHourRepository;
    @Mock BusinessValidator businessValidator;
    @Mock CompanyRoleProvisioningService companyRoleProvisioningService;
    @Mock AuditLogService auditLogService;
    @Mock PortalPropietarioPolicyService portalPropietarioPolicyService;

    @InjectMocks CompanyServiceImpl service;

    @Test
    void superAdminSinEmpresaObtieneLaPrimeraSinCargarTodasLasEmpresas() {
        Company primera = new Company();
        primera.setId(1);
        primera.setName("Clínica Uno");
        when(companyRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(primera));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentCompanyId).thenReturn(null);
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            CompanyDTO dto = service.getCompanyInfo();

            assertThat(dto.getId()).isEqualTo(1);
        }

        verify(companyRepository, never()).findAll();
    }

    @Test
    void superAdminSinEmpresasRegistradasRecibeNoEncontrada() {
        when(companyRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentCompanyId).thenReturn(null);
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            assertThatThrownBy(() -> service.getCompanyInfo())
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        verify(companyRepository, never()).findAll();
    }
}
