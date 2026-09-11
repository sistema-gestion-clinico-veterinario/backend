package veterinaria.vargasvet.ers.historias;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Consulta;
import veterinaria.vargasvet.domain.entity.HistoriaClinica;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Prescripcion;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EstadoConsulta;
import veterinaria.vargasvet.dto.request.PrescripcionRequest;
import veterinaria.vargasvet.dto.response.PrescripcionResumenResponse;
import veterinaria.vargasvet.mapper.ConsultaMapper;
import veterinaria.vargasvet.repository.ConsultaRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.PrescripcionRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.impl.PrescripcionServiceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RF31Test {

    private PrescripcionRepository prescripcionRepository;
    private ConsultaRepository consultaRepository;
    private EmpleadoRepository empleadoRepository;
    private ConsultaMapper consultaMapper;
    private Validator validator;

    @BeforeEach
    void setUp() {
        prescripcionRepository = mock(PrescripcionRepository.class);
        consultaRepository = mock(ConsultaRepository.class);
        empleadoRepository = mock(EmpleadoRepository.class);
        consultaMapper = mock(ConsultaMapper.class);
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("[CP-RF31-01] Crea una receta válida vinculada a la consulta")
    void cpRf3101_creaRecetaVinculada() {
        autenticarEmpresa(7);
        Consulta consulta = consultaDeEmpresa(7);
        PrescripcionRequest request = recetaCompleta();
        PrescripcionResumenResponse mapped = new PrescripcionResumenResponse();
        when(consultaRepository.findById(100L)).thenReturn(Optional.of(consulta));
        when(prescripcionRepository.save(any(Prescripcion.class))).thenAnswer(invocation -> {
            Prescripcion value = invocation.getArgument(0);
            value.setId(200L);
            return value;
        });
        when(consultaMapper.toPrescripcionResponse(any(Prescripcion.class))).thenReturn(mapped);

        PrescripcionResumenResponse response = service().crear(100L, request);

        assertThat(response).isSameAs(mapped);
        ArgumentCaptor<Prescripcion> captor = ArgumentCaptor.forClass(Prescripcion.class);
        verify(prescripcionRepository).save(captor.capture());
        assertThat(captor.getValue().getConsulta()).isSameAs(consulta);
        assertThat(captor.getValue().getMedicamento()).isEqualTo("Amoxicilina");
        assertThat(captor.getValue().getDuracionDias()).isEqualTo(7);
        assertThat(captor.getValue().getInstrucciones()).isEqualTo("Administrar después de los alimentos");
    }

    @Test
    @DisplayName("[CP-RF31-02] Rechaza una receta sin duración")
    void cpRf3102_rechazaRecetaSinDuracion() {
        PrescripcionRequest request = recetaCompleta();
        request.setDuracionDias(null);

        assertThat(validator.validateProperty(request, "duracionDias"))
                .as("CA-RF31-02 exige duración")
                .isNotEmpty();
    }

    @Test
    @DisplayName("[CP-RF31-02] Rechaza una receta sin indicaciones")
    void cpRf3102_rechazaRecetaSinIndicaciones() {
        PrescripcionRequest request = recetaCompleta();
        request.setInstrucciones(null);

        assertThat(validator.validateProperty(request, "instrucciones"))
                .as("CA-RF31-02 exige indicaciones")
                .isNotEmpty();
    }

    @Test
    @DisplayName("[CP-RNF02-02][RF-31] Rechaza crear una receta en una consulta de otra empresa")
    void cpRnf0202_rechazaRecetaEnConsultaDeOtraEmpresa() {
        autenticarEmpresa(7);
        Consulta consultaAjena = consultaDeEmpresa(9);
        when(consultaRepository.findById(100L)).thenReturn(Optional.of(consultaAjena));
        when(prescripcionRepository.save(any(Prescripcion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> service().crear(100L, recetaCompleta()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("otra empresa");
    }

    private PrescripcionServiceImpl service() {
        return new PrescripcionServiceImpl(
                prescripcionRepository,
                consultaRepository,
                empleadoRepository,
                consultaMapper,
                mock(veterinaria.vargasvet.service.AuditLogService.class)
        );
    }

    private PrescripcionRequest recetaCompleta() {
        PrescripcionRequest request = new PrescripcionRequest();
        request.setMedicamento("Amoxicilina");
        request.setDosis("Una tableta");
        request.setFrecuencia("Cada doce horas");
        request.setDuracionDias(7);
        request.setViaAdministracion("Oral");
        request.setInstrucciones("Administrar después de los alimentos");
        request.setFechaInicio(LocalDate.now());
        request.setFechaFin(LocalDate.now().plusDays(6));
        return request;
    }

    private Consulta consultaDeEmpresa(Integer companyId) {
        Company company = new Company();
        company.setId(companyId);
        Usuario propietario = new Usuario();
        propietario.setCompany(company);
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(propietario);
        Mascota mascota = new Mascota();
        mascota.setApoderado(apoderado);
        HistoriaClinica historia = new HistoriaClinica();
        historia.setMascota(mascota);
        Consulta consulta = new Consulta();
        consulta.setId(100L);
        consulta.setHistoriaClinica(historia);
        consulta.setEstado(EstadoConsulta.ABIERTA);
        return consulta;
    }

    private void autenticarEmpresa(Integer companyId) {
        UsuarioPrincipal principal = new UsuarioPrincipal(
                15,
                "qa@empresa.test",
                "",
                List.of(),
                companyId
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
