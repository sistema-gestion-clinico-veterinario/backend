package veterinaria.vargasvet.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmailChangeRequestRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.PasswordResetTokenRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SharedRateLimitService;
import veterinaria.vargasvet.security.UsuarioPrincipal;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Dos administradores piden a la vez el cambio de correo de la misma persona: la segunda
 * solicitud reemplaza a la primera, sin error y dejando una sola solicitud vigente.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class EmailChangeConcurrenciaTest {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EmailChangeRequestRepository emailChangeRequestRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioEmpresaCredencialRepository credencialRepository;
    @Autowired private UsuarioPorRolRepository usuarioPorRolRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @PersistenceContext private EntityManager entityManager;

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
        emailChangeRequestRepository.deleteAll();
        usuarioRepository.deleteAll();
        companyRepository.deleteAll();
    }

    private record Resultado(boolean primeraOk, boolean segundaOk, Throwable fallo, long solicitudes,
                             int correosConEnlace, long enlacesVigentes) {
    }

    private Resultado ejecutar(String segundoCorreo) throws Exception {
        Company company = new Company();
        company.setName("Clínica Prueba");
        company.setSlug("clinica-prueba");
        company.setActivo(true);
        company = companyRepository.save(company);

        Usuario usuario = new Usuario();
        usuario.setEmail("sin.acceso@clinica.test");
        usuario.setUsername("ana");
        usuario.setNombre("Ana");
        usuario.setApellido("Pérez");
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(company);
        usuario = usuarioRepository.save(usuario);
        Integer usuarioId = usuario.getId();

        EmailService emailService = mock(EmailService.class);
        java.util.List<String> enlaces = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        when(emailService.createMail(anyString(), anyString(), any()))
                .thenAnswer(i -> {
                    java.util.Map<String, Object> modelo = i.getArgument(2);
                    Object url = modelo.get("confirmationUrl");
                    if (url != null && String.valueOf(url).contains("type=nuevo")) {
                        String u = String.valueOf(url);
                        enlaces.add(u.substring(u.indexOf("&token=") + 7));
                    }
                    return new Mail(null, i.getArgument(0), i.getArgument(1), i.getArgument(2));
                });
        when(emailService.sendEmailWithRetry(any(), anyString())).thenAnswer(i -> {
            Thread.sleep(400);
            return CompletableFuture.completedFuture(true);
        });
        CompanyMembershipService membership = mock(CompanyMembershipService.class);
        when(membership.hasActiveMembership(usuarioId, company.getId())).thenReturn(true);

        EmailChangeService service = new EmailChangeService(
                usuarioRepository, emailChangeRequestRepository, mock(PasswordEncoder.class), emailService,
                mock(SessionSecurityService.class), mock(AuditLogService.class), mock(SharedRateLimitService.class),
                credencialRepository, companyRepository, membership, usuarioPorRolRepository,
                mock(PasswordResetTokenRepository.class), mock(EmpleadoRepository.class),
                mock(ApoderadoRepository.class), mock(MascotaRepository.class));
        ReflectionTestUtils.setField(service, "frontendUrl", "https://frontend.test");
        ReflectionTestUtils.setField(service, "defaultCompanyName", "Veterinaria");
        ReflectionTestUtils.setField(service, "administrativeValidityHours", 24L);
        ReflectionTestUtils.setField(service, "recoveryPerAccountPerHour", 3);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);

        UsuarioPrincipal principal = new UsuarioPrincipal(
                99, "admin@clinica.test", "", List.of(), company.getId(),
                2, RoleScope.STAFF, RolePurpose.COMPANY_ADMIN, 1L);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        java.util.concurrent.atomic.AtomicReference<Throwable> fallo = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.function.Function<String, Callable<Boolean>> solicitud = nuevoCorreo -> () -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
            salida.await();
            try {
                tx.executeWithoutResult(status -> service.requestAdministrativeChange(usuarioId, nuevoCorreo, null));
                return true;
            } catch (Exception e) {
                fallo.set(e);
                return false;
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
        Future<Boolean> primera = pool.submit(solicitud.apply("uno@clinica.test"));
        Future<Boolean> segunda = pool.submit(solicitud.apply(segundoCorreo));
        salida.countDown();
        boolean primeraOk = primera.get();
        boolean segundaOk = segunda.get();
        pool.shutdownNow();

        String guardado = emailChangeRequestRepository.findAll().get(0).getNewEmailTokenHash();
        long vigentes = enlaces.stream()
                .filter(t -> veterinaria.vargasvet.security.SecurityTokenUtils.hash(t).equals(guardado)).count();
        return new Resultado(primeraOk, segundaOk, fallo.get(), emailChangeRequestRepository.count(),
                enlaces.size(), vigentes);
    }

    @Test
    void dosSolicitudesSimultaneasConCorreosDistintosDejanUnaSolaVigente() throws Exception {
        Resultado r = ejecutar("dos@clinica.test");

        assertThat(r.fallo()).isNull();
        assertThat(r.primeraOk() && r.segundaOk()).isTrue();
        assertThat(r.solicitudes()).isEqualTo(1);
        assertThat(r.enlacesVigentes()).isEqualTo(1);
    }

    @Test
    void elMismoPedidoDosVecesALaVezEnviaUnSoloCorreoYDejaValidoElEnlace() throws Exception {
        Resultado r = ejecutar("uno@clinica.test");

        assertThat(r.fallo()).isNull();
        assertThat(r.primeraOk() && r.segundaOk()).isTrue();
        assertThat(r.solicitudes()).isEqualTo(1);
        assertThat(r.correosConEnlace()).isEqualTo(1);
        assertThat(r.enlacesVigentes()).isEqualTo(1);
    }
}
