package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.TipoInactividad;
import veterinaria.vargasvet.dto.response.ApoderadoEstadoResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CompanyMembershipService;
import veterinaria.vargasvet.service.PetOwnershipService;
import veterinaria.vargasvet.service.SessionSecurityService;
import veterinaria.vargasvet.util.AppClock;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Suspender (temporal) y dar de baja (fin de la relación) un cliente son acciones distintas; ambas lo
 * dejan sin acceso en SU empresa y ajustan sus mascotas, y reactivar revierte cualquiera de las dos.
 */
@ExtendWith(MockitoExtension.class)
class ApoderadoServiceImplEstadoTest {

    private static final int COMPANY_ID = 7;

    @Mock ApoderadoRepository apoderadoRepository;
    @Mock veterinaria.vargasvet.service.AccountClosureGuard accountClosureGuard;
    @Mock CitaRepository citaRepository;
    @Mock SessionSecurityService sessionSecurityService;
    @Mock CompanyMembershipService companyMembershipService;
    @Mock PetOwnershipService petOwnershipService;
    @Mock AuditLogService auditLogService;
    @Mock veterinaria.vargasvet.service.AdministratorProtection administratorProtection;
    @Mock jakarta.persistence.EntityManager entityManager;
    @Mock veterinaria.vargasvet.service.AccessRestoredNotifier accessRestoredNotifier;
    @InjectMocks ApoderadoServiceImpl service;

    private Apoderado cliente;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(COMPANY_ID);

        Usuario usuario = new Usuario();
        usuario.setId(20);
        usuario.setNombre("Ana");
        usuario.setApellido("Torres");
        usuario.setEmail("ana@example.test");

        cliente = new Apoderado();
        cliente.setId(40L);
        cliente.setUser(usuario);
        cliente.setCompany(company);
        cliente.setEstado(true);

        UsuarioPrincipal principal = new UsuarioPrincipal(1, "admin@empresa.test", "",
                List.of(), COMPANY_ID, 2, RoleScope.STAFF, RolePurpose.COMPANY_ADMIN, 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        org.springframework.test.util.ReflectionTestUtils.setField(service, "entityManager", entityManager);
        org.mockito.Mockito.lenient().when(apoderadoRepository.findByIdAndCompanyId(40L, COMPANY_ID))
                .thenReturn(Optional.of(cliente));
        org.mockito.Mockito.lenient().when(petOwnershipService.syncPets(any(), any()))
                .thenReturn(new ApoderadoEstadoResponse());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void suspenderBloqueaElAccesoSinRegistrarFechaDeSalidaNiTocarOtraEmpresa() {
        service.cambiarEstado(40L, false, TipoInactividad.SUSPENSION, "  Mora en pagos ");

        assertThat(cliente.getEstado()).isFalse();
        assertThat(cliente.getTipoInactividad()).isEqualTo(TipoInactividad.SUSPENSION);
        assertThat(cliente.getFechaSalida()).isNull();
        verify(sessionSecurityService).invalidateSessionsForCompany(cliente.getUser(), company);
        verify(petOwnershipService).syncPets(cliente, TipoInactividad.SUSPENSION);
        verify(auditLogService).log(eq(COMPANY_ID), eq("SUSPENDER_APODERADO"), eq("Clientes"),
                contains("Motivo: Mora en pagos"));
    }

    @Test
    void darDeBajaRegistraLaFechaDeSalida() {
        service.cambiarEstado(40L, false, TipoInactividad.BAJA, null);

        assertThat(cliente.getEstado()).isFalse();
        assertThat(cliente.getTipoInactividad()).isEqualTo(TipoInactividad.BAJA);
        assertThat(cliente.getFechaSalida()).isEqualTo(AppClock.today());
        verify(petOwnershipService).syncPets(cliente, TipoInactividad.BAJA);
        verify(auditLogService).log(eq(COMPANY_ID), eq("DAR_DE_BAJA_APODERADO"), eq("Clientes"), any());
    }

    @Test
    void sinTipoSeAsumeBaja() {
        service.cambiarEstado(40L, false, null, null);

        assertThat(cliente.getTipoInactividad()).isEqualTo(TipoInactividad.BAJA);
    }

    @Test
    void elAdministradorNoReactivaUnaCuentaQueLaPersonaCerro() {
        cliente.setEstado(false);
        cliente.setTipoInactividad(TipoInactividad.BAJA);
        org.mockito.Mockito.doThrow(new IllegalArgumentException("La persona cerró su propia cuenta"))
                .when(accountClosureGuard).assertNotSelfClosed(any(), any());

        assertThatThrownBy(() -> service.cambiarEstado(40L, true, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cerró su propia cuenta");

        assertThat(cliente.getEstado()).isFalse();
        org.mockito.Mockito.verifyNoInteractions(petOwnershipService);
    }

    @Test
    void reactivarLimpiaElTipoYLaFechaDeSalidaYRestauraLasMascotas() {
        cliente.setEstado(false);
        cliente.setTipoInactividad(TipoInactividad.BAJA);
        cliente.setFechaSalida(AppClock.today().minusDays(10));
        ApoderadoEstadoResponse restauradas = new ApoderadoEstadoResponse();
        restauradas.getMascotasRestauradas().add("Luna");
        when(petOwnershipService.syncPets(cliente, null)).thenReturn(restauradas);

        ApoderadoEstadoResponse resultado = service.cambiarEstado(40L, true, null, null);

        assertThat(cliente.getEstado()).isTrue();
        assertThat(cliente.getTipoInactividad()).isNull();
        assertThat(cliente.getFechaSalida()).isNull();
        assertThat(resultado.getMascotasRestauradas()).containsExactly("Luna");
        verify(auditLogService).log(eq(COMPANY_ID), eq("REACTIVAR_APODERADO"), eq("Clientes"), any());
    }

    @Test
    void conCitasVigentesNoSePuedeSuspenderNiDarDeBaja_YNadaCambia() {
        when(citaRepository.existsCitaVigenteByApoderadoId(eq(40L), any())).thenReturn(true);

        assertThatThrownBy(() -> service.cambiarEstado(40L, false, TipoInactividad.SUSPENSION, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("citas programadas vigentes");

        assertThat(cliente.getEstado()).isTrue();
        verifyNoInteractions(sessionSecurityService, petOwnershipService, auditLogService);
    }

    @Test
    void unaBajaSoloSePuedeReactivar() {
        cliente.setEstado(false);
        cliente.setTipoInactividad(TipoInactividad.BAJA);

        assertThatThrownBy(() -> service.cambiarEstado(40L, false, TipoInactividad.SUSPENSION, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dado de baja");
        assertThatThrownBy(() -> service.cambiarEstado(40L, false, TipoInactividad.BAJA, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(cliente.getTipoInactividad()).isEqualTo(TipoInactividad.BAJA);
        verify(petOwnershipService, never()).syncPets(any(), any());
    }

    @Test
    void unaSuspensionNoSePuedeRepetirPeroSiPuedePasarABaja() {
        cliente.setEstado(false);
        cliente.setTipoInactividad(TipoInactividad.SUSPENSION);

        assertThatThrownBy(() -> service.cambiarEstado(40L, false, TipoInactividad.SUSPENSION, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está suspendido");

        service.cambiarEstado(40L, false, TipoInactividad.BAJA, null);

        assertThat(cliente.getTipoInactividad()).isEqualTo(TipoInactividad.BAJA);
        assertThat(cliente.getFechaSalida()).isEqualTo(AppClock.today());
        verify(citaRepository, never()).existsCitaVigenteByApoderadoId(any(), any());
    }

    @Test
    void unClienteDeOtraEmpresaNoSeEncuentraNiSeModifica() {
        UsuarioPrincipal otraEmpresa = new UsuarioPrincipal(9, "admin@otra.test", "", List.of(), 99,
                2, RoleScope.STAFF, RolePurpose.COMPANY_ADMIN, 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(otraEmpresa, null, otraEmpresa.getAuthorities()));
        when(apoderadoRepository.findByIdAndCompanyId(40L, 99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarEstado(40L, false, TipoInactividad.BAJA, null))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(cliente.getEstado()).isTrue();
        verifyNoInteractions(sessionSecurityService, petOwnershipService, auditLogService);
    }

    @Test
    void siElClienteTambienEsAdministradorSoloOtroAdministradorPuedeCambiarleElEstado() {
        org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException(
                "Solo un administrador puede gestionar la cuenta de otro administrador"))
                .when(administratorProtection).assertCanManage(cliente.getUser(), COMPANY_ID);

        assertThatThrownBy(() -> service.cambiarEstado(40L, false, TipoInactividad.SUSPENSION, "x"))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(cliente.getEstado()).isTrue();

        cliente.setEstado(false);
        cliente.setTipoInactividad(TipoInactividad.SUSPENSION);
        assertThatThrownBy(() -> service.cambiarEstado(40L, true, null, null))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(cliente.getEstado()).isFalse();
        verify(sessionSecurityService, never()).invalidateSessionsForCompany(any(), any());
        verifyNoInteractions(petOwnershipService);
    }

    @Test
    void repetirLaMismaBajaALosPocosMinutosNoRepiteSusEfectos() {
        service.cambiarEstado(40L, false, TipoInactividad.BAJA, "Primera");
        ApoderadoEstadoResponse segunda = service.cambiarEstado(40L, false, TipoInactividad.BAJA, "Segunda");

        assertThat(segunda.getMascotasPausadas()).isEmpty();
        verify(auditLogService, org.mockito.Mockito.times(1)).log(any(), eq("DAR_DE_BAJA_APODERADO"), eq("Clientes"), any());
        verify(sessionSecurityService, org.mockito.Mockito.times(1)).invalidateSessionsForCompany(any(), any());
        verify(petOwnershipService, org.mockito.Mockito.times(1)).syncPets(any(), any());
    }

    @Test
    void reactivarAUnClienteActivoNoHaceNada() {
        service.cambiarEstado(40L, true, null, null);

        verifyNoInteractions(auditLogService, sessionSecurityService, petOwnershipService);
    }

    @Test
    void eliminarUnClienteEsUnaBajaQueConservaSusRegistros() {
        service.eliminar(40L);

        assertThat(cliente.getEstado()).isFalse();
        assertThat(cliente.getTipoInactividad()).isEqualTo(TipoInactividad.BAJA);
        assertThat(cliente.getFechaSalida()).isNotNull();
        verify(auditLogService).log(any(), eq("DAR_DE_BAJA_APODERADO"), eq("Clientes"), contains("Eliminado por el administrador"));
        verify(apoderadoRepository, never()).delete(any());
    }

    @Test
    void soloUnAdministradorPuedeEliminarUnCliente() {
        UsuarioPrincipal recepcion = new UsuarioPrincipal(5, "recepcion@empresa.test", "",
                List.of(), COMPANY_ID, 9, RoleScope.STAFF, RolePurpose.CUSTOM, 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(recepcion, null, recepcion.getAuthorities()));

        assertThatThrownBy(() -> service.eliminar(40L))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Solo un administrador");

        assertThat(cliente.getEstado()).isTrue();
        verifyNoInteractions(auditLogService);
    }

    @Test
    void alReactivarAUnClienteSeLeAvisaUnaSolaVezYNoAlDarloDeBaja() {
        service.cambiarEstado(40L, false, TipoInactividad.BAJA, "Se mudó");
        verifyNoInteractions(accessRestoredNotifier);

        service.cambiarEstado(40L, true, null, null);
        service.cambiarEstado(40L, true, null, null);

        verify(accessRestoredNotifier, org.mockito.Mockito.times(1)).send(cliente.getUser(), company);
    }
}
