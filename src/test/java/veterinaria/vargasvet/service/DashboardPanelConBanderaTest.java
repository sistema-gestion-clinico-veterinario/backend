package veterinaria.vargasvet.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.dto.response.DashboardOverviewDTO;
import veterinaria.vargasvet.dto.response.MascotaResponse;
import veterinaria.vargasvet.dto.response.PagoListResponse;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.impl.DashboardServiceImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import(DashboardServiceImpl.class)
class DashboardPanelConBanderaTest {

    @Autowired private DashboardService dashboardService;

    @MockitoBean private AuditLogService auditLogService;
    @MockitoBean private EmpleadoService empleadoService;
    @MockitoBean private CitaService citaService;
    @MockitoBean private MascotaService mascotaService;
    @MockitoBean private RoleService roleService;
    @MockitoBean private ApoderadoService apoderadoService;
    @MockitoBean private PagoService pagoService;
    @MockitoBean private CompanyService companyService;
    @MockitoBean private AccesoValidator accesoValidator;

    @BeforeEach
    void autenticarAdministradorConPermisoSoloSobreMascotasYPagos() {
        var autoridades = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        var principal = new UsuarioPrincipal(1, "admin@clinica.test", "", autoridades, 5,
                1, RoleScope.STAFF, RolePurpose.COMPANY_ADMIN, 0L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, autoridades));
        when(accesoValidator.can(anyString(), anyString())).thenReturn(false);
        when(accesoValidator.can("VISTA_MASCOTAS", "LEER")).thenReturn(true);
        when(accesoValidator.can("VISTA_PAGOS", "LEER")).thenReturn(true);
        when(mascotaService.listar(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Page.<MascotaResponse>empty());
        when(pagoService.listarHistorialPorEmpresa(anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.<PagoListResponse>empty());
    }

    @AfterEach
    void limpiarAutenticacion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void conLaBanderaActivaElPanelNoUsaLasListasPaginadasConConteo() {
        activar(true);

        DashboardOverviewDTO panel = dashboardService.getOverview(null);

        verify(mascotaService, never()).listar(any(), any(), any(), any(), any(), any(), anyInt(), anyInt());
        verify(pagoService, never()).listarHistorialPorEmpresa(anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any());
        verify(pagoService).ultimosDelHistorial(eq(5), any());
        assertThat(panel.getPets()).isEmpty();
        assertThat(panel.getPayments()).isEmpty();
        assertThat(panel.getStats().getTotalCitas()).isZero();
    }

    @Test
    void conLaBanderaApagadaElPanelConservaElCodigoAnterior() {
        activar(false);

        DashboardOverviewDTO panel = dashboardService.getOverview(null);

        verify(mascotaService).listar(any(), any(), any(), any(), any(), any(), eq(0), eq(6));
        verify(pagoService).listarHistorialPorEmpresa(eq(0), eq(5), any(), any(), any(), any(), any(), any(), any());
        verify(pagoService, never()).ultimosDelHistorial(anyInt(), any());
        assertThat(panel.getPets()).isEmpty();
        assertThat(panel.getPayments()).isEmpty();
        assertThat(panel.getStats().getTotalCitas()).isZero();
    }

    private void activar(boolean valor) {
        Object destino = AopTestUtils.getUltimateTargetObject(dashboardService);
        ReflectionTestUtils.setField(destino, "consultasOptimizadas", valor);
    }
}
