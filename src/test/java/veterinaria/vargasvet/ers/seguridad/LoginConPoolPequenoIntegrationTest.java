package veterinaria.vargasvet.ers.seguridad;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
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

    private DosEmpresasSembradas.Empresa empresa;

    @BeforeAll
    void sembrar() {
        empresa = sembrador.sembrar("TKP");
    }

    @Test
    void variosInicioDeSesionSimultaneosTerminanConUnPoolMasPequenoQueLosHilos() throws Exception {
        ExecutorService hilos = Executors.newFixedThreadPool(SESIONES_SIMULTANEAS);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<String>> resultados = new ArrayList<>();
        for (int i = 0; i < SESIONES_SIMULTANEAS; i++) {
            String usuario = i % 2 == 0 ? "admin.tkp" : "cliente1.tkp";
            Callable<String> sesion = () -> {
                salida.await();
                return usuarioService.login(credenciales(usuario)).getToken();
            };
            resultados.add(hilos.submit(sesion));
        }
        salida.countDown();

        List<String> fallos = new ArrayList<>();
        for (Future<String> resultado : resultados) {
            try {
                assertThat(resultado.get(60, TimeUnit.SECONDS)).isNotBlank();
            } catch (Exception e) {
                fallos.add(e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
        hilos.shutdownNow();

        assertThat(fallos).as("inicios de sesión que fallaron con un pool de 3 conexiones").isEmpty();
    }

    private LoginDTO credenciales(String usuario) {
        LoginDTO dto = new LoginDTO();
        dto.setSlug(empresa.slug());
        dto.setUsername(usuario);
        dto.setPassword(DosEmpresasSembradas.CLAVE);
        return dto;
    }

    @TestConfiguration
    static class Configuracion {

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
        AccountLockoutService bloqueoQueUsaOtraConexion(JdbcTemplate jdbc) {
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
