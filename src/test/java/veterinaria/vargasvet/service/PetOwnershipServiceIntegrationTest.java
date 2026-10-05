package veterinaria.vargasvet.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.MascotaPersonaRelacion;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.MotivoBajaMascota;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.domain.enums.TipoInactividad;
import veterinaria.vargasvet.domain.enums.TipoRelacionMascota;
import veterinaria.vargasvet.dto.response.ApoderadoEstadoResponse;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.MascotaPersonaRelacionRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.util.AppClock;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Una mascota está operable mientras alguna persona activa de SU empresa pueda autorizar su atención.
 * Al suspender, dar de baja o reactivar a una persona, solo se tocan las mascotas que quedan sin esa
 * persona (o la recuperan), nunca las que el personal dio de baja por otra causa, y nunca las de otra
 * clínica aunque la persona también sea cliente allí.
 */
@DataJpaTest
class PetOwnershipServiceIntegrationTest {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private MascotaPersonaRelacionRepository relacionRepository;

    private CitaRepository citaRepository;
    private AuditLogService auditLogService;
    private PetOwnershipService service;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

    @BeforeEach
    void setUp() {
        citaRepository = mock(CitaRepository.class);
        auditLogService = mock(AuditLogService.class);
        service = new PetOwnershipService(mascotaRepository, relacionRepository, citaRepository, auditLogService);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "entityManager", entityManager);
    }

    private Company empresa(String nombre) {
        Company company = new Company();
        company.setName(nombre);
        company.setSlug(nombre + "-" + UUID.randomUUID());
        company.setActivo(true);
        return companyRepository.saveAndFlush(company);
    }

    private Usuario persona(String prefijo) {
        Usuario usuario = new Usuario();
        usuario.setEmail(prefijo + "-" + UUID.randomUUID() + "@vargasvet.test");
        usuario.setUsername(prefijo + "-" + UUID.randomUUID());
        usuario.setNombre(prefijo);
        usuario.setApellido("Test");
        usuario.setDni(String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private Apoderado cliente(Usuario usuario, Company company) {
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(usuario);
        apoderado.setCompany(company);
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
        mascota = mascotaRepository.saveAndFlush(mascota);
        relacion(mascota, principal, TipoRelacionMascota.PROPIETARIO_PRINCIPAL, true);
        return mascota;
    }

    private MascotaPersonaRelacion relacion(Mascota mascota, Apoderado persona, TipoRelacionMascota tipo,
                                            boolean puedeAutorizar) {
        MascotaPersonaRelacion relacion = new MascotaPersonaRelacion();
        relacion.setMascota(mascota);
        relacion.setApoderado(persona);
        relacion.setCompany(persona.getCompany());
        relacion.setTipoRelacion(tipo);
        relacion.setPuedeAutorizarAtencion(puedeAutorizar);
        relacion.setPuedeRecibirInformacion(true);
        relacion.setPuedeRealizarPagos(true);
        relacion.setFechaInicio(AppClock.today().minusDays(30));
        relacion.setCreatedBy("test");
        relacion.setUpdatedBy("test");
        return relacionRepository.saveAndFlush(relacion);
    }

    private void cambiarEstado(Apoderado persona, boolean activo, TipoInactividad tipo) {
        persona.setEstado(activo);
        persona.setTipoInactividad(activo ? null : tipo);
        apoderadoRepository.saveAndFlush(persona);
    }

    private Mascota recargar(Mascota mascota) {
        return mascotaRepository.findById(mascota.getId()).orElseThrow();
    }

    @Test
    void alSuspenderALaPropietariaSeLlevanSusMascotasSinOtraPersonaAutorizada_YAlReactivarVuelven() {
        Company clinica = empresa("clinica");
        Apoderado ana = cliente(persona("ana"), clinica);
        Mascota luna = mascota("Luna", ana);

        cambiarEstado(ana, false, TipoInactividad.SUSPENSION);
        ApoderadoEstadoResponse suspension = service.syncPets(ana, TipoInactividad.SUSPENSION);

        assertThat(suspension.getMascotasPausadas()).containsExactly("Luna");
        assertThat(recargar(luna).getActivo()).isFalse();
        assertThat(recargar(luna).getMotivoBaja()).isEqualTo(MotivoBajaMascota.SUSPENSION_DEL_PROPIETARIO);

        cambiarEstado(ana, true, null);
        ApoderadoEstadoResponse reactivacion = service.syncPets(ana, null);

        assertThat(reactivacion.getMascotasRestauradas()).containsExactly("Luna");
        assertThat(recargar(luna).getActivo()).isTrue();
        assertThat(recargar(luna).getMotivoBaja()).isNull();
    }

    @Test
    void unaMascotaFallecidaAntesNuncaSeToca_NiAlPausarNiAlRestaurar() {
        Company clinica = empresa("clinica");
        Apoderado ana = cliente(persona("ana"), clinica);
        Mascota luna = mascota("Luna", ana);
        Mascota rex = mascota("Rex", ana);
        rex.setActivo(false);
        rex.setMotivoBaja(MotivoBajaMascota.FALLECIMIENTO);
        mascotaRepository.saveAndFlush(rex);

        cambiarEstado(ana, false, TipoInactividad.BAJA);
        ApoderadoEstadoResponse baja = service.syncPets(ana, TipoInactividad.BAJA);
        assertThat(baja.getMascotasPausadas()).containsExactly("Luna");
        assertThat(recargar(rex).getMotivoBaja()).isEqualTo(MotivoBajaMascota.FALLECIMIENTO);

        cambiarEstado(ana, true, null);
        ApoderadoEstadoResponse reactivacion = service.syncPets(ana, null);

        assertThat(reactivacion.getMascotasRestauradas()).containsExactly("Luna");
        assertThat(reactivacion.getMascotasQueSiguenInactivas()).containsExactly("Rex (fallecimiento)");
        assertThat(recargar(rex).getActivo()).isFalse();
        assertThat(recargar(rex).getMotivoBaja()).isEqualTo(MotivoBajaMascota.FALLECIMIENTO);
    }

    @Test
    void alReactivarSeInformaPorQueCadaMascotaQueNoVuelveSigueInactiva() {
        Company clinica = empresa("clinica");
        Apoderado ana = cliente(persona("ana"), clinica);
        mascota("Luna", ana);
        Mascota rex = mascota("Rex", ana);
        rex.setActivo(false);
        rex.setMotivoBaja(MotivoBajaMascota.FALLECIMIENTO);
        mascotaRepository.saveAndFlush(rex);
        Mascota toby = mascota("Toby", ana);
        toby.setActivo(false);
        toby.setMotivoBaja(MotivoBajaMascota.DEJA_ASISTIR);
        mascotaRepository.saveAndFlush(toby);
        Mascota pipo = mascota("Pipo", ana);
        pipo.setActivo(false);
        mascotaRepository.saveAndFlush(pipo);

        cambiarEstado(ana, false, TipoInactividad.SUSPENSION);
        ApoderadoEstadoResponse suspension = service.syncPets(ana, TipoInactividad.SUSPENSION);
        assertThat(suspension.getMascotasQueSiguenInactivas()).isEmpty();

        cambiarEstado(ana, true, null);
        ApoderadoEstadoResponse reactivacion = service.syncPets(ana, null);

        assertThat(reactivacion.getMascotasRestauradas()).containsExactly("Luna");
        assertThat(reactivacion.getMascotasQueSiguenInactivas())
                .containsExactlyInAnyOrder("Rex (fallecimiento)", "Toby (dejó de asistir)", "Pipo (sin motivo registrado)");
    }

    @Test
    void unaMascotaDadaDeBajaAManoConElMismoMotivoDeAntesNoSeRestaura() {
        Company clinica = empresa("clinica");
        Apoderado ana = cliente(persona("ana"), clinica);
        Mascota luna = mascota("Luna", ana);
        luna.setActivo(false);
        luna.setMotivoBaja(MotivoBajaMascota.DEJA_ASISTIR);
        mascotaRepository.saveAndFlush(luna);

        cambiarEstado(ana, false, TipoInactividad.BAJA);
        service.syncPets(ana, TipoInactividad.BAJA);
        cambiarEstado(ana, true, null);
        ApoderadoEstadoResponse reactivacion = service.syncPets(ana, null);

        assertThat(reactivacion.getMascotasRestauradas()).isEmpty();
        assertThat(recargar(luna).getActivo()).isFalse();
        assertThat(recargar(luna).getMotivoBaja()).isEqualTo(MotivoBajaMascota.DEJA_ASISTIR);
    }

    @Test
    void siHayUnCopropietarioActivoLaMascotaSigueActivaYSeAvisa() {
        Company clinica = empresa("clinica");
        Apoderado ana = cliente(persona("ana"), clinica);
        Apoderado luis = cliente(persona("luis"), clinica);
        Mascota luna = mascota("Luna", ana);
        relacion(luna, luis, TipoRelacionMascota.COPROPIETARIO, true);

        cambiarEstado(ana, false, TipoInactividad.SUSPENSION);
        ApoderadoEstadoResponse resultado = service.syncPets(ana, TipoInactividad.SUSPENSION);

        assertThat(resultado.getMascotasPausadas()).isEmpty();
        assertThat(resultado.getMascotasQueSiguenActivas()).containsExactly("Luna");
        assertThat(recargar(luna).getActivo()).isTrue();
        assertThat(service.hasActiveAuthorizer(recargar(luna))).isTrue();
    }

    @Test
    void siLaUltimaPersonaAutorizadaSeSuspendeLaMascotaSePausa_YAlVolverElloSeRestaura() {
        Company clinica = empresa("clinica");
        Apoderado ana = cliente(persona("ana"), clinica);
        Apoderado luis = cliente(persona("luis"), clinica);
        Mascota luna = mascota("Luna", ana);
        relacion(luna, luis, TipoRelacionMascota.COPROPIETARIO, true);

        cambiarEstado(ana, false, TipoInactividad.BAJA);
        service.syncPets(ana, TipoInactividad.BAJA);
        cambiarEstado(luis, false, TipoInactividad.SUSPENSION);
        ApoderadoEstadoResponse alSuspenderALuis = service.syncPets(luis, TipoInactividad.SUSPENSION);

        assertThat(alSuspenderALuis.getMascotasPausadas()).containsExactly("Luna");
        assertThat(recargar(luna).getMotivoBaja()).isEqualTo(MotivoBajaMascota.SUSPENSION_DEL_PROPIETARIO);

        cambiarEstado(luis, true, null);
        ApoderadoEstadoResponse alVolverLuis = service.syncPets(luis, null);

        assertThat(alVolverLuis.getMascotasRestauradas()).containsExactly("Luna");
        assertThat(recargar(luna).getActivo()).isTrue();
    }

    @Test
    void suspenderAUnCopropietarioNoToca_laMascotaCuyaPrincipalSigueActiva() {
        Company clinica = empresa("clinica");
        Apoderado ana = cliente(persona("ana"), clinica);
        Apoderado luis = cliente(persona("luis"), clinica);
        Mascota luna = mascota("Luna", ana);
        relacion(luna, luis, TipoRelacionMascota.COPROPIETARIO, true);

        cambiarEstado(luis, false, TipoInactividad.SUSPENSION);
        ApoderadoEstadoResponse resultado = service.syncPets(luis, TipoInactividad.SUSPENSION);

        assertThat(resultado.getMascotasPausadas()).isEmpty();
        assertThat(resultado.getMascotasQueSiguenActivas()).isEmpty();
        assertThat(recargar(luna).getActivo()).isTrue();
    }

    @Test
    void unRepresentanteSinPermisoParaAutorizarNoCuentaComoAutorizador() {
        Company clinica = empresa("clinica");
        Apoderado ana = cliente(persona("ana"), clinica);
        Apoderado pagos = cliente(persona("pagos"), clinica);
        Mascota luna = mascota("Luna", ana);
        relacion(luna, pagos, TipoRelacionMascota.RESPONSABLE_PAGO, false);

        cambiarEstado(ana, false, TipoInactividad.SUSPENSION);
        ApoderadoEstadoResponse resultado = service.syncPets(ana, TipoInactividad.SUSPENSION);

        assertThat(resultado.getMascotasPausadas()).containsExactly("Luna");
    }

    @Test
    void unaRelacionVencidaORevocadaNoCuentaComoAutorizadora() {
        Company clinica = empresa("clinica");
        Apoderado ana = cliente(persona("ana"), clinica);
        Apoderado vencida = cliente(persona("vencida"), clinica);
        Apoderado revocada = cliente(persona("revocada"), clinica);
        Mascota luna = mascota("Luna", ana);
        MascotaPersonaRelacion r1 = relacion(luna, vencida, TipoRelacionMascota.COPROPIETARIO, true);
        r1.setFechaInicio(AppClock.today().minusDays(60));
        r1.setFechaFin(AppClock.today().minusDays(1));
        relacionRepository.saveAndFlush(r1);
        MascotaPersonaRelacion r2 = relacion(luna, revocada, TipoRelacionMascota.COPROPIETARIO, true);
        r2.setActivo(false);
        relacionRepository.saveAndFlush(r2);

        cambiarEstado(ana, false, TipoInactividad.BAJA);

        assertThat(service.hasActiveAuthorizer(recargar(luna))).isFalse();
    }

    @Test
    void siLaMascotaTieneCitasVigentesNoSePausaYSeAvisa() {
        Company clinica = empresa("clinica");
        Apoderado ana = cliente(persona("ana"), clinica);
        Apoderado luis = cliente(persona("luis"), clinica);
        Mascota luna = mascota("Luna", ana);
        relacion(luna, luis, TipoRelacionMascota.COPROPIETARIO, true);
        cambiarEstado(ana, false, TipoInactividad.BAJA);
        service.syncPets(ana, TipoInactividad.BAJA);
        when(citaRepository.existsCitaVigenteByMascotaId(eq(luna.getId()), any())).thenReturn(true);

        cambiarEstado(luis, false, TipoInactividad.SUSPENSION);
        ApoderadoEstadoResponse resultado = service.syncPets(luis, TipoInactividad.SUSPENSION);

        assertThat(resultado.getMascotasConCitasVigentes()).containsExactly("Luna");
        assertThat(resultado.getMascotasPausadas()).isEmpty();
        assertThat(recargar(luna).getActivo()).isTrue();
    }

    @Test
    void pasarDeSuspensionABajaCambiaLaMarcaDeLasMascotasPausadas() {
        Company clinica = empresa("clinica");
        Apoderado ana = cliente(persona("ana"), clinica);
        Mascota luna = mascota("Luna", ana);
        cambiarEstado(ana, false, TipoInactividad.SUSPENSION);
        service.syncPets(ana, TipoInactividad.SUSPENSION);

        cambiarEstado(ana, false, TipoInactividad.BAJA);
        service.syncPets(ana, TipoInactividad.BAJA);

        assertThat(recargar(luna).getMotivoBaja()).isEqualTo(MotivoBajaMascota.BAJA_DEL_PROPIETARIO);
        assertThat(recargar(luna).getActivo()).isFalse();
    }

    @Test
    void laMismaPersonaEnDosClinicasNoSeMezcla_SuspenderEnUnaNoTocaLasMascotasDeLaOtra() {
        Company clinicaA = empresa("clinica-a");
        Company clinicaB = empresa("clinica-b");
        Usuario laMismaPersona = persona("ana");
        Apoderado anaEnA = cliente(laMismaPersona, clinicaA);
        Apoderado anaEnB = cliente(laMismaPersona, clinicaB);
        Mascota lunaA = mascota("Luna", anaEnA);
        Mascota lunaB = mascota("Luna", anaEnB);

        cambiarEstado(anaEnA, false, TipoInactividad.BAJA);
        ApoderadoEstadoResponse resultado = service.syncPets(anaEnA, TipoInactividad.BAJA);

        assertThat(resultado.getMascotasPausadas()).containsExactly("Luna");
        assertThat(recargar(lunaA).getActivo()).isFalse();
        assertThat(recargar(lunaB).getActivo()).isTrue();
        assertThat(recargar(lunaB).getMotivoBaja()).isNull();
        assertThat(apoderadoRepository.findById(anaEnB.getId()).orElseThrow().getEstado()).isTrue();

        cambiarEstado(anaEnA, true, null);
        service.syncPets(anaEnA, null);

        assertThat(recargar(lunaA).getActivo()).isTrue();
        assertThat(recargar(lunaB).getActivo()).isTrue();
    }

    @Test
    void unaRelacionDeOtraEmpresaNuncaCuentaComoAutorizadoraDeLaMascota() {
        Company clinicaA = empresa("clinica-a");
        Company clinicaB = empresa("clinica-b");
        Apoderado anaEnA = cliente(persona("ana"), clinicaA);
        Apoderado extranioEnB = cliente(persona("extranio"), clinicaB);
        Mascota luna = mascota("Luna", anaEnA);
        relacion(luna, extranioEnB, TipoRelacionMascota.COPROPIETARIO, true);

        cambiarEstado(anaEnA, false, TipoInactividad.BAJA);

        assertThat(service.hasActiveAuthorizer(recargar(luna))).isFalse();
        ApoderadoEstadoResponse resultado = service.syncPets(anaEnA, TipoInactividad.BAJA);
        assertThat(resultado.getMascotasPausadas()).containsExactly("Luna");
    }

    @Test
    void noSePuedeReactivarAManoUnaMascotaSinPropietarioActivo_PeroSiConUnCopropietarioActivo() {
        Company clinica = empresa("clinica");
        Apoderado ana = cliente(persona("ana"), clinica);
        Apoderado luis = cliente(persona("luis"), clinica);
        Mascota luna = mascota("Luna", ana);
        cambiarEstado(ana, false, TipoInactividad.BAJA);

        assertThatThrownBy(() -> service.assertCanBeReactivated(recargar(luna)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no tiene un propietario activo");

        relacion(luna, luis, TipoRelacionMascota.COPROPIETARIO, true);

        service.assertCanBeReactivated(recargar(luna));
    }

    @Test
    void lasMascotasPausadasYRestauradasQuedanEnLaAuditoriaDeSuEmpresa() {
        Company clinica = empresa("clinica");
        Apoderado ana = cliente(persona("ana"), clinica);
        mascota("Luna", ana);

        cambiarEstado(ana, false, TipoInactividad.BAJA);
        service.syncPets(ana, TipoInactividad.BAJA);
        cambiarEstado(ana, true, null);
        service.syncPets(ana, null);

        org.mockito.Mockito.verify(auditLogService).log(eq(clinica.getId()), eq("DESACTIVAR_MASCOTA"), eq("Mascotas"),
                org.mockito.ArgumentMatchers.contains("Luna"));
        org.mockito.Mockito.verify(auditLogService).log(eq(clinica.getId()), eq("ACTIVAR_MASCOTA"), eq("Mascotas"),
                org.mockito.ArgumentMatchers.contains("Luna"));
    }

    @Test
    void unaPersonaSinEmpresaNoAjustaNada() {
        Apoderado sinEmpresa = new Apoderado();
        sinEmpresa.setEstado(false);

        ApoderadoEstadoResponse resultado = service.syncPets(sinEmpresa, TipoInactividad.BAJA);

        assertThat(resultado.getMascotasPausadas()).isEmpty();
        assertThat(resultado.getMascotasRestauradas()).isEmpty();
    }

    @Test
    void alRevocarAlUnicoCopropietarioConElDuenoInactivoLaMascotaSePausaYAlVincularOtraSeRestaura() {
        Company clinica = empresa("clinica");
        Apoderado ana = cliente(persona("ana"), clinica);
        Apoderado carlos = cliente(persona("carlos"), clinica);
        Mascota luna = mascota("Luna", ana);
        MascotaPersonaRelacion copropiedad = relacion(luna, carlos, TipoRelacionMascota.COPROPIETARIO, true);
        cambiarEstado(ana, false, TipoInactividad.BAJA);
        assertThat(service.syncPets(ana, TipoInactividad.BAJA).getMascotasQueSiguenActivas()).containsExactly("Luna");

        copropiedad.setActivo(false);
        relacionRepository.saveAndFlush(copropiedad);
        String aviso = service.reevaluate(luna);

        assertThat(aviso).contains("Luna").contains("quedó inactiva");
        assertThat(recargar(luna).getActivo()).isFalse();
        assertThat(recargar(luna).getMotivoBaja()).isEqualTo(MotivoBajaMascota.BAJA_DEL_PROPIETARIO);

        copropiedad.setActivo(true);
        copropiedad.setFechaFin(AppClock.today().plusDays(5));
        relacionRepository.saveAndFlush(copropiedad);
        String restauracion = service.reevaluate(luna);

        assertThat(restauracion).contains("se reactivó");
        assertThat(recargar(luna).getActivo()).isTrue();
        assertThat(recargar(luna).getMotivoBaja()).isNull();
    }

    @Test
    void conCitasVigentesLaMascotaNoSePausaYSeAvisa() {
        Company clinica = empresa("clinica");
        Apoderado ana = cliente(persona("ana"), clinica);
        Mascota luna = mascota("Luna", ana);
        cambiarEstado(ana, false, TipoInactividad.SUSPENSION);
        when(citaRepository.existsCitaVigenteByMascotaId(eq(luna.getId()), any())).thenReturn(true);

        String aviso = service.reevaluate(luna);

        assertThat(aviso).contains("sigue activa porque tiene citas vigentes");
        assertThat(recargar(luna).getActivo()).isTrue();
    }

    @Test
    void unaMascotaDadaDeBajaPorElPersonalNoSeRestauraAlVincularAOtraPersona() {
        Company clinica = empresa("clinica");
        Apoderado ana = cliente(persona("ana"), clinica);
        Apoderado carlos = cliente(persona("carlos"), clinica);
        Mascota rex = mascota("Rex", ana);
        rex.setActivo(false);
        rex.setMotivoBaja(MotivoBajaMascota.FALLECIMIENTO);
        mascotaRepository.saveAndFlush(rex);
        relacion(rex, carlos, TipoRelacionMascota.COPROPIETARIO, true);

        assertThat(service.reevaluate(rex)).isNull();
        assertThat(recargar(rex).getActivo()).isFalse();
    }

    @Test
    void assertOperableRechazaUnaMascotaSinPersonaActivaQueAutorice() {
        Company clinica = empresa("clinica");
        Apoderado ana = cliente(persona("ana"), clinica);
        Mascota luna = mascota("Luna", ana);
        service.assertOperable(luna);

        cambiarEstado(ana, false, TipoInactividad.BAJA);

        assertThatThrownBy(() -> service.assertOperable(luna))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La mascota Luna no tiene un propietario activo que autorice su atención");
        luna.setActivo(false);
        assertThatThrownBy(() -> service.assertOperable(luna))
                .hasMessage("La mascota Luna está inactiva");
    }

    @Test
    void laRevisionPeriodicaPausaLasMascotasActivasDeDuenosInactivosSoloEnSuEmpresa() {
        Company a = empresa("clinicaA");
        Company b = empresa("clinicaB");
        Usuario identidad = persona("ana");
        Apoderado enA = cliente(identidad, a);
        Apoderado enB = cliente(persona("ana-b"), b);
        Mascota deA = mascota("DeA", enA);
        Mascota deB = mascota("DeB", enB);
        enA.setEstado(false);
        enA.setTipoInactividad(TipoInactividad.BAJA);
        apoderadoRepository.saveAndFlush(enA);

        long siguiente = service.reviewInactiveOwners(0, 50);

        assertThat(siguiente).isEqualTo(-1);
        assertThat(recargar(deA).getActivo()).isFalse();
        assertThat(recargar(deA).getMotivoBaja()).isEqualTo(MotivoBajaMascota.BAJA_DEL_PROPIETARIO);
        assertThat(recargar(deB).getActivo()).isTrue();
    }

    private MascotaPersonaRelacion vinculo(Mascota mascota, Apoderado persona, TipoRelacionMascota tipo,
                                           boolean informacion, boolean autorizar, boolean pagos,
                                           java.time.LocalDate inicio, java.time.LocalDate fin, boolean activo) {
        MascotaPersonaRelacion relacion = new MascotaPersonaRelacion();
        relacion.setMascota(mascota);
        relacion.setApoderado(persona);
        relacion.setCompany(persona.getCompany());
        relacion.setTipoRelacion(tipo);
        relacion.setPuedeRecibirInformacion(informacion);
        relacion.setPuedeAutorizarAtencion(autorizar);
        relacion.setPuedeRealizarPagos(pagos);
        relacion.setFechaInicio(inicio);
        relacion.setFechaFin(fin);
        relacion.setActivo(activo);
        relacion.setCreatedBy("test");
        relacion.setUpdatedBy("test");
        return relacionRepository.saveAndFlush(relacion);
    }

    private static final PetOwnershipService.Permiso INFO = PetOwnershipService.Permiso.INFORMACION;
    private static final PetOwnershipService.Permiso AUTORIZAR = PetOwnershipService.Permiso.AUTORIZAR;
    private static final PetOwnershipService.Permiso PAGOS = PetOwnershipService.Permiso.PAGOS;

    @Test
    void cadaVinculoDaSoloLosPermisosQueTiene() {
        Company clinica = empresa("clinica");
        Apoderado maria = cliente(persona("maria"), clinica);
        Apoderado carlos = cliente(persona("carlos"), clinica);
        Apoderado rosa = cliente(persona("rosa"), clinica);
        Apoderado luis = cliente(persona("luis"), clinica);
        Apoderado extrana = cliente(persona("extrana"), clinica);
        Mascota luna = mascota("Luna", maria);
        java.time.LocalDate hoy = AppClock.today();
        vinculo(luna, carlos, TipoRelacionMascota.COPROPIETARIO, true, true, true, hoy.minusDays(10), null, true);
        vinculo(luna, rosa, TipoRelacionMascota.REPRESENTANTE_AUTORIZADO, true, false, false, hoy.minusDays(10), null, true);
        vinculo(luna, luis, TipoRelacionMascota.RESPONSABLE_PAGO, false, false, true, hoy.minusDays(10), null, true);

        assertThat(service.puede(maria, luna, INFO, AUTORIZAR, PAGOS)).isTrue();
        assertThat(service.puede(carlos, luna, INFO)).isTrue();
        assertThat(service.puede(carlos, luna, AUTORIZAR)).isTrue();
        assertThat(service.puede(carlos, luna, PAGOS)).isTrue();
        assertThat(service.puede(rosa, luna, INFO)).isTrue();
        assertThat(service.puede(rosa, luna, AUTORIZAR)).isFalse();
        assertThat(service.puede(rosa, luna, PAGOS)).isFalse();
        assertThat(service.puede(luis, luna, INFO)).isFalse();
        assertThat(service.puede(luis, luna, AUTORIZAR)).isFalse();
        assertThat(service.puede(luis, luna, PAGOS)).isTrue();
        assertThat(service.puede(extrana, luna, INFO, AUTORIZAR, PAGOS)).isFalse();
        assertThatThrownBy(() -> service.exigir(extrana, luna, "sin acceso", INFO))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class).hasMessage("sin acceso");
    }

    @Test
    void unVinculoRevocadoVencidoPorEmpezarOSinPersonaActivaNoDaPermisos() {
        Company clinica = empresa("clinica");
        Apoderado maria = cliente(persona("maria"), clinica);
        Apoderado revocada = cliente(persona("revocada"), clinica);
        Apoderado vencida = cliente(persona("vencida"), clinica);
        Apoderado futura = cliente(persona("futura"), clinica);
        Apoderado inactiva = cliente(persona("inactiva"), clinica);
        Mascota luna = mascota("Luna", maria);
        java.time.LocalDate hoy = AppClock.today();
        vinculo(luna, revocada, TipoRelacionMascota.COPROPIETARIO, true, true, true, hoy.minusDays(10), hoy.minusDays(1), false);
        vinculo(luna, vencida, TipoRelacionMascota.COPROPIETARIO, true, true, true, hoy.minusDays(10), hoy.minusDays(1), true);
        vinculo(luna, futura, TipoRelacionMascota.COPROPIETARIO, true, true, true, hoy.plusDays(2), null, true);
        vinculo(luna, inactiva, TipoRelacionMascota.COPROPIETARIO, true, true, true, hoy.minusDays(10), null, true);
        cambiarEstado(inactiva, false, TipoInactividad.BAJA);

        for (Apoderado persona : java.util.List.of(revocada, vencida, futura, inactiva)) {
            assertThat(service.puede(persona, luna, INFO, AUTORIZAR, PAGOS)).as(persona.getUser().getNombre()).isFalse();
            assertThat(service.mascotasConAlgunPermiso(persona, INFO, AUTORIZAR, PAGOS)).isEmpty();
        }
        assertThat(service.tieneVinculoVigente(revocada)).isFalse();
        assertThat(service.tieneVinculoVigente(futura)).isFalse();
    }

    @Test
    void elVinculoVigenteDeUnaPersonaDeOtraClinicaNoDaPermisos() {
        Company a = empresa("clinicaA");
        Company b = empresa("clinicaB");
        Apoderado maria = cliente(persona("maria"), a);
        Mascota luna = mascota("Luna", maria);
        Apoderado carlosEnB = cliente(persona("carlos"), b);
        vinculo(luna, carlosEnB, TipoRelacionMascota.COPROPIETARIO, true, true, true, AppClock.today().minusDays(1), null, true);

        assertThat(service.puede(carlosEnB, luna, INFO, AUTORIZAR, PAGOS)).isFalse();
    }

    @Test
    void lasMascotasDeLaPersonaSonLasPropiasMasLasDeSusVinculosConElPermisoPedido() {
        Company clinica = empresa("clinica");
        Apoderado maria = cliente(persona("maria"), clinica);
        Apoderado carlos = cliente(persona("carlos"), clinica);
        Mascota luna = mascota("Luna", maria);
        Mascota rex = mascota("Rex", carlos);
        Mascota toby = mascota("Toby", maria);
        java.time.LocalDate hoy = AppClock.today();
        vinculo(luna, carlos, TipoRelacionMascota.RESPONSABLE_PAGO, false, false, true, hoy.minusDays(1), null, true);
        vinculo(toby, carlos, TipoRelacionMascota.REPRESENTANTE_AUTORIZADO, true, false, false, hoy.minusDays(1), null, true);

        assertThat(service.mascotasConAlgunPermiso(carlos, INFO, AUTORIZAR)).extracting(Mascota::getNombreCompleto)
                .containsExactly("Rex", "Toby");
        assertThat(service.mascotasConAlgunPermiso(carlos, INFO, PAGOS)).extracting(Mascota::getNombreCompleto)
                .containsExactly("Luna", "Rex", "Toby");
        assertThat(service.mascotasConAlgunPermiso(maria, INFO)).extracting(Mascota::getNombreCompleto)
                .containsExactly("Luna", "Toby");
    }

    @Test
    void losAvisosVanAlPrincipalYAQuienesPuedenRecibirInformacionSiguiendoActivos() {
        Company clinica = empresa("clinica");
        Apoderado maria = cliente(persona("maria"), clinica);
        Apoderado carlos = cliente(persona("carlos"), clinica);
        Apoderado rosa = cliente(persona("rosa"), clinica);
        Apoderado luis = cliente(persona("luis"), clinica);
        Apoderado vieja = cliente(persona("vieja"), clinica);
        Mascota luna = mascota("Luna", maria);
        java.time.LocalDate hoy = AppClock.today();
        vinculo(luna, carlos, TipoRelacionMascota.COPROPIETARIO, true, true, true, hoy.minusDays(10), null, true);
        vinculo(luna, rosa, TipoRelacionMascota.REPRESENTANTE_AUTORIZADO, true, false, false, hoy.minusDays(10), null, true);
        vinculo(luna, luis, TipoRelacionMascota.RESPONSABLE_PAGO, false, false, true, hoy.minusDays(10), null, true);
        vinculo(luna, vieja, TipoRelacionMascota.COPROPIETARIO, true, true, true, hoy.minusDays(10), hoy.minusDays(1), false);

        assertThat(service.destinatariosDeAvisos(luna)).extracting(a -> a.getUser().getNombre())
                .containsExactlyInAnyOrder("maria", "carlos", "rosa");
        assertThat(service.destinatariosDeAvisos(luna).get(0).getId()).isEqualTo(maria.getId());

        cambiarEstado(maria, false, TipoInactividad.BAJA);
        assertThat(service.destinatariosDeAvisos(luna)).extracting(a -> a.getUser().getNombre())
                .containsExactlyInAnyOrder("carlos", "rosa");
    }
}
