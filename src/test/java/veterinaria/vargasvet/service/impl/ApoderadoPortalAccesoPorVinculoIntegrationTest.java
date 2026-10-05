package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.MascotaPersonaRelacion;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.domain.enums.TipoRelacionMascota;
import veterinaria.vargasvet.dto.request.CitaRequest;
import veterinaria.vargasvet.dto.response.CitaResponse;
import veterinaria.vargasvet.dto.response.HistoriaClinicaDetalleResponse;
import veterinaria.vargasvet.dto.response.MascotaResponse;
import veterinaria.vargasvet.mapper.CitaMapper;
import veterinaria.vargasvet.mapper.ConsultaMapper;
import veterinaria.vargasvet.mapper.MascotaMapper;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyExceptionRepository;
import veterinaria.vargasvet.repository.CompanyOperatingHourRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.HorarioEmpleadoRepository;
import veterinaria.vargasvet.repository.MascotaPersonaRelacionRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.PrescripcionRepository;
import veterinaria.vargasvet.repository.ServiciosVeterinariosRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CitaService;
import veterinaria.vargasvet.service.HistoriaClinicaService;
import veterinaria.vargasvet.service.PetOwnershipService;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El portal entrega información y deja actuar sobre una mascota según el vínculo de quien entra: el principal todo,
 * un copropietario todo, un representante lo que se le autorizó y un responsable de pago solo la cuenta.
 */
@DataJpaTest
class ApoderadoPortalAccesoPorVinculoIntegrationTest {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private MascotaPersonaRelacionRepository relacionRepository;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

    private CitaRepository citaRepository;
    private PrescripcionRepository prescripcionRepository;
    private HistoriaClinicaService historiaClinicaService;
    private CitaService citaService;
    private ApoderadoPortalServiceImpl portal;

    private Company clinica;
    private Apoderado maria;
    private Apoderado carlos;
    private Apoderado rosa;
    private Apoderado luis;
    private Apoderado extrana;
    private Mascota luna;
    private Mascota rex;

    @BeforeEach
    void setUp() {
        citaRepository = mock(CitaRepository.class);
        prescripcionRepository = mock(PrescripcionRepository.class);
        historiaClinicaService = mock(HistoriaClinicaService.class);
        citaService = mock(CitaService.class);
        MascotaMapper mascotaMapper = mock(MascotaMapper.class);
        when(mascotaMapper.toResponse(any(Mascota.class))).thenAnswer(invocation -> {
            Mascota mascota = invocation.getArgument(0);
            MascotaResponse response = new MascotaResponse();
            response.setId(mascota.getId());
            response.setNombreCompleto(mascota.getNombreCompleto());
            response.setActivo(mascota.getActivo());
            response.setApoderadoId(mascota.getApoderado().getId());
            response.setApoderadoNombreCompleto("Propietaria Principal");
            return response;
        });
        CitaMapper citaMapper = mock(CitaMapper.class);
        when(citaMapper.toResponse(any(veterinaria.vargasvet.domain.entity.Cita.class))).thenAnswer(invocation -> {
            veterinaria.vargasvet.domain.entity.Cita cita = invocation.getArgument(0);
            CitaResponse response = new CitaResponse();
            response.setApoderadoId(cita.getMascota().getApoderado().getId());
            response.setApoderadoNombre("Propietaria Principal");
            response.setApoderadoEmail("principal@clinica.test");
            response.setTelefonoAviso("999999999");
            return response;
        });
        PetOwnershipService pets = new PetOwnershipService(mascotaRepository, relacionRepository, citaRepository,
                mock(AuditLogService.class));
        ReflectionTestUtils.setField(pets, "entityManager", entityManager);
        portal = new ApoderadoPortalServiceImpl(usuarioRepository, apoderadoRepository, mascotaRepository, citaRepository,
                prescripcionRepository, mock(ServiciosVeterinariosRepository.class), mock(EmpleadoRepository.class),
                mock(HorarioEmpleadoRepository.class), mock(CompanyOperatingHourRepository.class),
                mock(CompanyExceptionRepository.class), citaService, historiaClinicaService, mascotaMapper, citaMapper,
                mock(ConsultaMapper.class), mock(UsuarioContactoService.class), pets);

        clinica = new Company();
        clinica.setName("Clínica");
        clinica.setSlug("clinica-" + UUID.randomUUID());
        clinica.setActivo(true);
        clinica = companyRepository.saveAndFlush(clinica);
        maria = cliente("maria");
        carlos = cliente("carlos");
        rosa = cliente("rosa");
        luis = cliente("luis");
        extrana = cliente("extrana");
        luna = mascota("Luna", maria);
        rex = mascota("Rex", carlos);
        LocalDateTime ahora = AppClock.now();
        vinculo(luna, carlos, TipoRelacionMascota.COPROPIETARIO, true, true, true);
        vinculo(luna, rosa, TipoRelacionMascota.REPRESENTANTE_AUTORIZADO, true, false, false);
        vinculo(luna, luis, TipoRelacionMascota.RESPONSABLE_PAGO, false, false, true);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private Apoderado cliente(String prefijo) {
        Usuario usuario = new Usuario();
        usuario.setEmail(prefijo + "-" + UUID.randomUUID() + "@vargasvet.test");
        usuario.setUsername(prefijo + "-" + UUID.randomUUID());
        usuario.setNombre(prefijo);
        usuario.setApellido("Test");
        usuario.setDni(String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario = usuarioRepository.saveAndFlush(usuario);
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(usuario);
        apoderado.setCompany(clinica);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setNumeroDocumento(String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 8));
        apoderado.setGenero(Genero.FEMENINO);
        apoderado.setEstado(true);
        return apoderadoRepository.saveAndFlush(apoderado);
    }

    private Mascota mascota(String nombre, Apoderado principal) {
        Mascota mascota = new Mascota();
        mascota.setNombreCompleto(nombre);
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(principal);
        mascota.setUuid(UUID.randomUUID().toString());
        return mascotaRepository.saveAndFlush(mascota);
    }

    private void vinculo(Mascota mascota, Apoderado persona, TipoRelacionMascota tipo,
                         boolean informacion, boolean autorizar, boolean pagos) {
        MascotaPersonaRelacion relacion = new MascotaPersonaRelacion();
        relacion.setMascota(mascota);
        relacion.setApoderado(persona);
        relacion.setCompany(clinica);
        relacion.setTipoRelacion(tipo);
        relacion.setPuedeRecibirInformacion(informacion);
        relacion.setPuedeAutorizarAtencion(autorizar);
        relacion.setPuedeRealizarPagos(pagos);
        relacion.setFechaInicio(AppClock.today().minusDays(5));
        relacion.setActivo(true);
        relacion.setCreatedBy("test");
        relacion.setUpdatedBy("test");
        relacionRepository.saveAndFlush(relacion);
    }

    private void entrarComo(Apoderado persona) {
        UsuarioPrincipal principal = new UsuarioPrincipal(persona.getUser().getId(), persona.getUser().getEmail(), "", List.of(),
                clinica.getId(), 5, RoleScope.CLIENT, RolePurpose.CLIENT_PORTAL, 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void laListaDeMascotasIncluyeLasPropiasYLasDeSusVinculosConInformacionOAutorizacion() {
        entrarComo(carlos);
        assertThat(portal.getMascotas()).extracting(MascotaResponse::getNombreCompleto).containsExactly("Luna", "Rex");

        entrarComo(rosa);
        assertThat(portal.getMascotas()).extracting(MascotaResponse::getNombreCompleto).containsExactly("Luna");

        entrarComo(luis);
        assertThat(portal.getMascotas()).isEmpty();

        entrarComo(extrana);
        assertThat(portal.getMascotas()).isEmpty();

        entrarComo(maria);
        assertThat(portal.getMascotas()).extracting(MascotaResponse::getNombreCompleto).containsExactly("Luna");
    }

    @Test
    void quienNoEsElPropietarioPrincipalNoVeSusDatosPersonales() {
        entrarComo(carlos);
        MascotaResponse deCarlos = portal.getMascotas().stream().filter(m -> m.getNombreCompleto().equals("Luna")).findFirst().orElseThrow();
        assertThat(deCarlos.getApoderadoId()).isNull();
        assertThat(deCarlos.getApoderadoNombreCompleto()).isNull();
        MascotaResponse propia = portal.getMascotas().stream().filter(m -> m.getNombreCompleto().equals("Rex")).findFirst().orElseThrow();
        assertThat(propia.getApoderadoNombreCompleto()).isNotNull();

        entrarComo(maria);
        assertThat(portal.getMascotas().get(0).getApoderadoNombreCompleto()).isEqualTo("Propietaria Principal");
    }

    @Test
    void lasMascotasPaginadasSiguenLaMismaRegla() {
        entrarComo(rosa);
        Page<MascotaResponse> pagina = portal.getMascotasPaginated(null, null, null, PageRequest.of(0, 10));
        assertThat(pagina.getContent()).extracting(MascotaResponse::getNombreCompleto).containsExactly("Luna");

        entrarComo(luis);
        assertThat(portal.getMascotasPaginated(null, null, null, PageRequest.of(0, 10)).getContent()).isEmpty();
    }

    @Test
    void laHistoriaClinicaSoloLaVeQuienPuedeRecibirInformacionYSinDatosDelPrincipal() {
        HistoriaClinicaDetalleResponse historia = new HistoriaClinicaDetalleResponse();
        historia.setApoderadoId(maria.getId());
        historia.setPropietarioNombre("Maria Test");
        historia.setPropietarioTelefono("999111222");
        historia.setPropietarioDireccion("Av. Siempre Viva 123");
        when(historiaClinicaService.getPorMascota(luna.getId())).thenReturn(historia);

        entrarComo(luis);
        assertThatThrownBy(() -> portal.getHistoriaMascota(luna.getId())).isInstanceOf(AccessDeniedException.class);
        entrarComo(extrana);
        assertThatThrownBy(() -> portal.getHistoriaMascota(luna.getId())).isInstanceOf(AccessDeniedException.class);

        entrarComo(rosa);
        HistoriaClinicaDetalleResponse paraRosa = portal.getHistoriaMascota(luna.getId());
        assertThat(paraRosa.getPropietarioNombre()).isNull();
        assertThat(paraRosa.getPropietarioTelefono()).isNull();
        assertThat(paraRosa.getPropietarioDireccion()).isNull();
        assertThat(paraRosa.getApoderadoId()).isNull();
    }

    @Test
    void lasCitasYLasRecetasSeBuscanSoloEntreLasMascotasConElPermiso() {
        when(citaRepository.findByMascota_IdInAndEliminadaFalseOrderByFechaHoraInicioDesc(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(prescripcionRepository.findByMascotaIds(any())).thenReturn(List.of());

        entrarComo(rosa);
        portal.getCitas(null, PageRequest.of(0, 10));
        portal.getRecetas();
        entrarComo(luis);
        portal.getRecetas();

        @SuppressWarnings("unchecked") ArgumentCaptor<Collection<Long>> ids = ArgumentCaptor.forClass(Collection.class);
        verify(citaRepository).findByMascota_IdInAndEliminadaFalseOrderByFechaHoraInicioDesc(ids.capture(), any());
        assertThat(ids.getValue()).containsExactly(luna.getId());
        @SuppressWarnings("unchecked") ArgumentCaptor<Collection<Long>> recetas = ArgumentCaptor.forClass(Collection.class);
        verify(prescripcionRepository, org.mockito.Mockito.times(2)).findByMascotaIds(recetas.capture());
        assertThat(recetas.getAllValues().get(0)).containsExactly(luna.getId());
        assertThat(recetas.getAllValues().get(1)).containsExactly(-1L);
    }

    @Test
    void lasCitasDeUnaMascotaAjenaOSinPermisoSeRechazan() {
        entrarComo(luis);
        assertThatThrownBy(() -> portal.getCitas(luna.getId(), PageRequest.of(0, 10))).isInstanceOf(AccessDeniedException.class);
        entrarComo(extrana);
        assertThatThrownBy(() -> portal.getCitas(luna.getId(), PageRequest.of(0, 10))).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void soloQuienPuedeAutorizarAtencionAgendaCitas() {
        CitaRequest request = new CitaRequest();
        request.setMascotaId(luna.getId());
        request.setFechaHoraInicio(AppClock.now().plusDays(3));
        when(citaRepository.findActiveByApoderadoIdAndFecha(anyLong(), any())).thenReturn(List.of());
        when(citaService.createCita(any(CitaRequest.class))).thenReturn(new CitaResponse());

        entrarComo(rosa);
        assertThatThrownBy(() -> portal.createPortalCita(request)).isInstanceOf(AccessDeniedException.class);
        entrarComo(luis);
        assertThatThrownBy(() -> portal.createPortalCita(request)).isInstanceOf(AccessDeniedException.class);
        verify(citaService, never()).createCita(any());

        entrarComo(carlos);
        portal.createPortalCita(request);
        verify(citaService).createCita(request);
        verify(citaRepository).findActiveByApoderadoIdAndFecha(org.mockito.ArgumentMatchers.eq(maria.getId()), any());
    }

    @Test
    void unVinculoRevocadoPierdeElAccesoAlInstante() {
        entrarComo(carlos);
        assertThat(portal.getMascotas()).hasSize(2);

        MascotaPersonaRelacion relacion = relacionRepository.findAllByMascotaIdAndApoderadoIdAndActivoTrue(luna.getId(), carlos.getId()).get(0);
        relacion.setActivo(false);
        relacionRepository.saveAndFlush(relacion);

        assertThat(portal.getMascotas()).extracting(MascotaResponse::getNombreCompleto).containsExactly("Rex");
    }
}
