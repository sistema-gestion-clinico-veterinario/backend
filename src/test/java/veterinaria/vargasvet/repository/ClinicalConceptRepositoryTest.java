package veterinaria.vargasvet.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import veterinaria.vargasvet.domain.entity.ClinicalConcept;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.enums.ClinicalConceptOrigin;
import veterinaria.vargasvet.domain.enums.ClinicalValueType;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class ClinicalConceptRepositoryTest {

    @Autowired private ClinicalConceptRepository conceptRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private Company clinica;
    private Company otraClinica;
    private ClinicalConcept global;
    private ClinicalConcept propio;
    private ClinicalConcept ajeno;

    @BeforeEach
    void datos() {
        clinica = clinica("Clinica Uno");
        otraClinica = clinica("Clinica Dos");
        global = concepto(null, "global.peso");
        propio = concepto(clinica, "propio.dolor");
        ajeno = concepto(otraClinica, "ajeno.secreto");
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void traeLosConceptosGlobalesYLosDeLaEmpresaPedidos() {
        List<ClinicalConcept> conceptos = conceptRepository
                .findAccessibleByIds(List.of(global.getId(), propio.getId()), clinica.getId());

        assertThat(conceptos).extracting(ClinicalConcept::getId).containsExactlyInAnyOrder(global.getId(), propio.getId());
    }

    @Test
    void noTraeLosConceptosDeOtraEmpresaAunqueSePidan() {
        List<ClinicalConcept> conceptos = conceptRepository
                .findAccessibleByIds(List.of(global.getId(), propio.getId(), ajeno.getId()), clinica.getId());

        assertThat(conceptos).extracting(ClinicalConcept::getId)
                .containsExactlyInAnyOrder(global.getId(), propio.getId())
                .doesNotContain(ajeno.getId());
    }

    @Test
    void ignoraLosIdentificadoresQueNoExisten() {
        List<ClinicalConcept> conceptos = conceptRepository
                .findAccessibleByIds(List.of(propio.getId(), -1L), clinica.getId());

        assertThat(conceptos).extracting(ClinicalConcept::getId).containsExactly(propio.getId());
    }

    @Test
    void esUnaSolaSentenciaSinImportarCuantosConceptosSePidan() {
        Statistics estadisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        estadisticas.clear();

        conceptRepository.findAccessibleByIds(List.of(global.getId(), propio.getId(), ajeno.getId()), clinica.getId());

        assertThat(estadisticas.getPrepareStatementCount()).isEqualTo(1);
    }

    private Company clinica(String nombre) {
        Company company = new Company();
        company.setName(nombre);
        company.setSlug("c-" + UUID.randomUUID());
        company.setRuc("20" + String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 9));
        company.setActivo(true);
        return companyRepository.save(company);
    }

    private ClinicalConcept concepto(Company empresa, String codigo) {
        ClinicalConcept concepto = new ClinicalConcept();
        concepto.setCompany(empresa);
        concepto.setCode(codigo);
        concepto.setBaseName(codigo);
        concepto.setValueType(ClinicalValueType.TEXT_SHORT);
        concepto.setOrigin(empresa == null ? ClinicalConceptOrigin.SYSTEM : ClinicalConceptOrigin.COMPANY);
        concepto.setReportable(false);
        concepto.setActive(true);
        return conceptRepository.save(concepto);
    }
}
