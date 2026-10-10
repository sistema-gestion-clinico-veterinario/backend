package veterinaria.vargasvet.ers.preventivos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.ControlPreventivo;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.MascotaPersonaRelacion;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoControlPreventivo;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.domain.enums.TipoInactividad;
import veterinaria.vargasvet.domain.enums.TipoRelacionMascota;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.AvisoPrivacidadRepository;
import veterinaria.vargasvet.repository.ConsentimientoDatosRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.ControlPreventivoRepository;
import veterinaria.vargasvet.repository.MascotaPersonaRelacionRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.RecordatorioPreventivoRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CompanyMembershipService;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.OwnerContactPolicy;
import veterinaria.vargasvet.service.PetOwnershipService;
import veterinaria.vargasvet.service.impl.RecordatorioPreventivoServiceImpl;
import veterinaria.vargasvet.service.impl.UsuarioContactoService;
import veterinaria.vargasvet.util.AppClock;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Los recordatorios llegan a quien tiene un vínculo vigente con permiso de recibir información, y a nadie más. */
@DataJpaTest
class RecordatorioPreventivoDestinatariosIntegrationTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private MascotaPersonaRelacionRepository relacionRepository;
    @Autowired private ControlPreventivoRepository controlRepository;
    @Autowired private RecordatorioPreventivoRepository recordatorioRepository;
    @Autowired private AvisoPrivacidadRepository avisoRepository;
    @Autowired private ConsentimientoDatosRepository consentimientoRepository;

    private EmailService emailService;
    private RecordatorioPreventivoServiceImpl service;
    private Company clinica;
    private veterinaria.vargasvet.service.ConsentimientoDatosService consentimientos;

    @BeforeEach
    void setUp() {
        emailService = mock(EmailService.class);
        when(emailService.createMail(anyString(), anyString(), anyMap())).thenReturn(new Mail());
        when(emailService.sendEmail(any(), eq("email/recordatorio-preventivo-template")))
                .thenReturn(CompletableFuture.completedFuture(true));
        clinica = new Company();
        clinica.setName("Clínica recordatorios");
        clinica.setSlug("clinica-" + UUID.randomUUID());
        clinica.setRuc("20" + System.nanoTime());
        clinica.setActivo(true);
        clinica = companyRepository.saveAndFlush(clinica);
        CompanyMembershipService membershipService = mock(CompanyMembershipService.class);
        when(membershipService.hasAnyMembership(any(Integer.class), any(Integer.class)))
                .thenAnswer(invocation -> apoderadoRepository.existsByUserIdAndCompanyId(
                        invocation.getArgument(0), invocation.getArgument(1)));
        consentimientos = new veterinaria.vargasvet.service.ConsentimientoDatosService(consentimientoRepository, avisoRepository,
                usuarioRepository, apoderadoRepository, mock(AuditLogService.class), membershipService,
                mock(veterinaria.vargasvet.service.AvisoPrivacidadEntregaService.class));
        service = new RecordatorioPreventivoServiceImpl(controlRepository, recordatorioRepository, emailService,
                new OwnerContactPolicy(mock(UsuarioContactoService.class)),
                new PetOwnershipService(mascotaRepository, relacionRepository, mock(CitaRepository.class), mock(AuditLogService.class)),
                consentimientos);
        publicarAviso();
    }

    private Apoderado cliente(String prefijo) {
        Usuario usuario = new Usuario();
        usuario.setEmail(prefijo + "-" + UUID.randomUUID() + "@example.test");
        usuario.setUsername(prefijo + "-" + UUID.randomUUID());
        usuario.setNombre(prefijo);
        usuario.setApellido("Test");
        usuario.setCompany(clinica);
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario = usuarioRepository.saveAndFlush(usuario);
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(usuario);
        apoderado.setCompany(clinica);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setNumeroDocumento("9" + System.nanoTime());
        apoderado.setGenero(Genero.FEMENINO);
        return apoderadoRepository.saveAndFlush(apoderado);
    }

    private void vincular(Mascota mascota, Apoderado persona, TipoRelacionMascota tipo, boolean informacion, boolean pagos) {
        MascotaPersonaRelacion relacion = new MascotaPersonaRelacion();
        relacion.setMascota(mascota);
        relacion.setApoderado(persona);
        relacion.setCompany(clinica);
        relacion.setTipoRelacion(tipo);
        relacion.setPuedeRecibirInformacion(informacion);
        relacion.setPuedeAutorizarAtencion(false);
        relacion.setPuedeRealizarPagos(pagos);
        relacion.setFechaInicio(AppClock.today().minusDays(3));
        relacion.setActivo(true);
        relacion.setCreatedBy("test");
        relacion.setUpdatedBy("test");
        relacionRepository.saveAndFlush(relacion);
    }

    private Mascota mascotaConControl(Apoderado principal) {
        Mascota mascota = new Mascota();
        mascota.setNombreCompleto("Luna");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(principal);
        mascota.setUuid(UUID.randomUUID().toString());
        mascota = mascotaRepository.saveAndFlush(mascota);
        ControlPreventivo control = new ControlPreventivo();
        control.setMascota(mascota);
        control.setTipo(TipoControlPreventivo.VACUNACION);
        control.setNombreControl("Antirrábica");
        control.setFechaRecomendada(AppClock.today().plusDays(3));
        control.setEstado(EstadoControlPreventivo.PROXIMO);
        control.setCreatedBy("test");
        control.setUpdatedBy("test");
        controlRepository.saveAndFlush(control);
        return mascota;
    }

    private List<String> destinatariosDeLosCorreos() {
        ArgumentCaptor<String> destinatario = ArgumentCaptor.forClass(String.class);
        verify(emailService, org.mockito.Mockito.atLeast(0)).createMail(destinatario.capture(), anyString(), anyMap());
        return destinatario.getAllValues();
    }

    @Test
    void elRecordatorioLlegaAlPrincipalYALosQuePuedenRecibirInformacionPeroNoAlResponsableDePago() {
        Apoderado maria = cliente("maria");
        Apoderado carlos = cliente("carlos");
        Apoderado luis = cliente("luis");
        Mascota luna = mascotaConControl(maria);
        vincular(luna, carlos, TipoRelacionMascota.COPROPIETARIO, true, true);
        vincular(luna, luis, TipoRelacionMascota.RESPONSABLE_PAGO, false, true);
        decidirRecordatorios(maria, true);
        decidirRecordatorios(carlos, true);

        service.procesarRecordatorios();
        recordatorioRepository.flush();

        assertThat(destinatariosDeLosCorreos()).containsExactlyInAnyOrder(maria.getUser().getEmail(), carlos.getUser().getEmail());
        assertThat(recordatorioRepository.findAll()).extracting(r -> r.getApoderado().getId())
                .containsExactlyInAnyOrder(maria.getId(), carlos.getId());

        service.procesarRecordatorios();
        recordatorioRepository.flush();
        verify(emailService, times(2)).sendEmail(any(), eq("email/recordatorio-preventivo-template"));
        assertThat(recordatorioRepository.count()).isEqualTo(2L);
    }

    @Test
    void unPrincipalDadoDeBajaNoRecibeElRecordatorioPeroSiQuienSigueVinculado() {
        Apoderado maria = cliente("maria");
        Apoderado carlos = cliente("carlos");
        Mascota luna = mascotaConControl(maria);
        vincular(luna, carlos, TipoRelacionMascota.COPROPIETARIO, true, true);
        decidirRecordatorios(carlos, true);
        maria.setEstado(false);
        maria.setTipoInactividad(TipoInactividad.BAJA);
        apoderadoRepository.saveAndFlush(maria);

        service.procesarRecordatorios();

        assertThat(destinatariosDeLosCorreos()).containsExactly(carlos.getUser().getEmail());
    }

    @Test
    void sinNadieActivoConVinculoNoSeEnviaNada() {
        Apoderado maria = cliente("maria");
        mascotaConControl(maria);
        maria.setEstado(false);
        maria.setTipoInactividad(TipoInactividad.SUSPENSION);
        apoderadoRepository.saveAndFlush(maria);

        service.procesarRecordatorios();

        verify(emailService, never()).sendEmail(any(), anyString());
    }

    @Test
    void unVinculoRevocadoNoRecibeElRecordatorio() {
        Apoderado maria = cliente("maria");
        Apoderado carlos = cliente("carlos");
        Mascota luna = mascotaConControl(maria);
        vincular(luna, carlos, TipoRelacionMascota.COPROPIETARIO, true, true);
        MascotaPersonaRelacion relacion = relacionRepository.findAllByMascotaIdAndApoderadoIdAndActivoTrue(luna.getId(), carlos.getId()).get(0);
        relacion.setActivo(false);
        relacionRepository.saveAndFlush(relacion);
        decidirRecordatorios(maria, true);

        service.procesarRecordatorios();

        assertThat(destinatariosDeLosCorreos()).containsExactly(maria.getUser().getEmail());
    }

    private void publicarAviso() {
        veterinaria.vargasvet.dto.request.CamposAvisoPrivacidad campos = new veterinaria.vargasvet.dto.request.CamposAvisoPrivacidad(
                "Clínica S.A.C.", "20123456789", "Av. Los Olivos 123, Lima", "privacidad@clinica.test", null, null,
                List.of("Atender a su mascota"), List.of("Nombres y apellidos"), List.of(), List.of("El personal de la clínica"),
                "No se transfieren datos fuera del Perú", "Mientras sea cliente");
        veterinaria.vargasvet.dto.request.PublicarAvisoPrivacidadRequest request = new veterinaria.vargasvet.dto.request.PublicarAvisoPrivacidadRequest();
        request.setCampos(campos);
        request.setConfirmoRevisionLegal(true);
        new veterinaria.vargasvet.service.AvisoPrivacidadService(avisoRepository, companyRepository, mock(AuditLogService.class),
                new com.fasterxml.jackson.databind.ObjectMapper()).publicar(clinica.getId(), request);
    }

    private void decidirRecordatorios(Apoderado persona, boolean acepta) {
        consentimientos.cambiarFinalidad(persona.getUser().getId(), clinica.getId(),
                veterinaria.vargasvet.domain.enums.FinalidadDatos.RECORDATORIOS_PREVENTIVOS, acepta,
                veterinaria.vargasvet.domain.enums.CanalConsentimiento.PRESENCIAL, null, null, null, null);
    }

    @Test
    void soloQuienAutorizoLosRecordatoriosLosRecibe() {
        Apoderado maria = cliente("maria");
        Apoderado carlos = cliente("carlos");
        Apoderado rosa = cliente("rosa");
        Mascota luna = mascotaConControl(maria);
        vincular(luna, carlos, TipoRelacionMascota.COPROPIETARIO, true, true);
        vincular(luna, rosa, TipoRelacionMascota.REPRESENTANTE_AUTORIZADO, true, false);
        decidirRecordatorios(maria, false);
        decidirRecordatorios(rosa, true);

        service.procesarRecordatorios();
        recordatorioRepository.flush();

        assertThat(destinatariosDeLosCorreos()).containsExactly(rosa.getUser().getEmail());
        assertThat(recordatorioRepository.findAll()).extracting(r -> r.getApoderado().getId())
                .containsExactly(rosa.getId());
    }

    @Test
    void volverAPedirlosRestableceLosRecordatorios() {
        Apoderado maria = cliente("maria");
        mascotaConControl(maria);
        decidirRecordatorios(maria, false);
        decidirRecordatorios(maria, true);

        service.procesarRecordatorios();

        assertThat(destinatariosDeLosCorreos()).containsExactly(maria.getUser().getEmail());
    }

    @Test
    void siElUnicoDestinatarioPidioNoRecibirlosNoSeEnviaNada() {
        Apoderado maria = cliente("maria");
        mascotaConControl(maria);
        decidirRecordatorios(maria, false);

        service.procesarRecordatorios();

        verify(emailService, never()).sendEmail(any(), anyString());
        assertThat(recordatorioRepository.count()).isZero();
    }

    @Test
    void sinNingunaDecisionNoSeEnviaElRecordatorio() {
        Apoderado maria = cliente("maria");
        mascotaConControl(maria);

        service.procesarRecordatorios();

        verify(emailService, never()).sendEmail(any(), anyString());
        assertThat(recordatorioRepository.count()).isZero();
    }
}
