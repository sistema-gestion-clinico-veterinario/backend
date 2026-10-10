package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.ServiciosVeterinarios;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.SexoMascota;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.domain.enums.TipoInactividad;
import veterinaria.vargasvet.mapper.UserMapper;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyExceptionRepository;
import veterinaria.vargasvet.repository.CompanyOperatingHourRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.EspecialidadRepository;
import veterinaria.vargasvet.repository.HorarioEmpleadoRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.ServiciosVeterinariosRepository;
import veterinaria.vargasvet.repository.TipoEmpleadoRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioMembresiaRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AccountClosureGuard;
import veterinaria.vargasvet.service.AdministratorProtection;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CajasAbiertasDelPersonal;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.SessionSecurityService;
import veterinaria.vargasvet.util.AppClock;
import veterinaria.vargasvet.util.BusinessValidator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * La baja frente a las operaciones simultáneas: dos pedidos iguales dejan un solo efecto, y una cita que se
 * agenda justo cuando se da de baja nunca deja a un veterinario inactivo con una cita vigente.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BajaConcurrenciaTest {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private EspecialidadRepository especialidadRepository;
    @Autowired private TipoEmpleadoRepository tipoEmpleadoRepository;
    @Autowired private HorarioEmpleadoRepository horarioEmpleadoRepository;
    @Autowired private CompanyOperatingHourRepository companyOperatingHourRepository;
    @Autowired private CompanyExceptionRepository companyExceptionRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private ServiciosVeterinariosRepository serviciosVeterinariosRepository;
    @Autowired private UsuarioPorRolRepository usuarioPorRolRepository;
    @Autowired private UsuarioEmpresaCredencialRepository credencialRepository;
    @Autowired private UsuarioMembresiaRepository usuarioMembresiaRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

    private Company company;
    private Empleado vet;
    private Apoderado dueno;
    private ServiciosVeterinarios servicio;

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
        citaRepository.deleteAll();
        mascotaRepository.deleteAll();
        serviciosVeterinariosRepository.deleteAll();
        apoderadoRepository.deleteAll();
        empleadoRepository.deleteAll();
        usuarioRepository.deleteAll();
        companyRepository.deleteAll();
    }

    private Usuario persona(String correo, String username, String dni) {
        Usuario usuario = new Usuario();
        usuario.setEmail(correo);
        usuario.setUsername(username);
        usuario.setNombre(username);
        usuario.setApellido("Prueba");
        usuario.setDni(dni);
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(company);
        return usuarioRepository.save(usuario);
    }

    private void fixtures() {
        company = new Company();
        company.setName("Clínica Prueba");
        company.setSlug("clinica-prueba");
        company.setActivo(true);
        company = companyRepository.save(company);

        Usuario vetUsuario = persona("vet@clinica.test", "vet", "11111111");
        vet = new Empleado();
        vet.setUser(vetUsuario);
        vet.setCompany(company);
        vet.setEstado(true);
        vet.setGenero(Genero.FEMENINO);
        vet.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        vet.setNumeroDocumentoIdentidad("11111111");
        vet.setFechaIngreso(AppClock.today());
        vet = empleadoRepository.save(vet);

        Usuario duenoUsuario = persona("dueno@clinica.test", "dueno", "22222222");
        dueno = new Apoderado();
        dueno.setUser(duenoUsuario);
        dueno.setCompany(company);
        dueno.setEstado(true);
        dueno.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        dueno.setNumeroDocumento("22222222");
        dueno.setGenero(Genero.MASCULINO);
        dueno = apoderadoRepository.save(dueno);

        servicio = new ServiciosVeterinarios();
        servicio.setCompany(company);
        servicio.setNombre("Consulta");
        servicio.setDescripcion("Consulta");
        servicio.setPrecio(new BigDecimal("100.00"));
        servicio.setDuracionEstimada(30);
        servicio.setDisponible(true);
        servicio.setActivo(true);
        servicio = serviciosVeterinariosRepository.save(servicio);
    }

    private EmpleadoServiceImpl servicio(SessionSecurityService sessions) {
        CompanyMembershipServiceImpl membership = new CompanyMembershipServiceImpl(
                empleadoRepository, apoderadoRepository, usuarioMembresiaRepository, usuarioRepository, companyRepository);
        EmpleadoServiceImpl service = new EmpleadoServiceImpl(
                usuarioRepository, empleadoRepository, especialidadRepository, tipoEmpleadoRepository,
                companyRepository, horarioEmpleadoRepository, companyOperatingHourRepository, companyExceptionRepository,
                citaRepository, mock(PasswordEncoder.class), new UserMapper(new ModelMapper()), mock(EmailService.class),
                mock(BusinessValidator.class), mock(AuditLogService.class), mock(veterinaria.vargasvet.service.RoleAssignmentService.class), sessions, membership,
                credencialRepository, mock(UsuarioContactoService.class),
                new AdministratorProtection(usuarioPorRolRepository, membership, companyRepository),
                mock(AccountClosureGuard.class), mock(CajasAbiertasDelPersonal.class),
                mock(veterinaria.vargasvet.service.AccessRestoredNotifier.class),
                org.mockito.Mockito.mock(veterinaria.vargasvet.service.ConsentimientoDatosService.class));
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        return service;
    }

    private SessionSecurityService sesionesLentas() {
        SessionSecurityService sessions = mock(SessionSecurityService.class);
        doAnswer(invocation -> {
            Thread.sleep(400);
            return null;
        }).when(sessions).invalidateSessionsForCompany(any(), any());
        return sessions;
    }

    private Callable<Boolean> comoAdministrador(Runnable accion, CountDownLatch salida, long esperaMs,
                                                AtomicReference<Throwable> inesperado) {
        return () -> {
            UsuarioPrincipal principal = new UsuarioPrincipal(99, "admin@clinica.test", "", List.of(), company.getId(),
                    2, RoleScope.STAFF, RolePurpose.COMPANY_ADMIN, 1L);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
            salida.await();
            Thread.sleep(esperaMs);
            try {
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> accion.run());
                return true;
            } catch (IllegalArgumentException | IllegalStateException e) {
                return false;
            } catch (Exception e) {
                inesperado.set(e);
                return false;
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
    }

    @Test
    void dosBajasIgualesALaVezDejanUnSoloEfecto() throws Exception {
        fixtures();
        SessionSecurityService sessions = sesionesLentas();
        EmpleadoServiceImpl service = servicio(sessions);
        Long vetId = vet.getId();
        CountDownLatch salida = new CountDownLatch(1);
        AtomicReference<Throwable> inesperado = new AtomicReference<>();
        Runnable baja = () -> service.cambiarEstado(vetId, false, TipoInactividad.BAJA, "Prueba");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<Boolean> primera = pool.submit(comoAdministrador(baja, salida, 0, inesperado));
        Future<Boolean> segunda = pool.submit(comoAdministrador(baja, salida, 100, inesperado));
        salida.countDown();
        boolean primeraOk = primera.get();
        boolean segundaOk = segunda.get();
        pool.shutdownNow();

        assertThat(inesperado.get()).isNull();
        assertThat(primeraOk && segundaOk).isTrue();
        assertThat(empleadoRepository.findById(vetId).orElseThrow().getEstado()).isFalse();
        verify(sessions, times(1)).invalidateSessionsForCompany(any(), any());
    }

    @Test
    void siSeAgendaUnaCitaMientrasSeDaDeBajaPrimeroGanaElQueLlegaPrimero() throws Exception {
        for (boolean agendaPrimero : new boolean[]{true, false}) {
            fixtures();
            EmpleadoServiceImpl service = servicio(sesionesLentas());
            Long vetId = vet.getId();
            CountDownLatch salida = new CountDownLatch(1);
            AtomicReference<Throwable> inesperado = new AtomicReference<>();
            Runnable baja = () -> service.cambiarEstado(vetId, false, TipoInactividad.BAJA, "Prueba");
            Runnable agendar = () -> {
                Empleado bloqueado = empleadoRepository.findByIdForAppointmentWrite(vetId).orElseThrow();
                if (!Boolean.TRUE.equals(bloqueado.getEstado())) {
                    throw new IllegalArgumentException("No se puede asignar la cita a un empleado inactivo");
                }
                Mascota mascota = new Mascota();
                mascota.setApoderado(dueno);
                mascota.setNombreCompleto("Firulais");
                mascota.setEspecie(EspecieMascota.PERRO);
                mascota.setSexo(SexoMascota.MACHO);
                mascota.setActivo(true);
                mascota.setUuid(java.util.UUID.randomUUID().toString());
                mascota = mascotaRepository.save(mascota);
                LocalDateTime inicio = LocalDateTime.now().plusDays(1);
                Cita cita = new Cita();
                cita.setMascota(mascota);
                cita.setEmpleado(bloqueado);
                cita.setServicio(servicio);
                cita.setMotivoCita("Control");
                cita.setFechaHoraInicio(inicio);
                cita.setFechaHoraFin(inicio.plusMinutes(30));
                cita.setDuracionMinutos(30);
                cita.setEstado(EstadoCita.PROGRAMADA);
                cita.setTotalServicio(new BigDecimal("100.00"));
                cita.setMontoPagado(BigDecimal.ZERO);
                cita.setEliminada(false);
                cita.setEsEmergencia(false);
                citaRepository.save(cita);
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };
            ExecutorService pool = Executors.newFixedThreadPool(2);
            Future<Boolean> primera = pool.submit(comoAdministrador(agendaPrimero ? agendar : baja, salida, 0, inesperado));
            Future<Boolean> segunda = pool.submit(comoAdministrador(agendaPrimero ? baja : agendar, salida, 100, inesperado));
            salida.countDown();
            primera.get();
            segunda.get();
            pool.shutdownNow();

            assertThat(inesperado.get()).isNull();
            boolean inactivo = !empleadoRepository.findById(vetId).orElseThrow().getEstado();
            boolean conCitaVigente = citaRepository.existsCitaVigenteByEmpleadoId(vetId, LocalDateTime.now());
            assertThat(inactivo && conCitaVigente)
                    .as("veterinario inactivo con una cita vigente (agenda primero: %s)", agendaPrimero)
                    .isFalse();
            limpiar();
        }
    }
}
