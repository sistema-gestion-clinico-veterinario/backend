package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import veterinaria.vargasvet.domain.entity.*;
import veterinaria.vargasvet.domain.enums.*;
import veterinaria.vargasvet.dto.request.CartillaAplicacionRequest;
import veterinaria.vargasvet.mapper.MascotaCartillaMapper;
import veterinaria.vargasvet.repository.*;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.util.AppClock;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest
class CartillaServiceConcurrencyIntegrationTest {

    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private HistoriaClinicaRepository historiaClinicaRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private ServiciosVeterinariosRepository serviciosRepository;
    @Autowired private TipoVacunaRepository tipoVacunaRepository;
    @Autowired private TipoDesparasitanteRepository tipoDesparasitanteRepository;
    @Autowired private RegistroVacunaRepository vacunaRepository;
    @Autowired private RegistroDesparasitacionRepository desparasitacionRepository;
    @Autowired private ControlPreventivoRepository controlRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("[BUG] Dos registros concurrentes de la misma vacuna no duplican el proximo control")
    void registrosConcurrentesParaLaMismaVacunaConservanUnSoloProximoControl() throws Exception {
        CartillaServiceImpl cartillaService = new CartillaServiceImpl(
                mascotaRepository, historiaClinicaRepository, empleadoRepository, serviciosRepository,
                tipoVacunaRepository, tipoDesparasitanteRepository, vacunaRepository, desparasitacionRepository,
                controlRepository, citaRepository, mock(UsuarioContactoService.class), mock(AuditLogService.class),
                mock(SimpMessagingTemplate.class), mock(MascotaCartillaMapper.class));

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Setup setup = transaction.execute(status -> crearEscenario());

        CartillaAplicacionRequest request = new CartillaAplicacionRequest();
        request.setMascotaId(setup.mascotaId());
        request.setServicioId(setup.servicioId());
        request.setTipoVacunaId(setup.vacunaId());
        request.setFechaAplicacion(AppClock.today());
        request.setIntervaloCantidad(21);
        request.setIntervaloUnidad(IntervaloUnidad.DIAS);

        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Throwable> primera = executor.submit(
                    () -> ejecutarRegistroConcurrente(cartillaService, transaction, request, setup.vetUserId(), inicio));
            Future<Throwable> segunda = executor.submit(
                    () -> ejecutarRegistroConcurrente(cartillaService, transaction, request, setup.vetUserId(), inicio));
            inicio.countDown();

            assertThat(primera.get()).isNull();
            assertThat(segunda.get()).isNull();

            List<ControlPreventivo> proximosControles = transaction.execute(status ->
                    controlRepository.findByMascotaIdOrderByFechaRecomendadaDesc(setup.mascotaId()).stream()
                            .filter(c -> c.getTipo() == TipoControlPreventivo.VACUNACION)
                            .filter(c -> c.getFechaRecomendada().equals(AppClock.today().plusDays(21)))
                            .toList());
            assertThat(proximosControles).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("[BUG] Un registro real (sin mocks de repositorio) persiste la cita de cobro y el registro con id")
    void unSoloRegistroPersisteCitaYRegistroConIdReal() {
        CartillaServiceImpl cartillaService = new CartillaServiceImpl(
                mascotaRepository, historiaClinicaRepository, empleadoRepository, serviciosRepository,
                tipoVacunaRepository, tipoDesparasitanteRepository, vacunaRepository, desparasitacionRepository,
                controlRepository, citaRepository, mock(UsuarioContactoService.class), mock(AuditLogService.class),
                mock(SimpMessagingTemplate.class), mock(MascotaCartillaMapper.class));

        Setup setup = crearEscenario();

        CartillaAplicacionRequest request = new CartillaAplicacionRequest();
        request.setMascotaId(setup.mascotaId());
        request.setServicioId(setup.servicioId());
        request.setTipoVacunaId(setup.vacunaId());
        request.setFechaAplicacion(AppClock.today());
        request.setIntervaloCantidad(21);
        request.setIntervaloUnidad(IntervaloUnidad.DIAS);

        autenticarVeterinario(setup.vetUserId());
        try {
            var response = cartillaService.registrarVacunacion(request);
            assertThat(response.getRegistroId()).isNotNull();
            assertThat(response.getCitaId()).isNotNull();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Throwable ejecutarRegistroConcurrente(CartillaServiceImpl cartillaService,
                                                   TransactionTemplate transaction,
                                                   CartillaAplicacionRequest request,
                                                   Integer vetUserId,
                                                   CountDownLatch inicio) {
        try {
            autenticarVeterinario(vetUserId);
            inicio.await();
            transaction.executeWithoutResult(status -> cartillaService.registrarVacunacion(request));
            return null;
        } catch (Throwable error) {
            return error;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Setup crearEscenario() {
        Company company = new Company();
        company.setName("VargasVet Concurrencia");
        company.setSlug("vargasvet-concurrencia-" + UUID.randomUUID());
        company.setRuc(uniqueDigits(11));
        company.setActivo(true);
        company = companyRepository.save(company);

        Usuario apoderadoUser = usuario("cliente", company);
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(apoderadoUser);
        apoderado.setCompany(company);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setNumeroDocumento(uniqueDigits(8));
        apoderado.setGenero(Genero.FEMENINO);
        apoderado = apoderadoRepository.save(apoderado);

        Mascota mascota = new Mascota();
        mascota.setNombreCompleto("Firulais");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(apoderado);
        mascota.setUuid(UUID.randomUUID().toString());
        mascota = mascotaRepository.save(mascota);

        Usuario vetUser = usuario("vet", company);
        Empleado empleado = new Empleado();
        empleado.setUser(vetUser);
        empleado.setCompany(company);
        empleado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        empleado.setNumeroDocumentoIdentidad(uniqueDigits(8));
        empleado.setGenero(Genero.MASCULINO);
        empleado.setEstado(true);
        empleado = empleadoRepository.save(empleado);

        ServiciosVeterinarios servicio = new ServiciosVeterinarios();
        servicio.setCompany(company);
        servicio.setNombre("Vacunacion");
        servicio.setDescripcion("Aplicacion de vacunas");
        servicio.setPrecio(new BigDecimal("90.00"));
        servicio.setDisponible(true);
        servicio.setActivo(true);
        servicio.setDuracionEstimada(15);
        servicio.setPermiteEmergencia(false);
        servicio.setTipoControlPreventivo(TipoControlServicio.VACUNACION);
        servicio = serviciosRepository.save(servicio);

        TipoVacuna vacuna = new TipoVacuna();
        vacuna.setCompany(company);
        vacuna.setNombre("Nobivac Parvo-C");
        vacuna.setEspecie(EspecieMascota.PERRO);
        vacuna.setPrecio(new BigDecimal("90.00"));
        vacuna.setActivo(true);
        vacuna.setCreatedBy("SYSTEM");
        vacuna.setUpdatedBy("SYSTEM");
        vacuna = tipoVacunaRepository.save(vacuna);

        return new Setup(mascota.getId(), servicio.getId(), vacuna.getId(), vetUser.getId());
    }

    private void autenticarVeterinario(Integer userId) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        var principal = new UsuarioPrincipal(
                userId, "vet-" + userId + "@vargasvet.test", "", authorities, null, 1,
                RoleScope.PLATFORM, RolePurpose.PLATFORM_ADMIN, 0L);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal, null, authorities)
        );
    }

    private Usuario usuario(String prefix, Company company) {
        Usuario usuario = new Usuario();
        usuario.setEmail(prefix + "-" + UUID.randomUUID() + "@vargasvet.test");
        usuario.setUsername(prefix + "-" + UUID.randomUUID());
        usuario.setNombre(prefix);
        usuario.setApellido("Test");
        usuario.setDni(uniqueDigits(8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(company);
        return usuarioRepository.save(usuario);
    }

    private String uniqueDigits(int length) {
        String digits = String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        while (digits.length() < length) {
            digits += "0";
        }
        return digits.substring(0, length);
    }

    private record Setup(Long mascotaId, Long servicioId, Long vacunaId, Integer vetUserId) {}
}
