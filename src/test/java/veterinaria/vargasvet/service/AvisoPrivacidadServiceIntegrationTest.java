package veterinaria.vargasvet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.enums.AudienciaAvisoPrivacidad;
import veterinaria.vargasvet.dto.request.CamposAvisoPrivacidad;
import veterinaria.vargasvet.dto.request.PublicarAvisoPrivacidadRequest;
import veterinaria.vargasvet.dto.request.VistaPreviaAvisoRequest;
import veterinaria.vargasvet.dto.response.AvisoPrivacidadResponse;
import veterinaria.vargasvet.dto.response.VistaPreviaAvisoResponse;
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
        return peticion(campos, AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS);
    }

    private PublicarAvisoPrivacidadRequest peticion(CamposAvisoPrivacidad campos,
                                                     AudienciaAvisoPrivacidad audiencia) {
        PublicarAvisoPrivacidadRequest request = new PublicarAvisoPrivacidadRequest();
        request.setAudiencia(audiencia);
        request.setCampos(campos);
        request.setConfirmoRevisionLegal(true);
        return request;
    }

    @Test
    void cadaAudienciaTieneSuPropiaVigenciaNumeracionYRedaccion() {
        service.publicar(clinica.getId(), peticion(campos("Av. Clientes 100, Lima")));
        AvisoPrivacidadResponse trabajadores = service.publicar(clinica.getId(), peticion(
                campos("Av. Personal 200, Lima"), AudienciaAvisoPrivacidad.TRABAJADORES_Y_USUARIOS));

        assertThat(trabajadores.version()).isEqualTo(1);
        assertThat(trabajadores.audiencia()).isEqualTo(AudienciaAvisoPrivacidad.TRABAJADORES_Y_USUARIOS);
        assertThat(trabajadores.contenido()).contains("TRABAJADORES Y USUARIOS INTERNOS")
                .contains("vínculo laboral o profesional")
                .doesNotContain("recordatorios de las vacunas");
        assertThat(service.vigente(clinica.getId(), AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS)).isPresent();
        assertThat(service.vigente(clinica.getId(), AudienciaAvisoPrivacidad.TRABAJADORES_Y_USUARIOS)).isPresent();
        assertThat(service.historial(clinica.getId(), AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS)).hasSize(1);
        assertThat(service.historial(clinica.getId(), AudienciaAvisoPrivacidad.TRABAJADORES_Y_USUARIOS)).hasSize(1);
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

    private static final String CHROME_WINDOWS =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36";

    @Test
    void laPublicacionDejaConstanciaDeQuienCuandoYDesdeQueDispositivo() {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.TestingAuthenticationToken("admin@patitas.test", null));
        AvisoPrivacidadResponse publicado;
        try {
            publicado = service.publicar(clinica.getId(), peticion(campos("Av. Los Olivos 123, Lima")),
                    "190.40.10.5", CHROME_WINDOWS);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }

        assertThat(publicado.creadoPor()).isEqualTo("admin@patitas.test");
        assertThat(publicado.vigenteDesde()).isNotNull();
        assertThat(publicado.creadoDispositivo()).isEqualTo("Chrome en Windows");
        assertThat(publicado.creadoIp()).isEqualTo("190.40.10.5");
        AvisoPrivacidadResponse guardado = service.historial(clinica.getId()).get(0);
        assertThat(guardado.creadoDispositivo()).isEqualTo("Chrome en Windows");
        assertThat(guardado.creadoIp()).isEqualTo("190.40.10.5");
    }

    @Test
    void laAuditoriaDeLaPublicacionTambienNombraElDispositivo() {
        AuditLogService auditoria = mock(AuditLogService.class);
        AvisoPrivacidadService conAuditoria = new AvisoPrivacidadService(avisoRepository, companyRepository, auditoria, new ObjectMapper());

        conAuditoria.publicar(clinica.getId(), peticion(campos("Av. Los Olivos 123, Lima")), "190.40.10.5", CHROME_WINDOWS);

        org.mockito.Mockito.verify(auditoria).log(org.mockito.ArgumentMatchers.eq("PUBLICAR_AVISO_PRIVACIDAD"),
                org.mockito.ArgumentMatchers.eq("Seguridad"), org.mockito.ArgumentMatchers.contains("Desde Chrome en Windows"));
    }

    @Test
    void unaIpMuyLargaOUnEquipoSinIdentificarNoRompenLaPublicacion() {
        AvisoPrivacidadResponse publicado = service.publicar(clinica.getId(), peticion(campos("Av. Los Olivos 123, Lima")),
                "x".repeat(200), null);

        assertThat(publicado.creadoIp()).hasSize(64);
        assertThat(publicado.creadoDispositivo()).isEqualTo("Equipo sin identificar");
    }

    @Test
    void lasVersionesAnterioresALaConstanciaConservanSusDatosSinDispositivo() {
        AvisoPrivacidadResponse publicado = service.publicar(clinica.getId(), peticion(campos("Av. Los Olivos 123, Lima")));

        assertThat(publicado.creadoDispositivo()).isNull();
        assertThat(publicado.creadoIp()).isNull();
    }

    @Test
    void laVistaPreviaMuestraExactamenteElTextoQueSePublicariaYNoGuardaNada() {
        CamposAvisoPrivacidad datos = campos("Av. Los Olivos 123, Lima");
        VistaPreviaAvisoRequest solicitud = new VistaPreviaAvisoRequest();
        solicitud.setCampos(datos);

        VistaPreviaAvisoResponse previa = service.vistaPrevia(clinica.getId(), solicitud);

        assertThat(avisoRepository.findByCompanyIdOrderByVersionDesc(clinica.getId())).isEmpty();
        assertThat(previa.version()).isEqualTo(1);
        assertThat(previa.observaciones()).isEmpty();
        assertThat(previa.sinCambios()).isFalse();
        AvisoPrivacidadResponse publicado = service.publicar(clinica.getId(), peticion(datos));
        assertThat(publicado.contenido()).isEqualTo(previa.contenido());
    }

    @Test
    void laVistaPreviaSigueLaNumeracionYAvisaCuandoNoHayCambios() {
        CamposAvisoPrivacidad datos = campos("Av. Los Olivos 123, Lima");
        service.publicar(clinica.getId(), peticion(datos));
        VistaPreviaAvisoRequest igual = new VistaPreviaAvisoRequest();
        igual.setCampos(campos("Av. Los Olivos 123, Lima"));
        VistaPreviaAvisoRequest distinto = new VistaPreviaAvisoRequest();
        distinto.setCampos(campos("Jr. Cusco 456, Lima"));

        assertThat(service.vistaPrevia(clinica.getId(), igual).sinCambios()).isTrue();
        VistaPreviaAvisoResponse nueva = service.vistaPrevia(clinica.getId(), distinto);
        assertThat(nueva.sinCambios()).isFalse();
        assertThat(nueva.version()).isEqualTo(2);
        assertThat(nueva.contenido()).contains("Jr. Cusco 456, Lima").contains("Versión 2");
    }

    @Test
    void laVistaPreviaMuestraElAvisoIncompletoYDiceQueFalta() {
        CamposAvisoPrivacidad vacios = new CamposAvisoPrivacidad();
        vacios.setRazonSocial("Clínica Patitas S.A.C.");
        VistaPreviaAvisoRequest solicitud = new VistaPreviaAvisoRequest();
        solicitud.setCampos(vacios);

        VistaPreviaAvisoResponse previa = service.vistaPrevia(clinica.getId(), solicitud);

        assertThat(previa.contenido()).contains("Clínica Patitas S.A.C.");
        assertThat(previa.observaciones()).contains("Falta el RUC", "Falta el domicilio",
                "Indica al menos una finalidad", "Indica el plazo de conservación");
        assertThat(previa.observaciones()).doesNotContain("Falta la razón social");
    }

    @Test
    void laVistaPreviaSeñalaElTextoDeBorrador() {
        CamposAvisoPrivacidad datos = campos("[completar domicilio]");
        VistaPreviaAvisoRequest solicitud = new VistaPreviaAvisoRequest();
        solicitud.setCampos(datos);

        VistaPreviaAvisoResponse previa = service.vistaPrevia(clinica.getId(), solicitud);

        assertThat(previa.observaciones()).anyMatch(o -> o.contains("[completar domicilio]"));
    }

    @Test
    void sinClinicaNoHayVistaPrevia() {
        VistaPreviaAvisoRequest solicitud = new VistaPreviaAvisoRequest();
        solicitud.setCampos(campos("Av. Los Olivos 123, Lima"));

        assertThatThrownBy(() -> service.vistaPrevia(null, solicitud)).isInstanceOf(IllegalArgumentException.class);
    }
}
