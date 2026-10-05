package veterinaria.vargasvet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.ConsentimientoDatos;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.CanalConsentimiento;
import veterinaria.vargasvet.domain.enums.FinalidadDatos;
import veterinaria.vargasvet.dto.request.CamposAvisoPrivacidad;
import veterinaria.vargasvet.dto.request.PublicarAvisoPrivacidadRequest;
import veterinaria.vargasvet.dto.response.ConsentimientoEstadoResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.AvisoPrivacidadRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.ConsentimientoDatosRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyInt;

@DataJpaTest
class ConsentimientoDatosServiceIntegrationTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private AvisoPrivacidadRepository avisoRepository;
    @Autowired private ConsentimientoDatosRepository consentimientoRepository;

    private AvisoPrivacidadService avisoService;
    private ConsentimientoDatosService service;
    private Company clinica;
    private Company otraClinica;
    private Usuario ana;
    private Usuario recepcion;

    @BeforeEach
    void setUp() {
        avisoService = new AvisoPrivacidadService(avisoRepository, companyRepository, mock(AuditLogService.class), new ObjectMapper());
        CompanyMembershipService membership = mock(CompanyMembershipService.class);
        when(membership.hasAnyMembership(anyInt(), anyInt())).thenAnswer(invocation -> {
            Integer usuarioId = invocation.getArgument(0);
            Integer companyId = invocation.getArgument(1);
            return usuarioRepository.findById(usuarioId)
                    .map(usuario -> usuario.getCompany() != null && companyId.equals(usuario.getCompany().getId()))
                    .orElse(false);
        });
        service = new ConsentimientoDatosService(consentimientoRepository, avisoRepository, usuarioRepository,
                apoderadoRepository, mock(AuditLogService.class), membership);
        clinica = empresa("Clínica Patitas");
        otraClinica = empresa("Clínica Garras");
        ana = persona("ana", clinica);
        recepcion = persona("recepcion", clinica);
    }

    private Company empresa(String nombre) {
        Company company = new Company();
        company.setName(nombre);
        company.setSlug("clinica-" + UUID.randomUUID());
        company.setRuc("20" + System.nanoTime());
        company.setActivo(true);
        return companyRepository.saveAndFlush(company);
    }

    private Usuario persona(String prefijo, Company empresa) {
        Usuario usuario = new Usuario();
        usuario.setEmail(prefijo + "-" + UUID.randomUUID() + "@example.test");
        usuario.setUsername(prefijo + "-" + UUID.randomUUID());
        usuario.setNombre(prefijo);
        usuario.setApellido("Test");
        usuario.setCompany(empresa);
        usuario.setActivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private void publicarAviso(Company empresa, String domicilio) {
        CamposAvisoPrivacidad campos = new CamposAvisoPrivacidad("Clínica S.A.C.", "20123456789", domicilio,
                "privacidad@clinica.test", null, null, List.of("Atender a su mascota"), List.of("Nombres y apellidos"),
                List.of(), List.of("El personal de la clínica"), "No se transfieren datos fuera del Perú",
                "Mientras sea cliente");
        PublicarAvisoPrivacidadRequest request = new PublicarAvisoPrivacidadRequest();
        request.setCampos(campos);
        request.setConfirmoRevisionLegal(true);
        avisoService.publicar(empresa.getId(), request);
    }

    private ConsentimientoEstadoResponse.Finalidad recordatorios(ConsentimientoEstadoResponse estado) {
        return estado.finalidades().stream().filter(f -> f.codigo().equals("RECORDATORIOS_PREVENTIVOS")).findFirst().orElseThrow();
    }

    @Test
    void sinAvisoPublicadoNoSePuedeRegistrarANadie() {
        assertThatThrownBy(() -> service.exigirAltaValida(clinica.getId(), true, false, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(ConsentimientoDatosService.MENSAJE_SIN_AVISO);
        assertThat(service.hayAvisoPublicado(clinica.getId())).isFalse();
    }

    @Test
    void conAvisoPublicadoSeExigeLaConstanciaDeQueSeInformo() {
        publicarAviso(clinica, "Av. Los Olivos 123, Lima");

        assertThatThrownBy(() -> service.exigirAltaValida(clinica.getId(), null, false, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.exigirAltaValida(clinica.getId(), false, false, null)).isInstanceOf(IllegalArgumentException.class);
        service.exigirAltaValida(clinica.getId(), true, false, null);
    }

    @Test
    void alRegistrarUnClienteLaDecisionSobreRecordatoriosEsExplicitaPeroPuedeSerNo() {
        publicarAviso(clinica, "Av. Los Olivos 123, Lima");

        assertThatThrownBy(() -> service.exigirAltaValida(clinica.getId(), true, true, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("recordatorios");
        service.exigirAltaValida(clinica.getId(), true, true, false);
        service.exigirAltaValida(clinica.getId(), true, true, true);
    }

    @Test
    void elAvisoDeOtraClinicaNoValeParaEstaClinica() {
        publicarAviso(otraClinica, "Jr. Cusco 456, Lima");

        assertThatThrownBy(() -> service.exigirAltaValida(clinica.getId(), true, false, null)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void elAltaDejaConstanciaDeQueSeInformoYDeLaDecisionSobreLosRecordatorios() {
        publicarAviso(clinica, "Av. Los Olivos 123, Lima");

        service.registrarAlta(ana, clinica.getId(), false, recepcion.getId());

        ConsentimientoEstadoResponse estado = service.estado(ana.getId(), clinica.getId());
        assertThat(estado.informada()).isTrue();
        assertThat(estado.informadaVersion()).isEqualTo(1);
        assertThat(estado.informadaCanal()).isEqualTo(CanalConsentimiento.PRESENCIAL);
        assertThat(recordatorios(estado).estado()).isEqualTo("RETIRADO");
        assertThat(service.usuariosQueOtorgaron(Set.of(ana.getId(), recepcion.getId()), clinica.getId(),
                FinalidadDatos.RECORDATORIOS_PREVENTIVOS)).isEmpty();
        ConsentimientoDatos constancia = consentimientoRepository
                .findByUsuarioIdAndCompanyIdOrderByIdDesc(ana.getId(), clinica.getId()).get(0);
        assertThat(constancia.getRegistradoPor().getId()).isEqualTo(recepcion.getId());
        assertThat(constancia.getContenidoHash()).hasSize(64);
    }

    @Test
    void registrarDosVecesLoMismoNoDuplicaConstancias() {
        publicarAviso(clinica, "Av. Los Olivos 123, Lima");

        service.registrarAlta(ana, clinica.getId(), true, recepcion.getId());
        service.registrarAlta(ana, clinica.getId(), true, recepcion.getId());

        assertThat(consentimientoRepository.findByUsuarioIdAndCompanyIdOrderByIdDesc(
                ana.getId(), clinica.getId())).hasSize(2);
    }

    @Test
    void sinDecidirNoSeRegistraRecordatoriosYQuedaSinRegistro() {
        publicarAviso(clinica, "Av. Los Olivos 123, Lima");

        service.registrarAlta(ana, clinica.getId(), null, recepcion.getId());

        assertThat(recordatorios(service.estado(ana.getId(), clinica.getId())).estado()).isEqualTo("SIN_REGISTRO");
        assertThat(service.usuariosQueOtorgaron(Set.of(ana.getId()), clinica.getId(),
                FinalidadDatos.RECORDATORIOS_PREVENTIVOS)).isEmpty();
    }

    @Test
    void cambiarDeOpinionAgregaConstanciasYLaUltimaManda() {
        publicarAviso(clinica, "Av. Los Olivos 123, Lima");
        service.registrarAlta(ana, clinica.getId(), true, recepcion.getId());

        service.cambiarFinalidad(ana.getId(), clinica.getId(), FinalidadDatos.RECORDATORIOS_PREVENTIVOS, false,
                CanalConsentimiento.PORTAL, null, "No quiero correos", "10.0.0.1", "navegador");
        assertThat(recordatorios(service.estado(ana.getId(), clinica.getId())).estado()).isEqualTo("RETIRADO");
        assertThat(service.usuariosQueOtorgaron(Set.of(ana.getId()), clinica.getId(),
                FinalidadDatos.RECORDATORIOS_PREVENTIVOS)).isEmpty();

        service.cambiarFinalidad(ana.getId(), clinica.getId(), FinalidadDatos.RECORDATORIOS_PREVENTIVOS, true,
                CanalConsentimiento.PORTAL, null, null, "10.0.0.1", "navegador");

        assertThat(recordatorios(service.estado(ana.getId(), clinica.getId())).estado()).isEqualTo("OTORGADO");
        assertThat(service.usuariosQueOtorgaron(Set.of(ana.getId()), clinica.getId(),
                FinalidadDatos.RECORDATORIOS_PREVENTIVOS)).containsExactly(ana.getId());
        List<ConsentimientoDatos> historial = consentimientoRepository
                .findByUsuarioIdAndCompanyIdOrderByIdDesc(ana.getId(), clinica.getId());
        assertThat(historial).hasSize(4);
        assertThat(historial.get(1).getMotivo()).isEqualTo("No quiero correos");
        assertThat(historial.get(1).getRegistradoPor()).isNull();
        assertThat(historial.get(1).getIpAddress()).isEqualTo("10.0.0.1");
    }

    @Test
    void repetirLaMismaDecisionNoAgregaNada() {
        publicarAviso(clinica, "Av. Los Olivos 123, Lima");
        service.registrarAlta(ana, clinica.getId(), true, recepcion.getId());
        long antes = consentimientoRepository.count();

        service.cambiarFinalidad(ana.getId(), clinica.getId(), FinalidadDatos.RECORDATORIOS_PREVENTIVOS, true,
                CanalConsentimiento.PORTAL, null, null, null, null);

        assertThat(consentimientoRepository.count()).isEqualTo(antes);
    }

    @Test
    void haberSidoInformadoNoEsUnConsentimientoQueSePuedaRetirar() {
        publicarAviso(clinica, "Av. Los Olivos 123, Lima");

        assertThatThrownBy(() -> service.cambiarFinalidad(ana.getId(), clinica.getId(), FinalidadDatos.ENTERADO, false,
                CanalConsentimiento.PORTAL, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nadieOperaSobreUnaPersonaDeOtraClinica() {
        publicarAviso(clinica, "Av. Los Olivos 123, Lima");
        publicarAviso(otraClinica, "Jr. Cusco 456, Lima");

        assertThatThrownBy(() -> service.estado(ana.getId(), otraClinica.getId())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.registrarEnterado(ana.getId(), otraClinica.getId(), CanalConsentimiento.PORTAL, null, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.cambiarFinalidad(ana.getId(), otraClinica.getId(),
                FinalidadDatos.RECORDATORIOS_PREVENTIVOS, false, CanalConsentimiento.PRESENCIAL, recepcion.getId(), null, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(consentimientoRepository.findByUsuarioIdAndCompanyIdOrderByIdDesc(
                ana.getId(), clinica.getId())).isEmpty();
    }

    @Test
    void unAvisoNuevoPideInformarDeNuevoYLaConstanciaVieneConLaVersionQueSeMostro() {
        publicarAviso(clinica, "Av. Los Olivos 123, Lima");
        service.registrarAlta(ana, clinica.getId(), null, recepcion.getId());

        publicarAviso(clinica, "Jr. Cusco 456, Lima");
        ConsentimientoEstadoResponse conAvisoNuevo = service.estado(ana.getId(), clinica.getId());
        assertThat(conAvisoNuevo.avisoVersion()).isEqualTo(2);
        assertThat(conAvisoNuevo.informadaVersion()).isEqualTo(1);

        service.registrarEnterado(ana.getId(), clinica.getId(), CanalConsentimiento.PORTAL, null, "10.0.0.1", "navegador");

        ConsentimientoEstadoResponse alDia = service.estado(ana.getId(), clinica.getId());
        assertThat(alDia.informadaVersion()).isEqualTo(2);
        assertThat(alDia.informadaCanal()).isEqualTo(CanalConsentimiento.PORTAL);
    }

    @Test
    void sinAvisoPublicadoRegistrarEnteradoNoHaceNada() {
        assertThat(service.registrarEnterado(ana.getId(), clinica.getId(), CanalConsentimiento.ACTIVACION, null, null, null)).isFalse();
        assertThat(consentimientoRepository.findByUsuarioIdAndCompanyIdOrderByIdDesc(
                ana.getId(), clinica.getId())).isEmpty();
        assertThat(service.estado(ana.getId(), clinica.getId()).avisoPublicado()).isFalse();
    }

    @Test
    void laListaDeInformadosSoloIncluyeAQuienesTienenConstancia() {
        publicarAviso(clinica, "Av. Los Olivos 123, Lima");
        service.registrarAlta(ana, clinica.getId(), null, recepcion.getId());

        assertThat(service.usuariosInformados(Set.of(ana.getId(), recepcion.getId()), clinica.getId()))
                .containsExactly(ana.getId());
        assertThat(service.usuariosInformados(Set.of(), clinica.getId())).isEmpty();
    }
}
