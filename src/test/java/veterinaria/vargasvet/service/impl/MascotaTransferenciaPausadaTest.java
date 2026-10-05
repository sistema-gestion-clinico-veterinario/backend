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
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.MotivoBajaMascota;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.domain.enums.TipoInactividad;
import veterinaria.vargasvet.dto.request.MascotaRequest;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.MascotaPersonaRelacionRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.PetOwnershipService;
import veterinaria.vargasvet.util.BusinessValidator;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Una mascota pausada por el sistema porque su propietario dejó de estar activo no queda varada:
 * se le transfiere la titularidad a un cliente activo y vuelve a operar. Las dadas de baja por el
 * personal (fallecimiento, etc.) siguen sin poder editarse.
 */
@DataJpaTest
class MascotaTransferenciaPausadaTest {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private MascotaPersonaRelacionRepository relacionRepository;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

    private MascotaServiceImpl service;
    private Company clinica;
    private Apoderado anaInactiva;
    private Apoderado carlosActivo;

    @BeforeEach
    void setUp() {
        PetOwnershipService pets = new PetOwnershipService(mascotaRepository, relacionRepository, citaRepository,
                mock(AuditLogService.class));
        ReflectionTestUtils.setField(pets, "entityManager", entityManager);
        service = new MascotaServiceImpl(
                mascotaRepository, apoderadoRepository, citaRepository,
                mock(veterinaria.vargasvet.repository.HistoriaClinicaRepository.class),
                mock(veterinaria.vargasvet.mapper.MascotaMapper.class), mock(BusinessValidator.class),
                mock(AuditLogService.class), mock(veterinaria.vargasvet.repository.RazaRepository.class),
                mock(veterinaria.vargasvet.service.ApoderadoService.class),
                mock(veterinaria.vargasvet.service.MascotaRelacionService.class), pets);

        clinica = new Company();
        clinica.setName("Clínica Prueba");
        clinica.setSlug("clinica-" + UUID.randomUUID());
        clinica.setActivo(true);
        clinica = companyRepository.saveAndFlush(clinica);
        anaInactiva = cliente("ana", false);
        anaInactiva.setTipoInactividad(TipoInactividad.BAJA);
        apoderadoRepository.saveAndFlush(anaInactiva);
        carlosActivo = cliente("carlos", true);

        UsuarioPrincipal principal = new UsuarioPrincipal(99, "admin@clinica.test", "", List.of(), clinica.getId(),
                2, RoleScope.STAFF, RolePurpose.COMPANY_ADMIN, 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private Apoderado cliente(String prefijo, boolean activo) {
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
        apoderado.setEstado(activo);
        return apoderadoRepository.saveAndFlush(apoderado);
    }

    private Mascota mascotaInactiva(MotivoBajaMascota motivo) {
        Mascota mascota = new Mascota();
        mascota.setNombreCompleto("Luna");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(anaInactiva);
        mascota.setUuid(UUID.randomUUID().toString());
        mascota.setActivo(false);
        mascota.setMotivoBaja(motivo);
        return mascotaRepository.saveAndFlush(mascota);
    }

    private MascotaRequest transferirA(Apoderado nuevo) {
        MascotaRequest request = new MascotaRequest();
        request.setApoderadoId(nuevo.getId());
        return request;
    }

    @Test
    void unaMascotaPausadaPorElSistemaSePuedeTransferirYVuelveAOperar() {
        Mascota luna = mascotaInactiva(MotivoBajaMascota.BAJA_DEL_PROPIETARIO);

        service.updateMascota(luna.getId(), transferirA(carlosActivo));

        Mascota despues = mascotaRepository.findById(luna.getId()).orElseThrow();
        assertThat(despues.getApoderado().getId()).isEqualTo(carlosActivo.getId());
        assertThat(despues.getActivo()).isTrue();
        assertThat(despues.getMotivoBaja()).isNull();
    }

    @Test
    void unaMascotaPausadaPorElSistemaNoSeEditaSinTransferirla() {
        Mascota luna = mascotaInactiva(MotivoBajaMascota.SUSPENSION_DEL_PROPIETARIO);
        MascotaRequest sinTransferencia = new MascotaRequest();
        sinTransferencia.setNombreCompleto("Luna Nueva");

        assertThatThrownBy(() -> service.updateMascota(luna.getId(), sinTransferencia))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Reactiva al propietario o transfiere la titularidad");
        assertThat(mascotaRepository.findById(luna.getId()).orElseThrow().getActivo()).isFalse();
    }

    @Test
    void unaMascotaDadaDeBajaPorElPersonalNoSeTransfiere() {
        Mascota luna = mascotaInactiva(MotivoBajaMascota.FALLECIMIENTO);

        assertThatThrownBy(() -> service.updateMascota(luna.getId(), transferirA(carlosActivo)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede editar una mascota inactiva");
        assertThat(mascotaRepository.findById(luna.getId()).orElseThrow().getApoderado().getId())
                .isEqualTo(anaInactiva.getId());
    }
}
