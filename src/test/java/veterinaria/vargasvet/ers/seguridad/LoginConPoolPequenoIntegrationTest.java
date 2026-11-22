package veterinaria.vargasvet.ers.seguridad;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.dto.request.AdminLoginDTO;
import veterinaria.vargasvet.dto.request.LoginDTO;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CategoriaProductoRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.ConsultaRepository;
import veterinaria.vargasvet.repository.DiagnosticoRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.HistoriaClinicaRepository;
import veterinaria.vargasvet.repository.LoteRepository;
import veterinaria.vargasvet.repository.MarcaProductoRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.PrescripcionRepository;
import veterinaria.vargasvet.repository.ProductoRepository;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.ServiciosVeterinariosRepository;
import veterinaria.vargasvet.repository.TratamientoRepository;
import veterinaria.vargasvet.repository.UnidadMedidaRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaContactoRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioMembresiaRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.repository.VistaRepository;
import veterinaria.vargasvet.security.AccountLockoutService;
import veterinaria.vargasvet.security.SharedRateLimitService;
import veterinaria.vargasvet.service.CompanyRoleProvisioningService;
import veterinaria.vargasvet.service.UsuarioService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Con un pool de 3 conexiones, 12 inicios de sesión simultáneos deben terminar todos. Un login que mantiene su
 * conexión mientras pide otra (comprobación de límite, bloqueo o auditoría en transacción nueva) deja a todos los
 * hilos esperando una conexión que solo liberaría otro hilo atascado, hasta agotar el tiempo de espera.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(LoginConPoolPequenoIntegrationTest.Configuracion.class)
@TestPropertySource(locations = {"file:src/main/resources/application.properties", "classpath:aislamiento.properties"},
        properties = {
                "spring.datasource.url=jdbc:h2:mem:vargasvet_pool_pequeno;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.hikari.maximum-pool-size=3",
                "spring.datasource.hikari.minimum-idle=1",
                "spring.datasource.hikari.connection-timeout=4000"})
class LoginConPoolPequenoIntegrationTest {

    private static final int SESIONES_SIMULTANEAS = 12;

    @Autowired private UsuarioService usuarioService;
    @Autowired private DosEmpresasSembradas sembrador;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UsuarioPorRolRepository asignaciones;
    @Autowired private UsuarioEmpresaCredencialRepository credenciales;
    @Autowired private PasswordEncoder codificador;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private veterinaria.vargasvet.service.AuthenticationAuditService auditoria;

    private DosEmpresasSembradas.Empresa empresa;

    @BeforeAll
    void sembrar() {
        empresa = sembrador.sembrar("TKP");
        sembrarAdministradorDePlataforma();
        suspender("cliente2.tkp");
    }

    private void suspender(String username) {
        jdbc.update("UPDATE usuario SET activo = FALSE WHERE username = ?", username);
    }

    private void sembrarAdministradorDePlataforma() {
        Role rol = new Role();
        rol.setName("ROLE_ADMIN_PLATAFORMA_PRUEBA");
        rol.setScope(RoleScope.PLATFORM);
        rol.setPurpose(RolePurpose.PLATFORM_ADMIN);
        rol = roleRepository.save(rol);

        Usuario usuario = new Usuario();
        usuario.setEmail("plataforma@prueba.test");
        usuario.setUsername("plataforma.prueba");
        usuario.setNombre("Plataforma");
        usuario.setApellido("Prueba");
        usuario.setDni("99999999");
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario = usuarioRepository.save(usuario);

        UsuarioEmpresaCredencial credencial = new UsuarioEmpresaCredencial();
        credencial.setUsuario(usuario);
        credencial.setPassword(codificador.encode(DosEmpresasSembradas.CLAVE));
        credencial.setPasswordChanged(true);
        credencial.setCreatedAt(LocalDateTime.now());
        credencial.setUpdatedAt(LocalDateTime.now());
        credenciales.save(credencial);

        UsuarioPorRol asignacion = new UsuarioPorRol();
        asignacion.setUsuario(usuario);
        asignacion.setRol(rol);
        asignaciones.save(asignacion);
    }

    @Test
    void variosInicioDeSesionSimultaneosTerminanConUnPoolMasPequenoQueLosHilos() throws Exception {
        List<String> fallos = simultaneos(i -> usuarioService.login(
                credenciales(i % 2 == 0 ? "admin.tkp" : "cliente1.tkp", DosEmpresasSembradas.CLAVE)).getToken());

        assertThat(fallos).as("inicios de sesión que fallaron con un pool de 3 conexiones").isEmpty();
    }

    @Test
    void variosInicioDeSesionConGoogleSimultaneosTerminan() throws Exception {
        List<String> fallos = simultaneos(i -> usuarioService.loginWithGoogle(
                i % 2 == 0 ? "admin.tkp@tkp.test" : "cliente1.tkp@tkp.test", empresa.slug()).getToken());

        assertThat(fallos).as("inicios de sesión con Google que fallaron con un pool de 3 conexiones").isEmpty();
    }

    @Test
    void variosInicioDeSesionDeAdministradorDePlataformaSimultaneosTerminan() throws Exception {
        List<String> fallos = simultaneos(i -> {
            AdminLoginDTO dto = new AdminLoginDTO();
            dto.setUsername("plataforma.prueba");
            dto.setPassword(DosEmpresasSembradas.CLAVE);
            return usuarioService.adminLogin(dto).getToken();
        });

        assertThat(fallos).as("inicios de sesión de administrador que fallaron con un pool de 3 conexiones").isEmpty();
    }

    @Test
    void variasContrasenasIncorrectasSimultaneasSeRechazanSinAgotarElPool() throws Exception {
        List<String> fallos = simultaneos(i -> {
            try {
                usuarioService.login(credenciales(i % 2 == 0 ? "admin.tkp" : "cliente1.tkp", "incorrecta-" + i));
                return "entró con una contraseña incorrecta";
            } catch (BadCredentialsException esperado) {
                return "rechazada";
            }
        });

        assertThat(fallos).as("rechazos que terminaron en un error distinto de credenciales inválidas").isEmpty();
    }

    @Test
    void variasCuentasSuspendidasSimultaneasSeRechazanSinAgotarElPool() throws Exception {
        List<String> fallos = simultaneos(i -> {
            try {
                usuarioService.login(credenciales("cliente2.tkp", DosEmpresasSembradas.CLAVE));
                return "entró una cuenta suspendida";
            } catch (DisabledException esperado) {
                return "rechazada";
            }
        });

        assertThat(fallos).as("rechazos que terminaron en un error distinto de cuenta deshabilitada").isEmpty();
    }

    @Test
    void registrarCredencialesInvalidasNoRetieneUnaConexionMientrasRegistraElBloqueo() {
        Configuracion.conexionesActivasEnElBloqueo.set(-1);
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                new org.springframework.web.context.request.ServletRequestAttributes(
                        new org.springframework.mock.web.MockHttpServletRequest()));
        try {
            auditoria.recordInvalidCredentials(null, "alguien", "clave-de-prueba");
        } finally {
            org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        }

        assertThat(Configuracion.conexionesActivasEnElBloqueo.get())
                .as("conexiones activas mientras se registra el intento fallido (solo debe estar la del propio registro)")
                .isEqualTo(1);
    }

    private List<String> simultaneos(java.util.function.IntFunction<String> operacion) throws Exception {
        ExecutorService hilos = Executors.newFixedThreadPool(SESIONES_SIMULTANEAS);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<String>> resultados = new ArrayList<>();
        for (int i = 0; i < SESIONES_SIMULTANEAS; i++) {
            int indice = i;
            Callable<String> tarea = () -> {
                org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                        new org.springframework.web.context.request.ServletRequestAttributes(
                                new org.springframework.mock.web.MockHttpServletRequest()));
                try {
                    salida.await();
                    return operacion.apply(indice);
                } finally {
                    org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
                }
            };
            resultados.add(hilos.submit(tarea));
        }
        salida.countDown();

        List<String> fallos = new ArrayList<>();
        for (Future<String> resultado : resultados) {
            try {
                String valor = resultado.get(60, TimeUnit.SECONDS);
                if (valor == null || valor.isBlank() || valor.startsWith("entr")) {
                    fallos.add("resultado inesperado: " + valor);
                }
            } catch (Exception e) {
                fallos.add(e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
        hilos.shutdownNow();
        return fallos;
    }

    private LoginDTO credenciales(String usuario, String clave) {
        LoginDTO dto = new LoginDTO();
        dto.setSlug(empresa.slug());
        dto.setUsername(usuario);
        dto.setPassword(clave);
        return dto;
    }

    @TestConfiguration
    static class Configuracion {

        static final java.util.concurrent.atomic.AtomicInteger conexionesActivasEnElBloqueo =
                new java.util.concurrent.atomic.AtomicInteger(-1);

        @Bean
        @Primary
        SharedRateLimitService limitadorQueUsaOtraConexion(JdbcTemplate jdbc) {
            return new SharedRateLimitService(jdbc) {
                @Override
                @Transactional(propagation = Propagation.REQUIRES_NEW)
                public boolean tryConsume(String namespace, String identifier, int capacity, Duration period) {
                    jdbc.queryForObject("SELECT 1", Integer.class);
                    return true;
                }

                @Override
                @Transactional(propagation = Propagation.REQUIRES_NEW)
                public void enforce(String namespace, String identifier, int capacity, Duration period) {
                    jdbc.queryForObject("SELECT 1", Integer.class);
                }
            };
        }

        @Bean
        @Primary
        AccountLockoutService bloqueoQueUsaOtraConexion(JdbcTemplate jdbc, javax.sql.DataSource dataSource) {
            return new AccountLockoutService(jdbc) {
                @Override
                @Transactional(propagation = Propagation.REQUIRES_NEW)
                public void assertNotLocked(String identifier) {
                    jdbc.queryForObject("SELECT 1", Integer.class);
                }

                @Override
                @Transactional(propagation = Propagation.REQUIRES_NEW)
                public void registerFailedAttempt(String identifier) {
                    jdbc.queryForObject("SELECT 1", Integer.class);
                    if (dataSource instanceof com.zaxxer.hikari.HikariDataSource hikari) {
                        conexionesActivasEnElBloqueo.set(hikari.getHikariPoolMXBean().getActiveConnections());
                    }
                }

                @Override
                @Transactional(propagation = Propagation.REQUIRES_NEW)
                public void registerSuccessfulLogin(String identifier) {
                    jdbc.queryForObject("SELECT 1", Integer.class);
                }
            };
        }

        @Bean
        DosEmpresasSembradas sembradorDeClinicas(CompanyRepository companias, UsuarioRepository usuarios,
                                                 ApoderadoRepository apoderados, MascotaRepository mascotas,
                                                 EmpleadoRepository empleados, ServiciosVeterinariosRepository servicios,
                                                 CitaRepository citas, HistoriaClinicaRepository historias,
                                                 ConsultaRepository consultas, DiagnosticoRepository diagnosticos,
                                                 TratamientoRepository tratamientos, PrescripcionRepository prescripciones,
                                                 CategoriaProductoRepository categorias, MarcaProductoRepository marcas,
                                                 UnidadMedidaRepository unidades, ProductoRepository productos,
                                                 LoteRepository lotes, RoleRepository roles,
                                                 UsuarioPorRolRepository asignaciones,
                                                 UsuarioEmpresaContactoRepository contactos,
                                                 UsuarioEmpresaCredencialRepository credenciales,
                                                 UsuarioMembresiaRepository membresias, VistaRepository vistas,
                                                 RolVistaPermisoRepository permisos,
                                                 CompanyRoleProvisioningService aprovisionamiento,
                                                 PasswordEncoder codificador, PlatformTransactionManager transacciones) {
            return new DosEmpresasSembradas(companias, usuarios, apoderados, mascotas, empleados, servicios, citas,
                    historias, consultas, diagnosticos, tratamientos, prescripciones, categorias, marcas, unidades,
                    productos, lotes, roles, asignaciones, contactos, credenciales, membresias, vistas, permisos,
                    aprovisionamiento, codificador, new TransactionTemplate(transacciones));
        }
    }
}
