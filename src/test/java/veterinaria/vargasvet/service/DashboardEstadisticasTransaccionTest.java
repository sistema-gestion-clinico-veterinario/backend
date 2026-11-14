package veterinaria.vargasvet.service;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.dto.response.DashboardStatsDTO;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.impl.DashboardServiceImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(DashboardServiceImpl.class)
class DashboardEstadisticasTransaccionTest {

    @Autowired private DashboardService dashboardService;
    @Autowired private EntityManagerFactory entityManagerFactory;

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
    void autenticarAdministradorDeClinica() {
        var autoridades = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        var principal = new UsuarioPrincipal(1, "admin@clinica.test", "", autoridades, 5,
                1, RoleScope.STAFF, RolePurpose.COMPANY_ADMIN, 0L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, autoridades));
    }

    @AfterEach
    void limpiarAutenticacion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void lasEstadisticasLeenTodoEnUnaSolaTransaccionDeSoloLecturaConUnaSolaConexion() {
        Statistics estadisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        dashboardService.getStats(null);
        estadisticas.clear();

        dashboardService.getStats(null);

        assertThat(estadisticas.getPrepareStatementCount()).isGreaterThan(1);
        assertThat(estadisticas.getConnectCount()).isEqualTo(1);
    }

    @Test
    void sinCitasNiPacientesDevuelveCerosYLasSeriesCompletas() {
        DashboardStatsDTO estadisticas = dashboardService.getStats(null);

        assertThat(estadisticas.getTotalPacientes()).isZero();
        assertThat(estadisticas.getTotalCitas()).isZero();
        assertThat(estadisticas.getTotalEmpleados()).isZero();
        assertThat(estadisticas.getCitasPorDia()).hasSize(7).containsOnly(0L);
        assertThat(estadisticas.getCitasPorSemana()).hasSize(4).containsOnly(0L);
        assertThat(estadisticas.getCitasPorMes()).hasSize(12).containsOnly(0L);
    }
}
