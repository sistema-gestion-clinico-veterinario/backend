package veterinaria.vargasvet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.CanalConsentimiento;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.FinalidadDatos;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.dto.request.CamposAvisoPrivacidad;
import veterinaria.vargasvet.dto.request.PublicarAvisoPrivacidadRequest;
import veterinaria.vargasvet.dto.response.AutorizacionIaResponse;
import veterinaria.vargasvet.exception.AutorizacionIaRequeridaException;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.AvisoPrivacidadRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.ConsentimientoDatosRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest
class AutorizacionIaServiceIntegrationTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private AvisoPrivacidadRepository avisoRepository;
    @Autowired private ConsentimientoDatosRepository consentimientoRepository;

    private AuditLogService auditLogService;
    private ConsentimientoDatosService consentimientos;
    private AutorizacionIaService service;
    private Company clinicaA;
    private Company clinicaB;
    private Apoderado ana;
    private Apoderado luis;
    private Mascota luna;
    private Mascota rex;

    @BeforeEach
    void setUp() {
        auditLogService = mock(AuditLogService.class);
        CompanyMembershipService membership = mock(CompanyMembershipService.class);
        when(membership.hasAnyMembership(anyInt(), anyInt())).thenAnswer(invocation ->
                apoderadoRepository.existsByUserIdAndCompanyId(invocation.getArgument(0), invocation.getArgument(1)));
        consentimientos = new ConsentimientoDatosService(consentimientoRepository, avisoRepository, usuarioRepository,
                apoderadoRepository, mock(AuditLogService.class), membership,
                mock(AvisoPrivacidadEntregaService.class));
        service = new AutorizacionIaService(mascotaRepository, consentimientoRepository, auditLogService);

        clinicaA = empresa("Clínica A");
        clinicaB = empresa("Clínica B");
        publicarAviso(clinicaA);
        publicarAviso(clinicaB);
        ana = cliente("ana", clinicaA);
        luis = cliente("luis", clinicaB);
        luna = mascota("Luna", ana);
        rex = mascota("Rex", luis);
        sesionEn(clinicaA);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void sesionEn(Company empresa) {
        UsuarioPrincipal principal = new UsuarioPrincipal(1, "vet@example.test", "", List.of(), empresa.getId(), 2,
                RoleScope.STAFF, RolePurpose.CUSTOM, 0L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Company empresa(String nombre) {
        Company company = new Company();
        company.setName(nombre);
        company.setSlug("clinica-" + UUID.randomUUID());
        company.setRuc("20" + System.nanoTime());
        company.setActivo(true);
        return companyRepository.saveAndFlush(company);
    }

    private void publicarAviso(Company empresa) {
        AvisoPrivacidadService avisoService = new AvisoPrivacidadService(avisoRepository, companyRepository,
                mock(AuditLogService.class), new ObjectMapper());
        PublicarAvisoPrivacidadRequest request = new PublicarAvisoPrivacidadRequest();
        request.setCampos(new CamposAvisoPrivacidad("Clínica S.A.C.", "20123456789", "Av. Los Olivos 123",
                "privacidad@clinica.test", null, null, List.of("Atender a su mascota"), List.of("Nombres y apellidos"),
                List.of(), List.of("El personal de la clínica"), "No se transfieren datos fuera del Perú",
                "Mientras sea cliente"));
        request.setConfirmoRevisionLegal(true);
        avisoService.publicar(empresa.getId(), request);
    }

    private Apoderado cliente(String prefijo, Company empresa) {
        Usuario usuario = new Usuario();
        usuario.setEmail(prefijo + "-" + UUID.randomUUID() + "@example.test");
        usuario.setUsername(prefijo + "-" + UUID.randomUUID());
        usuario.setNombre(prefijo);
        usuario.setApellido("Pérez");
        usuario.setCompany(empresa);
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario = usuarioRepository.saveAndFlush(usuario);
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(usuario);
        apoderado.setCompany(empresa);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setNumeroDocumento("9" + System.nanoTime());
        apoderado.setGenero(Genero.FEMENINO);
        return apoderadoRepository.saveAndFlush(apoderado);
    }

    private Mascota mascota(String nombre, Apoderado titular) {
        Mascota mascota = new Mascota();
        mascota.setNombreCompleto(nombre);
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(titular);
        mascota.setUuid(UUID.randomUUID().toString());
        return mascotaRepository.saveAndFlush(mascota);
    }

    private void decidir(Apoderado titular, Company empresa, FinalidadDatos finalidad, boolean otorgar, CanalConsentimiento canal) {
        consentimientos.cambiarFinalidad(titular.getUser().getId(), empresa.getId(), finalidad, otorgar, canal,
                null, null, null, null);
    }

    @Test
    void sinConstanciaNoHayAutorizacionAunqueElClienteSeaDeLaClinica() {
        AutorizacionIaResponse estado = service.estado(luna.getId());

        assertThat(estado.autorizada()).isFalse();
        assertThat(estado.apoderadoId()).isEqualTo(ana.getId());
        assertThatThrownBy(() -> service.exigir(luna.getId()))
                .isInstanceOfSatisfying(AutorizacionIaRequeridaException.class,
                        ex -> assertThat(ex.getApoderadoId()).isEqualTo(ana.getId()));
    }

    @Test
    void conLaAutorizacionDelTitularSeConoceQuienCuandoYPorQueCanal() {
        decidir(ana, clinicaA, FinalidadDatos.USO_IA_CLINICA, true, CanalConsentimiento.PORTAL);

        AutorizacionIaResponse estado = service.exigir(luna.getId());

        assertThat(estado.autorizada()).isTrue();
        assertThat(estado.titular()).isEqualTo("ana Pérez");
        assertThat(estado.canal()).isEqualTo(CanalConsentimiento.PORTAL);
        assertThat(estado.fecha()).isNotNull();
    }

    @Test
    void retirarLaAutorizacionBloqueaLasSolicitudesSiguientes() {
        decidir(ana, clinicaA, FinalidadDatos.USO_IA_CLINICA, true, CanalConsentimiento.PORTAL);
        service.exigir(luna.getId());

        decidir(ana, clinicaA, FinalidadDatos.USO_IA_CLINICA, false, CanalConsentimiento.PORTAL);

        assertThatThrownBy(() -> service.exigir(luna.getId())).isInstanceOf(AutorizacionIaRequeridaException.class);
        assertThat(service.estado(luna.getId()).fecha()).isNull();
    }

    @Test
    void volverAAutorizarDejaTodoElHistorialYVuelveAPermitir() {
        decidir(ana, clinicaA, FinalidadDatos.USO_IA_CLINICA, true, CanalConsentimiento.PORTAL);
        decidir(ana, clinicaA, FinalidadDatos.USO_IA_CLINICA, false, CanalConsentimiento.PORTAL);
        decidir(ana, clinicaA, FinalidadDatos.USO_IA_CLINICA, true, CanalConsentimiento.PRESENCIAL);

        assertThat(service.exigir(luna.getId()).canal()).isEqualTo(CanalConsentimiento.PRESENCIAL);
        assertThat(consentimientoRepository.findAll().stream()
                .filter(c -> c.getFinalidad() == FinalidadDatos.USO_IA_CLINICA)).hasSize(3);
    }

    @Test
    void laAutorizacionNoDependeDeOtrasDecisionesNiDeHaberSidoInformado() {
        decidir(ana, clinicaA, FinalidadDatos.RECORDATORIOS_PREVENTIVOS, true, CanalConsentimiento.PORTAL);
        consentimientos.registrarEnterado(ana.getUser().getId(), clinicaA.getId(), CanalConsentimiento.PORTAL, null, null, null);

        assertThat(service.estado(luna.getId()).autorizada()).isFalse();
    }

    @Test
    void laAutorizacionDeUnClienteNoValeParaLasMascotasDeOtro() {
        decidir(ana, clinicaA, FinalidadDatos.USO_IA_CLINICA, true, CanalConsentimiento.PORTAL);
        Mascota otraDeLaMismaClinica = mascota("Nala", cliente("beto", clinicaA));

        assertThat(service.estado(luna.getId()).autorizada()).isTrue();
        assertThat(service.estado(otraDeLaMismaClinica.getId()).autorizada()).isFalse();
    }

    @Test
    void nadaDeOtraClinicaSeVeNiSeAutoriza() {
        decidir(luis, clinicaB, FinalidadDatos.USO_IA_CLINICA, true, CanalConsentimiento.PORTAL);

        assertThatThrownBy(() -> service.estado(rex.getId())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.exigir(rex.getId())).isInstanceOf(ResourceNotFoundException.class);

        sesionEn(clinicaB);
        assertThat(service.exigir(rex.getId()).autorizada()).isTrue();
        assertThatThrownBy(() -> service.exigir(luna.getId())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void laFinalidadAparecePorDefectoSinRegistroEnElEstadoDelCliente() {
        var estado = consentimientos.estado(ana.getUser().getId(), clinicaA.getId());

        assertThat(estado.finalidades()).anySatisfy(f -> {
            assertThat(f.codigo()).isEqualTo("USO_IA_CLINICA");
            assertThat(f.estado()).isEqualTo("SIN_REGISTRO");
        });
    }

    @Test
    void sinClinicaEnLaSesionNoSeResuelveNada() {
        SecurityContextHolder.clearContext();
        UsuarioPrincipal plataforma = new UsuarioPrincipal(1, "root@example.test", "", List.of(), null, 1,
                RoleScope.PLATFORM, RolePurpose.PLATFORM_ADMIN, 0L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(plataforma, null, plataforma.getAuthorities()));

        assertThatThrownBy(() -> service.estado(luna.getId())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cadaUsoQuedaEnLaAuditoriaConLaAutorizacionQueLoAmparo() {
        decidir(ana, clinicaA, FinalidadDatos.USO_IA_CLINICA, true, CanalConsentimiento.PORTAL);
        AutorizacionIaResponse autorizacion = service.exigir(luna.getId());

        service.registrarUso(luna.getId(), "laboratorio", autorizacion);

        verify(auditLogService).log(eq("USAR_IA_CLINICA"), eq("Inteligencia artificial"), contains("laboratorio"));
        verify(auditLogService).log(eq("USAR_IA_CLINICA"), eq("Inteligencia artificial"), contains("ana Pérez"));
    }
}
