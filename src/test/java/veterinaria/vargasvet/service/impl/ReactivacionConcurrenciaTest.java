package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.MotivoBajaMascota;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.SexoMascota;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.domain.enums.TipoInactividad;
import veterinaria.vargasvet.dto.request.EstadoMascotaRequest;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Reactivar al dueño y registrar el fallecimiento de una de sus mascotas a la vez: el fallecimiento nunca se pierde,
 * llegue primero la reactivación o llegue primero el registro.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReactivacionConcurrenciaTest {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private MascotaPersonaRelacionRepository relacionRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
        mascotaRepository.deleteAll();
        apoderadoRepository.deleteAll();
        usuarioRepository.deleteAll();
        companyRepository.deleteAll();
    }

    @Test
    void elFallecimientoNoSePierdeSeaCualSeaElOrden() throws Exception {
        for (boolean reactivaPrimero : new boolean[]{true, false}) {
            Company company = new Company();
            company.setName("Clínica Prueba");
            company.setSlug("clinica-prueba");
            company.setActivo(true);
            company = companyRepository.save(company);
            Usuario usuario = new Usuario();
            usuario.setEmail("ana@clinica.test");
            usuario.setUsername("ana");
            usuario.setNombre("Ana");
            usuario.setApellido("Prueba");
            usuario.setDni("11111111");
            usuario.setActivo(true);
            usuario.setEmailVerified(true);
            usuario.setCompany(company);
            usuario = usuarioRepository.save(usuario);
            Apoderado ana = new Apoderado();
            ana.setUser(usuario);
            ana.setCompany(company);
            ana.setEstado(false);
            ana.setTipoInactividad(TipoInactividad.BAJA);
            ana.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
            ana.setNumeroDocumento("11111111");
            ana.setGenero(Genero.FEMENINO);
            ana = apoderadoRepository.save(ana);
            Mascota luna = new Mascota();
            luna.setApoderado(ana);
            luna.setNombreCompleto("Luna");
            luna.setEspecie(EspecieMascota.PERRO);
            luna.setSexo(SexoMascota.HEMBRA);
            luna.setActivo(false);
            luna.setMotivoBaja(MotivoBajaMascota.BAJA_DEL_PROPIETARIO);
            luna.setUuid(java.util.UUID.randomUUID().toString());
            luna = mascotaRepository.save(luna);
            Long anaId = ana.getId();
            Long lunaId = luna.getId();
            Integer companyId = company.getId();

            AuditLogService lento = mock(AuditLogService.class);
            doAnswer(i -> { Thread.sleep(500); return null; }).when(lento)
                    .log(any(Integer.class), anyString(), anyString(), anyString());
            doAnswer(i -> { Thread.sleep(500); return null; }).when(lento)
                    .log(anyString(), anyString(), anyString());
            PetOwnershipService pets = new PetOwnershipService(mascotaRepository, relacionRepository, citaRepository, lento);
            ReflectionTestUtils.setField(pets, "entityManager", entityManager);
            ApoderadoServiceImpl apoderadoService = new ApoderadoServiceImpl(
                    usuarioRepository, apoderadoRepository, mascotaRepository,
                    mock(veterinaria.vargasvet.repository.RefreshTokenRepository.class),
                    mock(veterinaria.vargasvet.service.RoleAssignmentService.class), companyRepository,
                    mock(org.springframework.security.crypto.password.PasswordEncoder.class),
                    mock(veterinaria.vargasvet.mapper.UserMapper.class), mock(BusinessValidator.class),
                    mock(veterinaria.vargasvet.service.EmailService.class), lento,
                    mock(veterinaria.vargasvet.service.CompanyRoleProvisioningService.class),
                    mock(veterinaria.vargasvet.service.SessionSecurityService.class),
                    mock(veterinaria.vargasvet.service.CompanyMembershipService.class), citaRepository,
                    mock(veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository.class),
                    mock(veterinaria.vargasvet.service.impl.UsuarioContactoService.class), pets,
                    mock(veterinaria.vargasvet.service.AccountClosureGuard.class),
                    mock(veterinaria.vargasvet.service.AdministratorProtection.class),
                    mock(veterinaria.vargasvet.service.AccessRestoredNotifier.class),
                org.mockito.Mockito.mock(veterinaria.vargasvet.service.ConsentimientoDatosService.class));
            ReflectionTestUtils.setField(apoderadoService, "entityManager", entityManager);
            MascotaServiceImpl mascotaService = new MascotaServiceImpl(
                    mascotaRepository, apoderadoRepository, citaRepository,
                    mock(veterinaria.vargasvet.repository.HistoriaClinicaRepository.class),
                    mock(veterinaria.vargasvet.mapper.MascotaMapper.class), mock(BusinessValidator.class),
                    lento, mock(veterinaria.vargasvet.repository.RazaRepository.class),
                    mock(veterinaria.vargasvet.service.ApoderadoService.class),
                    mock(veterinaria.vargasvet.service.MascotaRelacionService.class), pets);
            EstadoMascotaRequest fallecimiento = new EstadoMascotaRequest();
            fallecimiento.setActive(false);
            fallecimiento.setMotivoBaja(MotivoBajaMascota.FALLECIMIENTO);

            Runnable reactivar = () -> apoderadoService.cambiarEstado(anaId, true, null, null);
            Runnable registrar = () -> mascotaService.cambiarEstado(lunaId, fallecimiento);
            CountDownLatch salida = new CountDownLatch(1);
            AtomicReference<Throwable> fallo = new AtomicReference<>();
            ExecutorService pool = Executors.newFixedThreadPool(2);
            Future<Boolean> primera = pool.submit(enHilo(reactivaPrimero ? reactivar : registrar, salida, 0, companyId, fallo));
            Future<Boolean> segunda = pool.submit(enHilo(reactivaPrimero ? registrar : reactivar, salida, 150, companyId, fallo));
            salida.countDown();
            boolean primeraOk = primera.get();
            boolean segundaOk = segunda.get();
            pool.shutdownNow();

            Mascota despues = mascotaRepository.findById(lunaId).orElseThrow();
            String contexto = "reactiva primero: " + reactivaPrimero;
            assertThat(fallo.get()).as(contexto).isNull();
            assertThat(primeraOk && segundaOk).as(contexto).isTrue();
            assertThat(despues.getActivo()).as(contexto).isFalse();
            assertThat(despues.getMotivoBaja()).as(contexto).isEqualTo(MotivoBajaMascota.FALLECIMIENTO);
            limpiar();
        }
    }

    private Callable<Boolean> enHilo(Runnable accion, CountDownLatch salida, long esperaMs, Integer companyId,
                                     AtomicReference<Throwable> fallo) {
        return () -> {
            UsuarioPrincipal principal = new UsuarioPrincipal(99, "admin@clinica.test", "", List.of(), companyId,
                    2, RoleScope.STAFF, RolePurpose.COMPANY_ADMIN, 1L);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
            salida.await();
            Thread.sleep(esperaMs);
            try {
                new TransactionTemplate(transactionManager).executeWithoutResult(s -> accion.run());
                return true;
            } catch (Exception e) {
                fallo.set(e);
                return false;
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
    }
}
