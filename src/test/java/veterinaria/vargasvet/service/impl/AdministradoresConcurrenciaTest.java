package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
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
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.TipoEmpleadoRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioMembresiaRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AccountClosureGuard;
import veterinaria.vargasvet.service.AdministratorProtection;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.SessionSecurityService;
import veterinaria.vargasvet.util.AppClock;
import veterinaria.vargasvet.util.BusinessValidator;

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

/**
 * Dos administradores que se dan de baja entre sí al mismo tiempo: cada uno vería al otro como el
 * reemplazo. Solo una de las dos operaciones puede prosperar, para que la empresa conserve un administrador.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AdministradoresConcurrenciaTest {

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
    @Autowired private UsuarioPorRolRepository usuarioPorRolRepository;
    @Autowired private UsuarioEmpresaCredencialRepository credencialRepository;
    @Autowired private UsuarioMembresiaRepository usuarioMembresiaRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
        usuarioPorRolRepository.deleteAll();
        empleadoRepository.deleteAll();
        usuarioRepository.deleteAll();
        roleRepository.deleteAll();
        companyRepository.deleteAll();
    }

    private Usuario administrador(Company company, Role rol, String correo, String username, String dni) {
        Usuario usuario = new Usuario();
        usuario.setEmail(correo);
        usuario.setUsername(username);
        usuario.setNombre(username);
        usuario.setApellido("Prueba");
        usuario.setDni(dni);
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(company);
        usuario = usuarioRepository.save(usuario);

        Empleado empleado = new Empleado();
        empleado.setUser(usuario);
        empleado.setCompany(company);
        empleado.setEstado(true);
        empleado.setGenero(Genero.FEMENINO);
        empleado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        empleado.setNumeroDocumentoIdentidad(dni);
        empleado.setFechaIngreso(AppClock.today());
        empleadoRepository.save(empleado);

        UsuarioPorRol asignacion = new UsuarioPorRol();
        asignacion.setUsuario(usuario);
        asignacion.setRol(rol);
        asignacion.setCompany(company);
        usuarioPorRolRepository.save(asignacion);
        return usuario;
    }

    @Test
    void dosAdministradoresQueSeDanDeBajaEntreSiNoDejanALaEmpresaSinAdministrador() throws Exception {
        Company company = new Company();
        company.setName("Clínica Prueba");
        company.setSlug("clinica-prueba");
        company.setActivo(true);
        company = companyRepository.save(company);
        Role rolAdmin = new Role();
        rolAdmin.setName("ROLE_ADMIN_CONC");
        rolAdmin.setScope(RoleScope.STAFF);
        rolAdmin.setPurpose(RolePurpose.COMPANY_ADMIN);
        rolAdmin.setActivo(true);
        rolAdmin = roleRepository.save(rolAdmin);
        Usuario lucia = administrador(company, rolAdmin, "lucia@clinica.test", "lucia", "11111111");
        Usuario marco = administrador(company, rolAdmin, "marco@clinica.test", "marco", "22222222");
        Long empleadoLucia = empleadoRepository.findActiveByUserId(lucia.getId()).orElseThrow().getId();
        Long empleadoMarco = empleadoRepository.findActiveByUserId(marco.getId()).orElseThrow().getId();

        SessionSecurityService sessions = mock(SessionSecurityService.class);
        doAnswer(invocation -> {
            Thread.sleep(400);
            return null;
        }).when(sessions).invalidateSessionsForCompany(any(), any());
        CompanyMembershipServiceImpl membership = new CompanyMembershipServiceImpl(
                empleadoRepository, apoderadoRepository, usuarioMembresiaRepository, usuarioRepository, companyRepository);
        EmpleadoServiceImpl service = new EmpleadoServiceImpl(
                usuarioRepository, roleRepository, empleadoRepository, especialidadRepository, tipoEmpleadoRepository,
                companyRepository, horarioEmpleadoRepository, companyOperatingHourRepository, companyExceptionRepository,
                citaRepository, mock(PasswordEncoder.class), new UserMapper(new ModelMapper()), mock(EmailService.class),
                mock(BusinessValidator.class), mock(AuditLogService.class), usuarioPorRolRepository, sessions, membership,
                credencialRepository, mock(UsuarioContactoService.class),
                new AdministratorProtection(usuarioPorRolRepository, membership, companyRepository),
                mock(AccountClosureGuard.class), mock(veterinaria.vargasvet.service.CajasAbiertasDelPersonal.class),
                mock(veterinaria.vargasvet.service.AccessRestoredNotifier.class),
                org.mockito.Mockito.mock(veterinaria.vargasvet.service.ConsentimientoDatosService.class));

        org.springframework.test.util.ReflectionTestUtils.setField(service, "entityManager", entityManager);
        Integer companyId = company.getId();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        AtomicReference<Throwable> inesperado = new AtomicReference<>();
        java.util.function.BiFunction<Usuario, Long, Callable<Boolean>> baja = (actor, objetivo) -> () -> {
            UsuarioPrincipal principal = new UsuarioPrincipal(actor.getId(), actor.getEmail(), "", List.of(), companyId,
                    2, RoleScope.STAFF, RolePurpose.COMPANY_ADMIN, 1L);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
            salida.await();
            try {
                tx.executeWithoutResult(status ->
                        service.cambiarEstado(objetivo, false, TipoInactividad.SUSPENSION, "Prueba"));
                return true;
            } catch (IllegalStateException e) {
                return false;
            } catch (Exception e) {
                inesperado.set(e);
                return false;
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
        Future<Boolean> luciaSuspendeAMarco = pool.submit(baja.apply(lucia, empleadoMarco));
        Future<Boolean> marcoSuspendeALucia = pool.submit(baja.apply(marco, empleadoLucia));
        salida.countDown();
        int exitos = (luciaSuspendeAMarco.get() ? 1 : 0) + (marcoSuspendeALucia.get() ? 1 : 0);
        pool.shutdownNow();

        assertThat(inesperado.get()).isNull();
        assertThat(exitos).isEqualTo(1);
        assertThat(empleadoRepository.findAll().stream().filter(Empleado::getEstado).count()).isEqualTo(1);
    }
}
