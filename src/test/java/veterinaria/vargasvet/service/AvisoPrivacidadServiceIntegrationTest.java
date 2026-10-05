package veterinaria.vargasvet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.dto.request.CamposAvisoPrivacidad;
import veterinaria.vargasvet.dto.request.PublicarAvisoPrivacidadRequest;
import veterinaria.vargasvet.dto.response.AvisoPrivacidadResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.AvisoPrivacidadRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.util.LegalText;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DataJpaTest
class AvisoPrivacidadServiceIntegrationTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private AvisoPrivacidadRepository avisoRepository;

    private AvisoPrivacidadService service;
    private Company clinica;
    private Company otraClinica;

    @BeforeEach
    void setUp() {
        service = new AvisoPrivacidadService(avisoRepository, companyRepository, mock(AuditLogService.class), new ObjectMapper());
        clinica = empresa("Clínica Patitas", true);
        otraClinica = empresa("Clínica Garras", true);
    }

    private Company empresa(String nombre, boolean activa) {
        Company company = new Company();
        company.setName(nombre);
        company.setSlug("clinica-" + UUID.randomUUID());
        company.setRuc("20" + System.nanoTime());
        company.setAddress("Av. Principal 100, Lima");
        company.setEmail("contacto@" + UUID.randomUUID() + ".test");
        company.setActivo(activa);
        return companyRepository.saveAndFlush(company);
    }

    private CamposAvisoPrivacidad campos(String domicilio) {
        return new CamposAvisoPrivacidad("Clínica Patitas S.A.C.", "20123456789", domicilio, "privacidad@patitas.test",
                null, "VetSoft, proveedor de la plataforma", List.of("Atender a su mascota"),
                List.of("Nombres y apellidos", "Correo electrónico"), List.of("Observaciones"),
                List.of("El personal de la clínica"), "No se transfieren datos fuera del Perú",
                "Mientras sea cliente y 10 años después");
    }

    private PublicarAvisoPrivacidadRequest peticion(CamposAvisoPrivacidad campos) {
        PublicarAvisoPrivacidadRequest request = new PublicarAvisoPrivacidadRequest();
        request.setCampos(campos);
        request.setConfirmoRevisionLegal(true);
        return request;
    }

    @Test
    void laPrimeraPublicacionQuedaVigenteConSuHuella() {
        AvisoPrivacidadResponse publicado = service.publicar(clinica.getId(), peticion(campos("Av. Los Olivos 123, Lima")));

        assertThat(publicado.version()).isEqualTo(1);
        assertThat(publicado.activo()).isTrue();
        assertThat(publicado.contenido()).contains("Av. Los Olivos 123, Lima").contains("Versión 1");
        assertThat(publicado.contenidoHash()).isEqualTo(LegalText.sha256Hex(publicado.contenido()));
        assertThat(service.vigente(clinica.getId())).isPresent();
    }

    @Test
    void unCambioEsUnaVersionNuevaYLaAnteriorQuedaRetiradaSinPerderse() {
        service.publicar(clinica.getId(), peticion(campos("Av. Los Olivos 123, Lima")));

        AvisoPrivacidadResponse segunda = service.publicar(clinica.getId(), peticion(campos("Jr. Cusco 456, Lima")));

        assertThat(segunda.version()).isEqualTo(2);
        List<AvisoPrivacidadResponse> historial = service.historial(clinica.getId());
        assertThat(historial).extracting(AvisoPrivacidadResponse::version).containsExactly(2, 1);
        assertThat(historial).extracting(AvisoPrivacidadResponse::activo).containsExactly(true, false);
        assertThat(historial.get(1).contenido()).contains("Av. Los Olivos 123, Lima").doesNotContain("Jr. Cusco 456");
        assertThat(avisoRepository.findByCompanyIdAndActivoTrue(clinica.getId())).get()
                .extracting("version").isEqualTo(2);
    }

    @Test
    void publicarSinCambiosSeRechaza() {
        service.publicar(clinica.getId(), peticion(campos("Av. Los Olivos 123, Lima")));

        assertThatThrownBy(() -> service.publicar(clinica.getId(), peticion(campos("Av. Los Olivos 123, Lima"))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("No hay cambios");
        assertThat(service.historial(clinica.getId())).hasSize(1);
    }

    @Test
    void unCampoSinCompletarImpidePublicar() {
        assertThatThrownBy(() -> service.publicar(clinica.getId(), peticion(campos("[completar domicilio]"))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("sin completar");
        assertThatThrownBy(() -> service.publicar(clinica.getId(), peticion(campos("XXXXXXXX"))))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(service.historial(clinica.getId())).isEmpty();
    }

    @Test
    void cadaClinicaTieneSuPropioAvisoYSoloSeLeeElSuyo() {
        service.publicar(clinica.getId(), peticion(campos("Av. Los Olivos 123, Lima")));

        assertThat(service.publico(clinica.getSlug()).contenido()).contains("Av. Los Olivos 123, Lima");
        assertThatThrownBy(() -> service.publico(otraClinica.getSlug())).isInstanceOf(ResourceNotFoundException.class);
        assertThat(service.historial(otraClinica.getId())).isEmpty();
        assertThat(service.vigente(otraClinica.getId())).isEmpty();
    }

    @Test
    void elAvisoPublicoNoSeEntregaDeUnaClinicaInactivaNiDeUnSlugInexistente() {
        Company inactiva = empresa("Clínica cerrada", false);
        service.publicar(inactiva.getId(), peticion(campos("Av. Los Olivos 123, Lima")));

        assertThatThrownBy(() -> service.publico(inactiva.getSlug())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.publico("no-existe")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void sinClinicaNoSePuedePublicar() {
        assertThatThrownBy(() -> service.publicar(null, peticion(campos("Av. Los Olivos 123, Lima"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void laPlantillaPrecargaLosDatosDeLaClinicaYLuegoLosUltimosPublicados() {
        CamposAvisoPrivacidad inicial = service.plantilla(clinica.getId());

        assertThat(inicial.getRazonSocial()).isEqualTo("Clínica Patitas");
        assertThat(inicial.getRuc()).isEqualTo(clinica.getRuc());
        assertThat(inicial.getDomicilio()).isEqualTo("Av. Principal 100, Lima");
        assertThat(inicial.getTransferencias()).isNull();
        assertThat(inicial.getPlazoConservacion()).isNull();
        assertThat(inicial.getDatosObligatorios()).contains("Correo electrónico", "Teléfono");

        service.publicar(clinica.getId(), peticion(campos("Av. Los Olivos 123, Lima")));

        assertThat(service.plantilla(clinica.getId()).getDomicilio()).isEqualTo("Av. Los Olivos 123, Lima");
    }
}
