package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Consulta;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.HistoriaClinica;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Prescripcion;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EstadoConsulta;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.dto.request.PrescripcionRequest;
import veterinaria.vargasvet.mapper.ConsultaMapper;
import veterinaria.vargasvet.repository.ConsultaRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.PrescripcionRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AuditLogService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescripcionServiceImplVeterinarioTest {

    private static final int EMPRESA_DE_LA_CONSULTA = 7;
    private static final int USUARIO = 42;

    @Mock PrescripcionRepository prescripcionRepository;
    @Mock ConsultaRepository consultaRepository;
    @Mock EmpleadoRepository empleadoRepository;
    @Mock ConsultaMapper consultaMapper;
    @Mock AuditLogService auditLogService;

    private PrescripcionServiceImpl service;
    private Consulta consulta;

    @BeforeEach
    void setUp() {
        service = new PrescripcionServiceImpl(prescripcionRepository, consultaRepository, empleadoRepository,
                consultaMapper, auditLogService);

        Company empresa = new Company();
        empresa.setId(EMPRESA_DE_LA_CONSULTA);
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(new Usuario());
        apoderado.setCompany(empresa);
        Mascota mascota = new Mascota();
        mascota.setNombreCompleto("Firulais");
        mascota.setApoderado(apoderado);
        HistoriaClinica historia = new HistoriaClinica();
        historia.setMascota(mascota);
        consulta = new Consulta();
        consulta.setId(10L);
        consulta.setEstado(EstadoConsulta.ABIERTA);
        consulta.setHistoriaClinica(historia);

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        var principal = new UsuarioPrincipal(USUARIO, "ana@example.test", "", authorities, EMPRESA_DE_LA_CONSULTA, 2,
                RoleScope.STAFF, RolePurpose.COMPANY_ADMIN, 0L);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(principal, null, authorities));

        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(prescripcionRepository.save(any(Prescripcion.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private PrescripcionRequest receta() {
        PrescripcionRequest request = new PrescripcionRequest();
        request.setMedicamento("Amoxicilina");
        request.setDosis("1 tableta");
        request.setFrecuencia("Cada 8 horas");
        request.setDuracionDias(5);
        request.setViaAdministracion("Oral");
        request.setInstrucciones("Con alimento");
        request.setFechaInicio(LocalDate.now());
        return request;
    }

    @Test
    void laRecetaSeAsociaAlProfesionalDeLaEmpresaDeLaConsulta() {
        Empleado propio = new Empleado();
        propio.setId(5L);
        when(empleadoRepository.findByUserIdAndCompanyIdAndEstadoTrue(USUARIO, EMPRESA_DE_LA_CONSULTA))
                .thenReturn(Optional.of(propio));

        service.crear(10L, receta());

        ArgumentCaptor<Prescripcion> guardada = ArgumentCaptor.forClass(Prescripcion.class);
        verify(prescripcionRepository).save(guardada.capture());
        assertThat(guardada.getValue().getVeterinario()).isSameAs(propio);
    }

    @Test
    void unProfesionalDeOtraEmpresaNuncaQuedaAsociadoALaReceta() {
        when(empleadoRepository.findByUserIdAndCompanyIdAndEstadoTrue(USUARIO, EMPRESA_DE_LA_CONSULTA))
                .thenReturn(Optional.empty());

        service.crear(10L, receta());

        ArgumentCaptor<Prescripcion> guardada = ArgumentCaptor.forClass(Prescripcion.class);
        verify(prescripcionRepository).save(guardada.capture());
        assertThat(guardada.getValue().getVeterinario()).isNull();
        verify(empleadoRepository, never()).findActiveByUserId(USUARIO);
    }
}
