package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
import veterinaria.vargasvet.dto.request.MascotaRelacionRequest;
import veterinaria.vargasvet.dto.response.MascotaRelacionResponse;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.MascotaPersonaRelacionRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.ApoderadoService;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.PetLinkNotifier;
import veterinaria.vargasvet.service.PetOwnershipService;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * El vínculo de una persona con una mascota tiene inicio, vigencia y revocación, cada periodo queda como su propia
 * fila, y cada cambio deja en la auditoría qué permisos había y cuáles quedaron.
 */
@DataJpaTest
class MascotaRelacionVigenciaIntegrationTest {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private MascotaPersonaRelacionRepository relacionRepository;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

    private AuditLogService auditLogService;
    private PetLinkNotifier petLinkNotifier;
    private ApoderadoService apoderadoService;
    private MascotaRelacionServiceImpl service;
    private Company clinica;
    private Apoderado maria;
    private Apoderado carlos;
    private Mascota luna;

    @BeforeEach
    void setUp() {
        auditLogService = mock(AuditLogService.class);
        petLinkNotifier = mock(PetLinkNotifier.class);
        apoderadoService = mock(ApoderadoService.class);
        PetOwnershipService pets = new PetOwnershipService(mascotaRepository, relacionRepository,
                mock(CitaRepository.class), auditLogService);
        ReflectionTestUtils.setField(pets, "entityManager", entityManager);
        service = new MascotaRelacionServiceImpl(relacionRepository, mascotaRepository, apoderadoRepository,
                auditLogService, pets, petLinkNotifier, apoderadoService);

        clinica = new Company();
        clinica.setName("Clínica");
        clinica.setSlug("clinica-" + UUID.randomUUID());
        clinica.setActivo(true);
        clinica = companyRepository.saveAndFlush(clinica);
        maria = cliente("maria");
        carlos = cliente("carlos");
        luna = new Mascota();
        luna.setNombreCompleto("Luna");
        luna.setEspecie(EspecieMascota.PERRO);
        luna.setApoderado(maria);
        luna.setUuid(UUID.randomUUID().toString());
        luna = mascotaRepository.saveAndFlush(luna);

        UsuarioPrincipal principal = new UsuarioPrincipal(99, "recepcion@clinica.test", "", List.of(), clinica.getId(),
                2, RoleScope.STAFF, RolePurpose.COMPANY_ADMIN, 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
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

    private MascotaRelacionRequest pedido(Apoderado persona, TipoRelacionMascota tipo) {
        MascotaRelacionRequest request = new MascotaRelacionRequest();
        request.setApoderadoId(persona.getId());
        request.setTipoRelacion(tipo);
        request.setPuedeRecibirInformacion(false);
        request.setPuedeAutorizarAtencion(false);
        request.setPuedeRealizarPagos(false);
        return request;
    }

    @Test
    void unVinculoConInicioFuturoQuedaPorEmpezarYNoDaPermisosHastaEntonces() {
        MascotaRelacionRequest request = pedido(carlos, TipoRelacionMascota.COPROPIETARIO);
        request.setFechaInicio(AppClock.today().plusDays(3));

        MascotaRelacionResponse respuesta = service.crear(luna.getUuid(), request);

        assertThat(respuesta.getPorEmpezar()).isTrue();
        assertThat(respuesta.getActivo()).isFalse();
        assertThat(respuesta.getFechaInicio()).isEqualTo(AppClock.today().plusDays(3));
        assertThat(relacionRepository.existsVigenteDeLaPersona(carlos.getId(), clinica.getId(), AppClock.today())).isFalse();
        assertThat(relacionRepository.existsVigenteDeLaPersona(carlos.getId(), clinica.getId(), AppClock.today().plusDays(3))).isTrue();
    }

    @Test
    void laFechaDeInicioNoPuedeSerPasadaNiLaDeFinAnteriorAlInicio() {
        MascotaRelacionRequest pasada = pedido(carlos, TipoRelacionMascota.COPROPIETARIO);
        pasada.setFechaInicio(AppClock.today().minusDays(1));
        assertThatThrownBy(() -> service.crear(luna.getUuid(), pasada))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("fecha de inicio");

        MascotaRelacionRequest invertida = pedido(carlos, TipoRelacionMascota.COPROPIETARIO);
        invertida.setFechaInicio(AppClock.today().plusDays(5));
        invertida.setFechaFin(AppClock.today().plusDays(2));
        assertThatThrownBy(() -> service.crear(luna.getUuid(), invertida))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("fecha de fin");
    }

    @Test
    void noSePuedeVincularALaPropietariaPrincipalNiDosVecesALaMismaPersona() {
        assertThatThrownBy(() -> service.crear(luna.getUuid(), pedido(maria, TipoRelacionMascota.COPROPIETARIO)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("propietaria principal");

        service.crear(luna.getUuid(), pedido(carlos, TipoRelacionMascota.COPROPIETARIO));
        assertThatThrownBy(() -> service.crear(luna.getUuid(), pedido(carlos, TipoRelacionMascota.RESPONSABLE_PAGO)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ya tiene una relación activa");
    }

    @Test
    void revincularConservaElPeriodoAnteriorComoOtraFila() {
        MascotaRelacionResponse primera = service.crear(luna.getUuid(), pedido(carlos, TipoRelacionMascota.COPROPIETARIO));
        service.revocar(luna.getUuid(), primera.getUuid());

        MascotaRelacionResponse segunda = service.crear(luna.getUuid(), pedido(carlos, TipoRelacionMascota.RESPONSABLE_PAGO));

        List<MascotaPersonaRelacion> periodos = relacionRepository
                .findByMascotaUuidAndCompanyId(luna.getUuid(), clinica.getId()).stream()
                .filter(r -> r.getApoderado().getId().equals(carlos.getId())).toList();
        assertThat(periodos).hasSize(2);
        assertThat(periodos).filteredOn(r -> r.getUuid().equals(primera.getUuid())).singleElement()
                .satisfies(r -> {
                    assertThat(r.getActivo()).isFalse();
                    assertThat(r.getRevokedAt()).isNotNull();
                    assertThat(r.getTipoRelacion()).isEqualTo(TipoRelacionMascota.COPROPIETARIO);
                });
        assertThat(segunda.getUuid()).isNotEqualTo(primera.getUuid());
        assertThat(segunda.getActivo()).isTrue();
    }

    @Test
    void unVinculoVencidoSinCerrarSeCierraAlVolverAVincularYQuedaEnLaAuditoria() {
        MascotaPersonaRelacion vencida = new MascotaPersonaRelacion();
        vencida.setMascota(luna);
        vencida.setApoderado(carlos);
        vencida.setCompany(clinica);
        vencida.setTipoRelacion(TipoRelacionMascota.COPROPIETARIO);
        vencida.setPuedeRecibirInformacion(true);
        vencida.setPuedeAutorizarAtencion(true);
        vencida.setPuedeRealizarPagos(true);
        vencida.setFechaInicio(AppClock.today().minusDays(20));
        vencida.setFechaFin(AppClock.today().minusDays(2));
        vencida.setActivo(true);
        vencida.setCreatedBy("test");
        vencida.setUpdatedBy("test");
        relacionRepository.saveAndFlush(vencida);

        service.crear(luna.getUuid(), pedido(carlos, TipoRelacionMascota.COPROPIETARIO));

        assertThat(relacionRepository.findById(vencida.getId()).orElseThrow().getActivo()).isFalse();
        assertThat(relacionRepository.findById(vencida.getId()).orElseThrow().getRevokedBy()).contains("vigencia vencida");
        verify(auditLogService).log(eq(clinica.getId()), eq("VENCER_RELACION_MASCOTA"), eq("Mascotas"), contains("Venció el vínculo"));
    }

    @Test
    void elJobCierraLosVinculosVencidosYDejaConstancia() {
        MascotaRelacionRequest request = pedido(carlos, TipoRelacionMascota.COPROPIETARIO);
        MascotaRelacionResponse creada = service.crear(luna.getUuid(), request);
        MascotaPersonaRelacion relacion = relacionRepository.findByUuidAndCompanyId(creada.getUuid(), clinica.getId()).orElseThrow();
        relacion.setFechaFin(AppClock.today().minusDays(1));
        relacionRepository.saveAndFlush(relacion);

        int cerradas = service.cerrarVencidas();

        assertThat(cerradas).isEqualTo(1);
        assertThat(relacionRepository.findById(relacion.getId()).orElseThrow().getActivo()).isFalse();
        verify(auditLogService).log(eq(clinica.getId()), eq("VENCER_RELACION_MASCOTA"), eq("Mascotas"),
                contains("recibir información: sí"));
        assertThat(service.cerrarVencidas()).isZero();
    }

    @Test
    void laAuditoriaDiceQuePermisosYVigenciaTeniaElVinculoAlCrearActualizarYRevocar() {
        MascotaRelacionRequest request = pedido(carlos, TipoRelacionMascota.REPRESENTANTE_AUTORIZADO);
        request.setPuedeRecibirInformacion(true);
        request.setFechaFin(AppClock.today().plusDays(30));
        MascotaRelacionResponse creada = service.crear(luna.getUuid(), request);
        verify(auditLogService).log(eq(clinica.getId()), eq("VINCULAR_PERSONA_MASCOTA"), eq("Mascotas"),
                contains("recibir información: sí, autorizar atención: no, realizar pagos: no; vigente desde "));

        MascotaRelacionRequest cambio = pedido(carlos, TipoRelacionMascota.REPRESENTANTE_AUTORIZADO);
        cambio.setPuedeRecibirInformacion(true);
        cambio.setPuedeRealizarPagos(true);
        cambio.setFechaFin(AppClock.today().plusDays(30));
        service.actualizar(luna.getUuid(), creada.getUuid(), cambio);
        verify(auditLogService).log(eq(clinica.getId()), eq("ACTUALIZAR_RELACION_MASCOTA"), eq("Mascotas"),
                contains("Antes: recibir información: sí, autorizar atención: no, realizar pagos: no"));
        verify(auditLogService).log(eq(clinica.getId()), eq("ACTUALIZAR_RELACION_MASCOTA"), eq("Mascotas"),
                contains("Ahora: recibir información: sí, autorizar atención: no, realizar pagos: sí"));

        service.actualizar(luna.getUuid(), creada.getUuid(), cambio);
        verify(auditLogService, org.mockito.Mockito.times(1)).log(eq(clinica.getId()), eq("ACTUALIZAR_RELACION_MASCOTA"),
                eq("Mascotas"), anyString());

        service.revocar(luna.getUuid(), creada.getUuid());
        verify(auditLogService).log(eq(clinica.getId()), eq("REVOCAR_RELACION_MASCOTA"), eq("Mascotas"),
                contains("Tenía: recibir información: sí, autorizar atención: no, realizar pagos: sí"));
    }

    @Test
    void alPropietarioPrincipalSeLeAvisaDeCadaCambioDeVinculo() {
        MascotaRelacionResponse creada = service.crear(luna.getUuid(), pedido(carlos, TipoRelacionMascota.COPROPIETARIO));
        verify(petLinkNotifier).avisarAlPrincipal(org.mockito.ArgumentMatchers.any(Mascota.class),
                org.mockito.ArgumentMatchers.any(Apoderado.class), eq("vinculó"), anyString());

        service.revocar(luna.getUuid(), creada.getUuid());
        verify(petLinkNotifier).avisarAlPrincipal(org.mockito.ArgumentMatchers.any(Mascota.class),
                org.mockito.ArgumentMatchers.any(Apoderado.class), eq("revocó"), anyString());
    }

    @Test
    void elAccesoAlPortalSoloSeOfreceCuandoSeEligio() {
        service.crear(luna.getUuid(), pedido(carlos, TipoRelacionMascota.COPROPIETARIO));
        verify(apoderadoService, never()).invitarAcceso(org.mockito.ArgumentMatchers.anyLong());

        Apoderado rosa = cliente("rosa");
        MascotaRelacionRequest conAcceso = pedido(rosa, TipoRelacionMascota.COPROPIETARIO);
        conAcceso.setDarAccesoPortal(true);
        service.crear(luna.getUuid(), conAcceso);

        verify(apoderadoService).invitarAcceso(rosa.getId());
    }

    @Test
    void laRespuestaDiceSiLaPersonaYaActivoSuCuenta() {
        Apoderado pendiente = cliente("pendiente");
        pendiente.getUser().setEmailVerified(false);
        usuarioRepository.saveAndFlush(pendiente.getUser());

        MascotaRelacionResponse respuesta = service.crear(luna.getUuid(), pedido(pendiente, TipoRelacionMascota.COPROPIETARIO));

        assertThat(respuesta.getCuentaActivada()).isFalse();
        assertThat(service.crear(luna.getUuid(), pedido(carlos, TipoRelacionMascota.COPROPIETARIO)).getCuentaActivada()).isTrue();
    }
}
