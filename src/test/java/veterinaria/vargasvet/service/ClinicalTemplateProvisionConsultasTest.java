package veterinaria.vargasvet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.entity.ClinicalConcept;
import veterinaria.vargasvet.domain.entity.ClinicalTemplate;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.repository.ClinicalConceptRepository;
import veterinaria.vargasvet.repository.ClinicalTemplateRepository;
import veterinaria.vargasvet.repository.ClinicalTemplateSectionRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.ConsultationFieldAnswerRepository;
import veterinaria.vargasvet.repository.ServiciosVeterinariosRepository;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicalTemplateProvisionConsultasTest {

    @Mock private ClinicalConceptRepository conceptRepository;
    @Mock private ClinicalTemplateRepository templateRepository;
    @Mock private ClinicalTemplateSectionRepository sectionRepository;
    @Mock private ConsultationFieldAnswerRepository answerRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private ServiciosVeterinariosRepository serviceRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private ClinicalTemplateService service;

    @Test
    void crearLaFichaGeneralBuscaLosConceptosBaseUnaVezPorSeccion() {
        Company company = new Company();
        company.setId(5);
        when(templateRepository.countByCompanyIdAndTemplateKey(5, ClinicalTemplateService.GENERAL_TEMPLATE_KEY)).thenReturn(0);
        when(conceptRepository.findByCompanyIsNullAndCodeIn(any())).thenAnswer(invocation -> {
            Collection<String> codigos = invocation.getArgument(0);
            return codigos.stream().map(codigo -> {
                ClinicalConcept concepto = new ClinicalConcept();
                concepto.setCode(codigo);
                return concepto;
            }).toList();
        });

        service.provisionDefault(company);

        verify(conceptRepository, times(3)).findByCompanyIsNullAndCodeIn(any());
        verify(conceptRepository, never()).findByCompanyIsNullAndCode(any());
        var plantilla = forClass(ClinicalTemplate.class);
        verify(templateRepository).save(plantilla.capture());
        assertThat(plantilla.getValue().getSections()).hasSize(3);
        assertThat(plantilla.getValue().getSections().stream().mapToInt(s -> s.getFields().size()).sum()).isEqualTo(10);
    }
}
