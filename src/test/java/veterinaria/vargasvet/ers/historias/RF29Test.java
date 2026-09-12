package veterinaria.vargasvet.ers.historias;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Consulta;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.HistoriaClinica;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoConsulta;
import veterinaria.vargasvet.domain.enums.TipoConsulta;
import veterinaria.vargasvet.dto.response.AplicacionPreventivaResponse;
import veterinaria.vargasvet.dto.response.ControlPreventivoResponse;
import veterinaria.vargasvet.dto.response.HistoriaClinicaDetalleResponse;
import veterinaria.vargasvet.repository.ConsultaRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.HistoriaClinicaRepository;
import veterinaria.vargasvet.repository.PrescripcionRepository;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.ControlPreventivoService;
import veterinaria.vargasvet.service.impl.ArchivoClinicoServiceImpl;
import veterinaria.vargasvet.service.impl.HistoriaClinicaServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RF29Test {

    private final HistoriaClinicaRepository historiaRepository = mock(HistoriaClinicaRepository.class);
    private final ConsultaRepository consultaRepository = mock(ConsultaRepository.class);
    private final ArchivoClinicoServiceImpl archivoService = mock(ArchivoClinicoServiceImpl.class);
    private final ControlPreventivoService preventivoService = mock(ControlPreventivoService.class);
    private final AccesoValidator accesoValidator = mock(AccesoValidator.class);
    private final EmpleadoRepository empleadoRepository = mock(EmpleadoRepository.class);
    private final PrescripcionRepository prescripcionRepository = mock(PrescripcionRepository.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("[CP-RF29-01] Alcance COMPANY autorizado: presenta consultas en orden cronológico descendente y preventivos separados")
    void cpRf2901_ordenaConsultasYExponePreventivosSeparados() {
        autenticarEmpresa(7);
        when(accesoValidator.canAccessCompanyData("VISTA_HISTORIAS")).thenReturn(true);
        HistoriaClinica historia = historia(7);
        Consulta antigua = consulta(10L, historia, LocalDateTime.of(2026, 5, 1, 10, 0));
        Consulta reciente = consulta(20L, historia, LocalDateTime.of(2026, 8, 20, 16, 30));
        historia.setConsultas(List.of(antigua, reciente));
        ControlPreventivoResponse control = mock(ControlPreventivoResponse.class);
        AplicacionPreventivaResponse aplicacion = mock(AplicacionPreventivaResponse.class);
        when(historiaRepository.findById(100L)).thenReturn(Optional.of(historia));
        when(preventivoService.listarControles(50L)).thenReturn(List.of(control));
        when(preventivoService.listarAplicaciones(50L)).thenReturn(List.of(aplicacion));

        HistoriaClinicaDetalleResponse response = service().getDetalle(100L);

        assertThat(response.getConsultas()).extracting("id").containsExactly(20L, 10L);
        assertThat(response.getControlesPreventivos()).containsExactly(control);
        assertThat(response.getAplicacionesPreventivas()).containsExactly(aplicacion);
    }

    @Test
    @DisplayName("[CP-RF29-02] Rechaza leer una historia clínica de otra empresa")
    void cpRf2902_rechazaHistoriaDeOtraEmpresa() {
        autenticarEmpresa(7);
        when(historiaRepository.findById(100L)).thenReturn(Optional.of(historia(9)));

        assertThatThrownBy(() -> service().getDetalle(100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No tienes permiso para ver esta historia clínica");

        verify(historiaRepository).findById(100L);
    }

    @Test
    @DisplayName("[CP-RF29-02] Alcance OWN con historia asignada: acceso permitido")
    void cpRf2903_permiteAlcanceOwnConHistoriaAsignada() {
        autenticarEmpresa(7);
        when(accesoValidator.canAccessCompanyData("VISTA_HISTORIAS")).thenReturn(false);
        Empleado propio = new Empleado();
        propio.setId(77L);
        when(empleadoRepository.findByUserEmail("qa@empresa.test")).thenReturn(Optional.of(propio));

        HistoriaClinica historia = historia(7);
        Consulta consultaPropia = consulta(10L, historia, LocalDateTime.of(2026, 5, 1, 10, 0));
        consultaPropia.setVeterinario(propio);
        historia.setConsultas(List.of(consultaPropia));
        when(historiaRepository.findById(100L)).thenReturn(Optional.of(historia));

        HistoriaClinicaDetalleResponse response = service().getDetalle(100L);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("[CP-RF29-02] Alcance OWN con historia no asignada: acceso rechazado por el servidor")
    void cpRf2904_rechazaAlcanceOwnConHistoriaNoAsignada() {
        autenticarEmpresa(7);
        when(accesoValidator.canAccessCompanyData("VISTA_HISTORIAS")).thenReturn(false);
        Empleado propio = new Empleado();
        propio.setId(77L);
        Empleado otroVeterinario = new Empleado();
        otroVeterinario.setId(88L);
        when(empleadoRepository.findByUserEmail("qa@empresa.test")).thenReturn(Optional.of(propio));

        HistoriaClinica historia = historia(7);
        Consulta consultaAjena = consulta(10L, historia, LocalDateTime.of(2026, 5, 1, 10, 0));
        consultaAjena.setVeterinario(otroVeterinario);
        historia.setConsultas(List.of(consultaAjena));
        when(historiaRepository.findById(100L)).thenReturn(Optional.of(historia));

        assertThatThrownBy(() -> service().getDetalle(100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No tienes permiso para ver esta historia clínica");
    }

    @Test
    @DisplayName("[CP-RF29-02][regresión portal apoderado] Una cuenta CLIENT accede a su propia mascota sin registro de empleado")
    void cpRf2905_permiteAlcanceClienteSinRegistroDeEmpleado() {
        autenticarCliente(7);
        Empleado sinRegistro = null;
        when(empleadoRepository.findByUserEmail("cliente@empresa.test")).thenReturn(Optional.ofNullable(sinRegistro));

        HistoriaClinica historia = historia(7);
        historia.setConsultas(List.of());
        when(historiaRepository.findByMascotaId(50L)).thenReturn(Optional.of(historia));

        HistoriaClinicaDetalleResponse response = service().getPorMascota(50L);

        assertThat(response).isNotNull();
    }

    private HistoriaClinicaServiceImpl service() {
        HistoriaClinicaServiceImpl service = new HistoriaClinicaServiceImpl(
                historiaRepository,
                consultaRepository,
                archivoService
        );
        ReflectionTestUtils.setField(service, "controlPreventivoService", preventivoService);
        ReflectionTestUtils.setField(service, "accesoValidator", accesoValidator);
        ReflectionTestUtils.setField(service, "prescripcionRepository", prescripcionRepository);
        ReflectionTestUtils.setField(service, "empleadoRepository", empleadoRepository);
        return service;
    }

    private HistoriaClinica historia(Integer companyId) {
        Company company = new Company();
        company.setId(companyId);
        company.setName("Empresa " + companyId);
        Usuario propietario = new Usuario();
        propietario.setNombre("Ana");
        propietario.setApellido("Prueba");
        propietario.setCompany(company);
        Apoderado apoderado = new Apoderado();
        apoderado.setId(40L);
        apoderado.setUser(propietario);
        Mascota mascota = new Mascota();
        mascota.setId(50L);
        mascota.setNombreCompleto("Paciente QA");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(apoderado);
        HistoriaClinica historia = new HistoriaClinica();
        historia.setId(100L);
        historia.setNumeroHc("HC-000100");
        historia.setMascota(mascota);
        return historia;
    }

    private Consulta consulta(Long id, HistoriaClinica historia, LocalDateTime fecha) {
        Consulta consulta = new Consulta();
        consulta.setId(id);
        consulta.setHistoriaClinica(historia);
        consulta.setFechaConsulta(fecha);
        consulta.setMotivoConsulta("Control");
        consulta.setTipoConsulta(TipoConsulta.CONTROL_RUTINA);
        consulta.setEstado(EstadoConsulta.CERRADA);
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

    private void autenticarCliente(Integer companyId) {
        UsuarioPrincipal principal = new UsuarioPrincipal(
                16,
                "cliente@empresa.test",
                "",
                List.of(),
                companyId,
                1,
                veterinaria.vargasvet.domain.enums.RoleScope.CLIENT,
                veterinaria.vargasvet.domain.enums.RolePurpose.CLIENT_PORTAL,
                0L
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
