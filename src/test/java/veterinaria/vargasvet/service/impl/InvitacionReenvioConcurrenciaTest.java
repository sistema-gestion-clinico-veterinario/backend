package veterinaria.vargasvet.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.mapper.UserMapper;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyExceptionRepository;
import veterinaria.vargasvet.repository.CompanyOperatingHourRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.EspecialidadRepository;
import veterinaria.vargasvet.repository.HorarioEmpleadoRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.TipoEmpleadoRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AccountClosureGuard;
import veterinaria.vargasvet.service.AdministratorProtection;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CompanyMembershipService;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.SessionSecurityService;
import veterinaria.vargasvet.service.impl.UsuarioContactoService;
import veterinaria.vargasvet.util.AppClock;
import veterinaria.vargasvet.util.BusinessValidator;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Dos reenvíos de la invitación al mismo tiempo (doble clic, o dos administradores): solo uno
 * debe generar enlace y correo; el otro encuentra el intervalo mínimo ya corriendo.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InvitacionReenvioConcurrenciaTest {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private EspecialidadRepository especialidadRepository;
    @Autowired private TipoEmpleadoRepository tipoEmpleadoRepository;
    @Autowired private HorarioEmpleadoRepository horarioEmpleadoRepository;
    @Autowired private CompanyOperatingHourRepository companyOperatingHourRepository;
    @Autowired private CompanyExceptionRepository companyExceptionRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private UsuarioPorRolRepository usuarioPorRolRepository;
    @Autowired private UsuarioEmpresaCredencialRepository credencialRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @PersistenceContext private EntityManager entityManager;

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
        empleadoRepository.deleteAll();
        usuarioRepository.deleteAll();
        companyRepository.deleteAll();
    }

    @Test
    void dosReenviosSimultaneosGeneranUnSoloEnlaceYUnSoloCorreo() throws Exception {
        Company company = new Company();
        company.setName("Clínica Prueba");
        company.setSlug("clinica-prueba");
        company.setActivo(true);
        company = companyRepository.save(company);

        Usuario usuario = new Usuario();
        usuario.setEmail("ana@clinica.test");
        usuario.setUsername("ana");
        usuario.setNombre("Ana");
        usuario.setApellido("Pérez");
        usuario.setActivo(false);
        usuario.setEmailVerified(false);
        usuario.setCompany(company);
        usuario.setVerificationToken("hash-anterior");
        usuario.setVerificationTokenExpiresAt(AppClock.now().plusHours(24).minusHours(2));
        usuario = usuarioRepository.save(usuario);

        Empleado empleado = new Empleado();
        empleado.setUser(usuario);
        empleado.setCompany(company);
        empleado.setEstado(true);
        empleado.setFechaIngreso(AppClock.today());
        empleado.setGenero(veterinaria.vargasvet.domain.enums.Genero.FEMENINO);
        empleado.setTipoDocumentoIdentidad(veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad.DNI);
        empleado.setNumeroDocumentoIdentidad("12345678");
        empleado = empleadoRepository.save(empleado);

        EmailService emailService = mock(EmailService.class);
        AtomicInteger correos = new AtomicInteger();
        when(emailService.createMail(anyString(), anyString(), any()))
                .thenAnswer(i -> new Mail(null, i.getArgument(0), i.getArgument(1), i.getArgument(2)));
        when(emailService.sendEmailWithRetry(any(), anyString())).thenAnswer(i -> {
            correos.incrementAndGet();
            Thread.sleep(400);
            return CompletableFuture.completedFuture(true);
        });

        CompanyMembershipService membership = mock(CompanyMembershipService.class);
        EmpleadoServiceImpl service = new EmpleadoServiceImpl(
                usuarioRepository, roleRepository, empleadoRepository, especialidadRepository, tipoEmpleadoRepository,
                companyRepository, horarioEmpleadoRepository, companyOperatingHourRepository, companyExceptionRepository,
                citaRepository, new BCryptPasswordEncoder(), new UserMapper(new ModelMapper()), emailService,
                mock(BusinessValidator.class), mock(AuditLogService.class), usuarioPorRolRepository,
                mock(SessionSecurityService.class), membership, credencialRepository, mock(UsuarioContactoService.class),
                mock(AdministratorProtection.class), mock(AccountClosureGuard.class),
                mock(veterinaria.vargasvet.service.CajasAbiertasDelPersonal.class),
                mock(veterinaria.vargasvet.service.AccessRestoredNotifier.class),
                org.mockito.Mockito.mock(veterinaria.vargasvet.service.ConsentimientoDatosService.class));
        ReflectionTestUtils.setField(service, "appUrl", "https://frontend.test");
        ReflectionTestUtils.setField(service, "verificationTokenValidityHours", 24L);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);

        UsuarioPrincipal principal = new UsuarioPrincipal(
                1, "admin@clinica.test", "", List.of(), company.getId(),
                2, RoleScope.STAFF, RolePurpose.COMPANY_ADMIN, 1L);
        Long empleadoId = empleado.getId();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<Boolean> reenvio = () -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
            salida.await();
            try {
                tx.executeWithoutResult(status -> service.reenviarInvitacion(empleadoId));
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
        Future<Boolean> primero = pool.submit(reenvio);
        Future<Boolean> segundo = pool.submit(reenvio);
        salida.countDown();
        int exitos = (primero.get() ? 1 : 0) + (segundo.get() ? 1 : 0);
        pool.shutdownNow();

        assertThat(exitos).isEqualTo(1);
        assertThat(correos.get()).isEqualTo(1);
    }
}
