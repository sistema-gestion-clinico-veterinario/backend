package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Consulta;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.HistoriaClinica;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.ServiciosVeterinarios;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.EstadoConsulta;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoConsulta;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.dto.request.CerrarConsultaRequest;
import veterinaria.vargasvet.dto.request.ConsultaRequest;
import veterinaria.vargasvet.mapper.ConsultaMapper;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.ConsultaRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.HistoriaClinicaRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.ServiciosVeterinariosRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.service.AuditLogService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DataJpaTest
class ConsultaServiceIntegrationTest {

    @Autowired private ConsultaRepository consultaRepository;
    @Autowired private HistoriaClinicaRepository historiaClinicaRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private ServiciosVeterinariosRepository serviciosVeterinariosRepository;

    private AuditLogService auditLogService;
    private ConsultaServiceImpl consultaService;

    @BeforeEach
    void setUp() {
        auditLogService = mock(AuditLogService.class);
        consultaService = new ConsultaServiceImpl(
                consultaRepository,
                historiaClinicaRepository,
                mascotaRepository,
                citaRepository,
                new ConsultaMapper(),
                auditLogService
        );
        autenticarSuperAdmin();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void actualizarConsultaPersisteDatosClinicosYSincronizaHistoriaYMascota() {
        Consulta consulta = crearConsultaAbiertaCompleta();
        ConsultaRequest request = new ConsultaRequest();
        request.setVersion(consulta.getVersion());
        request.setPesoEnConsulta(12.8);
        request.setTemperatura(38.6);
        request.setAntecedentesEnfermedades("Dermatitis previa");
        request.setAntecedentesPersonales("Alergia alimentaria");
        request.setGrupoSanguineo("DEA 1.1+");

        consultaService.updateConsulta(consulta.getId(), request);

        Consulta actualizada = consultaRepository.findById(consulta.getId()).orElseThrow();
        HistoriaClinica hc = historiaClinicaRepository.findById(actualizada.getHistoriaClinica().getId()).orElseThrow();
        Mascota mascota = mascotaRepository.findById(hc.getMascota().getId()).orElseThrow();

        assertThat(actualizada.getPesoEnConsulta()).isEqualTo(12.8);
        assertThat(actualizada.getTemperatura()).isEqualTo(38.6);
        assertThat(mascota.getPeso()).isEqualTo(12.8);
        assertThat(hc.getEnfermedades()).isEqualTo("Dermatitis previa");
        assertThat(hc.getAntecedentesPersonales()).isEqualTo("Alergia alimentaria");
        assertThat(hc.getGrupoSanguineo()).isEqualTo("DEA 1.1+");
    }

    @Test
    void actualizarConsultaRechazaVersionDesactualizadaSinPersistirCambios() {
        Consulta consulta = crearConsultaAbiertaCompleta();
        ConsultaRequest request = new ConsultaRequest();
        request.setVersion(consulta.getVersion() + 1);
        request.setPesoEnConsulta(18.0);

        assertThatThrownBy(() -> consultaService.updateConsulta(consulta.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consulta ha sido modificada");

        Consulta persisted = consultaRepository.findById(consulta.getId()).orElseThrow();
        assertThat(persisted.getPesoEnConsulta()).isEqualTo(10.0);
        verify(auditLogService, never()).log(any(), any(), any());
    }

    @Test
    void cerrarConsultaCompletaCitaAsociada() {
        Consulta consulta = crearConsultaAbiertaCompleta();
        CerrarConsultaRequest request = new CerrarConsultaRequest();
        request.setVersion(consulta.getVersion());

        consultaService.cerrarConsulta(consulta.getId(), request);

        Consulta cerrada = consultaRepository.findById(consulta.getId()).orElseThrow();
        Cita cita = citaRepository.findById(cerrada.getCita().getId()).orElseThrow();

        assertThat(cerrada.getEstado()).isEqualTo(EstadoConsulta.CERRADA);
        assertThat(cerrada.getFechaCierre()).isNotNull();
        assertThat(cerrada.getCerradoPor()).isEqualTo("doctor@vargasvet.test");
        assertThat(cita.getEstado()).isEqualTo(EstadoCita.COMPLETADA);
    }

    @Test
    void cerrarConsultaRechazaFaltaDeAnamnesisSinCambiarEstado() {
        Consulta consulta = crearConsultaAbiertaCompleta();
        consulta.setAnamnesis(" ");
        consultaRepository.saveAndFlush(consulta);
        CerrarConsultaRequest request = new CerrarConsultaRequest();
        request.setVersion(consulta.getVersion());

        assertThatThrownBy(() -> consultaService.cerrarConsulta(consulta.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anamnesis es obligatoria");

        Consulta persisted = consultaRepository.findById(consulta.getId()).orElseThrow();
        assertThat(persisted.getEstado()).isEqualTo(EstadoConsulta.ABIERTA);
        assertThat(citaRepository.findById(persisted.getCita().getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoCita.EN_PROCESO);
    }

    @Test
    void historiaClinicaMantieneConsultaRelacionadaPorCitaYMascota() {
        Consulta consulta = crearConsultaAbiertaCompleta();

        HistoriaClinica hc = historiaClinicaRepository.findByMascotaId(
                consulta.getHistoriaClinica().getMascota().getId()
        ).orElseThrow();

        assertThat(hc.getNumeroHc()).startsWith("HC-");
        assertThat(consultaRepository.findByCitaId(consulta.getCita().getId())).isPresent();
        assertThat(consultaRepository.findByCitaId(consulta.getCita().getId()).orElseThrow().getHistoriaClinica().getId())
                .isEqualTo(hc.getId());
    }

    private Consulta crearConsultaAbiertaCompleta() {
        Cita cita = crearCita(EstadoCita.EN_PROCESO);
        HistoriaClinica hc = new HistoriaClinica();
        hc.setMascota(cita.getMascota());
        hc.setNumeroHc("HC-" + UUID.randomUUID().toString().substring(0, 8));
        hc.setActiva(true);
        hc = historiaClinicaRepository.save(hc);

        Consulta consulta = new Consulta();
        consulta.setHistoriaClinica(hc);
        consulta.setCita(cita);
        consulta.setVeterinario(cita.getEmpleado());
        consulta.setFechaConsulta(LocalDateTime.now());
        consulta.setMotivoConsulta("Control general");
        consulta.setTipoConsulta(TipoConsulta.CONTROL_RUTINA);
        consulta.setEstado(EstadoConsulta.ABIERTA);
        consulta.setPesoEnConsulta(10.0);
        consulta.setAnamnesis("Paciente estable");
        return consultaRepository.saveAndFlush(consulta);
    }

    private Cita crearCita(EstadoCita estado) {
        Company company = new Company();
        company.setName("VargasVet Clinica");
        company.setRuc(uniqueDigits(11));
        company.setActivo(true);
        company = companyRepository.save(company);

        Usuario apoderadoUser = usuario("cliente", company);
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(apoderadoUser);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setNumeroDocumento(uniqueDigits(8));
        apoderado.setGenero(Genero.FEMENINO);
        apoderado = apoderadoRepository.save(apoderado);

        Mascota mascota = new Mascota();
        mascota.setNombreCompleto("Firulais");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setPeso(10.0);
        mascota.setApoderado(apoderado);
        mascota.setUuid(UUID.randomUUID().toString());
        mascota = mascotaRepository.save(mascota);

        Usuario empleadoUser = usuario("vet", company);
        Empleado empleado = new Empleado();
        empleado.setUser(empleadoUser);
        empleado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        empleado.setNumeroDocumentoIdentidad(uniqueDigits(8));
        empleado.setGenero(Genero.MASCULINO);
        empleado.setEstado(true);
        empleado = empleadoRepository.save(empleado);

        ServiciosVeterinarios servicio = new ServiciosVeterinarios();
        servicio.setCompany(company);
        servicio.setNombre("Consulta general");
        servicio.setDescripcion("Consulta veterinaria general");
        servicio.setPrecio(new BigDecimal("100.00"));
        servicio.setDisponible(true);
        servicio.setActivo(true);
        servicio.setDuracionEstimada(30);
        servicio.setPermiteEmergencia(false);
        servicio = serviciosVeterinariosRepository.save(servicio);

        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setEmpleado(empleado);
        cita.setServicio(servicio);
        cita.setMotivoCita("Control general");
        cita.setFechaHoraInicio(LocalDateTime.now().minusMinutes(20));
        cita.setFechaHoraFin(LocalDateTime.now().plusMinutes(10));
        cita.setDuracionMinutos(30);
        cita.setEstado(estado);
        cita.setTotalServicio(new BigDecimal("100.00"));
        cita.setMontoPagado(BigDecimal.ZERO);
        cita.setEliminada(false);
        cita.setEsEmergencia(false);
        return citaRepository.save(cita);
    }

    private Usuario usuario(String prefix, Company company) {
        Usuario usuario = new Usuario();
        usuario.setEmail(prefix + "-" + UUID.randomUUID() + "@vargasvet.test");
        usuario.setPassword("password");
        usuario.setNombre(prefix);
        usuario.setApellido("Test");
        usuario.setDni(uniqueDigits(8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(company);
        return usuarioRepository.save(usuario);
    }

    private void autenticarSuperAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(
                        "doctor@vargasvet.test",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                )
        );
    }

    private String uniqueDigits(int length) {
        String digits = String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        while (digits.length() < length) {
            digits += "0";
        }
        return digits.substring(0, length);
    }
}
