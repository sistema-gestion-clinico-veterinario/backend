package veterinaria.vargasvet.service.recordatorios;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.ControlPreventivo;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.RecordatorioPreventivo;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoControlPreventivo;
import veterinaria.vargasvet.domain.enums.EstadoRecordatorio;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoAvisoRecordatorio;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.ControlPreventivoRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.RecordatorioPreventivoRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CompanyComplianceService;
import veterinaria.vargasvet.service.ConsentimientoDatosService;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.OwnerContactPolicy;
import veterinaria.vargasvet.service.PetOwnershipService;
import veterinaria.vargasvet.service.impl.RecordatorioPreventivoServiceImpl;
import veterinaria.vargasvet.service.impl.UsuarioContactoService;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({RecordatorioPreventivoServiceImpl.class, PlanificadorDeRecordatoriosPreventivos.class,
        ComponedorDelCorreoPreventivo.class, RegistroDeRecordatoriosEnviados.class, OwnerContactPolicy.class,
        PetOwnershipService.class})
class RecordatorioPreventivoTransaccionesIntegrationTest {

    private static final String PLANTILLA = "email/recordatorio-preventivo-template";

    @Autowired private RecordatorioPreventivoServiceImpl servicio;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private ControlPreventivoRepository controlRepository;
    @Autowired private RecordatorioPreventivoRepository recordatorioRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transacciones;
    @MockitoBean private EmailService emailService;
    @MockitoBean private ConsentimientoDatosService consentimientoDatosService;
    @MockitoBean private CompanyComplianceService complianceService;
    @MockitoBean private UsuarioContactoService usuarioContactoService;
    @MockitoBean private AuditLogService auditLogService;

    private Company clinica;
    private final List<String> destinatarios = Collections.synchronizedList(new ArrayList<>());
    private final List<Boolean> transaccionAbiertaAlEnviar = Collections.synchronizedList(new ArrayList<>());
    private Consumer<String> alEnviarA = correo -> { };
    private boolean elEnvioTiene = true;

    @BeforeEach
    void preparar() {
        limpiar();
        when(complianceService.gestionadoPorSoftvet(anyInt(), any())).thenReturn(true);
        when(consentimientoDatosService.usuariosQueAutorizaron(anyCollection(), anyInt(), any()))
                .thenAnswer(invocation -> Set.copyOf(invocation.getArgument(0)));
        when(emailService.createMail(anyString(), anyString(), anyMap()))
                .thenAnswer(invocation -> new Mail(null, invocation.getArgument(0), invocation.getArgument(1),
                        invocation.getArgument(2)));
        when(emailService.sendEmail(any(Mail.class), eq(PLANTILLA))).thenAnswer(invocation -> {
            Mail correo = invocation.getArgument(0);
            destinatarios.add(correo.getTo());
            transaccionAbiertaAlEnviar.add(TransactionSynchronizationManager.isActualTransactionActive());
            alEnviarA.accept(correo.getTo());
            return CompletableFuture.completedFuture(elEnvioTiene);
        });
        clinica = new Company();
        clinica.setName("Clinica Patitas");
        clinica.setSlug("patitas-" + UUID.randomUUID());
        clinica.setRuc("20" + String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 9));
        clinica.setActivo(true);
        clinica = companyRepository.save(clinica);
    }

    @AfterEach
    void limpiar() {
        jdbcTemplate.update("DELETE FROM recordatorio_preventivo");
        jdbcTemplate.update("DELETE FROM control_preventivo");
    }

    @Test
    void elCorreoSeEnviaSinNingunaTransaccionAbierta() {
        propietario("ana", 2);
        propietario("beto", 2);

        servicio.procesarRecordatorios();

        assertThat(destinatarios).hasSize(2);
        assertThat(transaccionAbiertaAlEnviar).containsOnly(false);
    }

    @Test
    void guardaLosAvisosDeCadaPropietarioDespuesDeEnviarle() {
        propietario("ana", 2);
        propietario("beto", 3);

        servicio.procesarRecordatorios();

        assertThat(registrosDe("ana")).isEqualTo(2);
        assertThat(registrosDe("beto")).isEqualTo(3);
        assertThat(jdbcTemplate.queryForList("SELECT DISTINCT estado FROM recordatorio_preventivo", String.class))
                .containsExactly("ENVIADO");
    }

    @Test
    void siFallaElGuardadoDeUnPropietarioLosDemasQuedanRegistradosYElTrabajoTermina() {
        propietario("ana", 2);
        Apoderado beto = propietario("beto", 2);
        propietario("carla", 2);
        alEnviarA = correo -> {
            if (correo.startsWith("beto")) {
                new TransactionTemplate(transacciones).executeWithoutResult(
                        status -> recordatorioRepository.save(registroDelPrimerControlDe(beto)));
            }
        };

        servicio.procesarRecordatorios();

        assertThat(destinatarios).hasSize(3);
        assertThat(registrosDe("ana")).isEqualTo(2);
        assertThat(registrosDe("carla")).isEqualTo(2);
        assertThat(registrosDe("beto")).isEqualTo(1);
    }

    @Test
    void alDiaSiguienteSoloSeReenviaAlPropietarioCuyoGuardadoFallo() {
        propietario("ana", 2);
        Apoderado beto = propietario("beto", 2);
        propietario("carla", 2);
        alEnviarA = correo -> {
            if (correo.startsWith("beto")) {
                new TransactionTemplate(transacciones).executeWithoutResult(
                        status -> recordatorioRepository.save(registroDelPrimerControlDe(beto)));
            }
        };
        servicio.procesarRecordatorios();
        destinatarios.clear();
        alEnviarA = correo -> { };
        jdbcTemplate.update("UPDATE recordatorio_preventivo SET fecha_envio = ?", LocalDateTime.now().minusDays(8));

        servicio.procesarRecordatorios();

        assertThat(destinatarios).hasSize(1);
        assertThat(destinatarios.getFirst()).startsWith("beto");
    }

    @Test
    void siElCorreoFallaNoGuardaNadaPeroElEstadoDeLosControlesSiSeActualiza() {
        propietario("ana", 2);
        elEnvioTiene = false;

        servicio.procesarRecordatorios();

        assertThat(destinatarios).hasSize(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM recordatorio_preventivo", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForList("SELECT DISTINCT estado FROM control_preventivo", String.class))
                .containsExactly("PROXIMO");
    }

    @Test
    void siSeEjecutaDosVecesNoSeVuelveAEnviarNiDuplicaRegistros() {
        propietario("ana", 2);

        servicio.procesarRecordatorios();
        servicio.procesarRecordatorios();

        assertThat(destinatarios).hasSize(1);
        assertThat(registrosDe("ana")).isEqualTo(2);
    }

    private Apoderado propietario(String prefijo, int controles) {
        return new TransactionTemplate(transacciones).execute(status -> {
            Usuario usuario = new Usuario();
            usuario.setEmail(prefijo + "-" + UUID.randomUUID() + "@example.test");
            usuario.setUsername(prefijo + "-" + UUID.randomUUID());
            usuario.setNombre(prefijo);
            usuario.setApellido("Test");
            usuario.setDni(String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 8));
            usuario.setActivo(true);
            usuario.setEmailVerified(true);
            usuario.setCompany(clinica);
            usuario = usuarioRepository.save(usuario);
            Apoderado apoderado = new Apoderado();
            apoderado.setUser(usuario);
            apoderado.setCompany(clinica);
            apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
            apoderado.setNumeroDocumento(usuario.getDni());
            apoderado.setGenero(Genero.FEMENINO);
            apoderado = apoderadoRepository.save(apoderado);
            Mascota mascota = new Mascota();
            mascota.setNombreCompleto("Mascota de " + prefijo);
            mascota.setEspecie(EspecieMascota.PERRO);
            mascota.setApoderado(apoderado);
            mascota.setUuid(UUID.randomUUID().toString());
            mascota = mascotaRepository.save(mascota);
            for (int i = 0; i < controles; i++) {
                ControlPreventivo control = new ControlPreventivo();
                control.setMascota(mascota);
                control.setTipo(TipoControlPreventivo.VACUNACION);
                control.setNombreControl("Control " + i);
                control.setFechaRecomendada(AppClock.today().plusDays(3));
                control.setEstado(EstadoControlPreventivo.PROGRAMADO);
                control.setCreatedBy("qa");
                control.setUpdatedBy("qa");
                controlRepository.save(control);
            }
            return apoderado;
        });
    }

    private RecordatorioPreventivo registroDelPrimerControlDe(Apoderado apoderado) {
        Long controlId = jdbcTemplate.queryForObject("SELECT MIN(c.id) FROM control_preventivo c JOIN mascota m ON m.id = c.mascota_id "
                + "WHERE m.apoderado_id = ?", Long.class, apoderado.getId());
        RecordatorioPreventivo registro = new RecordatorioPreventivo();
        registro.setApoderado(apoderadoRepository.getReferenceById(apoderado.getId()));
        registro.setControlPreventivo(controlRepository.getReferenceById(controlId));
        registro.setTipoAviso(TipoAvisoRecordatorio.PROXIMO);
        registro.setFechaProgramada(AppClock.today().plusDays(3));
        registro.setFechaEnvio(LocalDateTime.now());
        registro.setEstado(EstadoRecordatorio.ENVIADO);
        registro.setCreatedBy("QA");
        registro.setUpdatedBy("QA");
        return registro;
    }

    private int registrosDe(String prefijo) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM recordatorio_preventivo r JOIN apoderado a ON a.id = r.apoderado_id "
                + "JOIN usuario u ON u.id = a.user_id WHERE u.username LIKE ?", Integer.class, prefijo + "-%");
    }
}
