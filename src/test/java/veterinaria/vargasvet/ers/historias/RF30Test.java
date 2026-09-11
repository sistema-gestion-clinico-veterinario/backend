package veterinaria.vargasvet.ers.historias;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
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
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.impl.ConsultaServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Cobertura técnica inicial de RF-30.
 *
 * <p>Estas pruebas expresan el comportamiento del ERS 1.2. Una prueba fallida
 * representa una brecha verificable y no debe resolverse debilitando la aserción.</p>
 */
@DataJpaTest
class RF30Test {

    @Autowired private ConsultaRepository consultaRepository;
    @Autowired private HistoriaClinicaRepository historiaClinicaRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private ServiciosVeterinariosRepository serviciosVeterinariosRepository;

    private ConsultaServiceImpl consultaService;
    private Validator validator;

    @BeforeEach
    void setUp() {
        consultaService = new ConsultaServiceImpl(
                consultaRepository,
                historiaClinicaRepository,
                mascotaRepository,
                citaRepository,
                new ConsultaMapper(),
                mock(AuditLogService.class),
                mock(AccesoValidator.class)
        );
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        autenticarSuperAdmin();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("[CP-RF30-01] Cierra una consulta completa y completa su cita en una operación")
    void cpRf3001_cierraConsultaCompleta() {
        Consulta consulta = crearConsultaAbierta();
        CerrarConsultaRequest request = cierreCompleto(consulta);
        request.setPesoEnConsulta(14.25);
        request.setTemperatura(38.6);
        request.setFrecuenciaCardiaca(90);
        request.setFrecuenciaRespiratoria(24);

        consultaService.cerrarConsulta(consulta.getId(), request);

        Consulta persisted = consultaRepository.findById(consulta.getId()).orElseThrow();
        Cita cita = citaRepository.findById(persisted.getCita().getId()).orElseThrow();
        assertThat(persisted.getEstado()).isEqualTo(EstadoConsulta.CERRADA);
        assertThat(persisted.getFechaCierre()).isNotNull();
        assertThat(persisted.getCerradoPor()).isEqualTo("qa@vargasvet.test");
        assertThat(persisted.getPesoEnConsulta()).isEqualTo(14.25);
        assertThat(persisted.getAnamnesis()).isEqualTo("Paciente evaluado y clínicamente estable");
        assertThat(persisted.getVacunacionAplicada()).isFalse();
        assertThat(persisted.getDesparasitacionAplicada()).isFalse();
        assertThat(cita.getEstado()).isEqualTo(EstadoCita.COMPLETADA);
    }

    @Test
    @DisplayName("[CP-RF30-02] Rechaza el cierre sin peso y conserva consulta y cita abiertas")
    void cpRf3002_rechazaCierreSinPeso() {
        Consulta consulta = crearConsultaAbierta();
        consulta.setPesoEnConsulta(null);
        consulta = consultaRepository.saveAndFlush(consulta);
        CerrarConsultaRequest request = cierreCompleto(consulta);
        request.setPesoEnConsulta(null);

        Long consultaId = consulta.getId();
        Long citaId = consulta.getCita().getId();
        assertThatThrownBy(() -> consultaService.cerrarConsulta(consultaId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("peso del paciente es obligatorio");

        assertThat(consultaRepository.findById(consultaId).orElseThrow().getEstado())
                .isEqualTo(EstadoConsulta.ABIERTA);
        assertThat(citaRepository.findById(citaId).orElseThrow().getEstado())
                .isEqualTo(EstadoCita.EN_PROCESO);
    }

    @Test
    @DisplayName("[CP-RF30-02] Rechaza el cierre sin anamnesis y conserva consulta y cita abiertas")
    void cpRf3002_rechazaCierreSinAnamnesis() {
        Consulta consulta = crearConsultaAbierta();
        consulta.setAnamnesis(null);
        consulta = consultaRepository.saveAndFlush(consulta);
        CerrarConsultaRequest request = cierreCompleto(consulta);
        request.setAnamnesis(null);

        Long consultaId = consulta.getId();
        Long citaId = consulta.getCita().getId();
        assertThatThrownBy(() -> consultaService.cerrarConsulta(consultaId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anamnesis es obligatoria");

        assertThat(consultaRepository.findById(consultaId).orElseThrow().getEstado())
                .isEqualTo(EstadoConsulta.ABIERTA);
        assertThat(citaRepository.findById(citaId).orElseThrow().getEstado())
                .isEqualTo(EstadoCita.EN_PROCESO);
    }

    @Test
    @DisplayName("[CP-RF30-03] Acepta exactamente los límites clínicos definidos")
    void cpRf3003_aceptaLimitesClinicos() {
        CerrarConsultaRequest minimum = requestParaValidacion();
        minimum.setPesoEnConsulta(0.01);
        minimum.setTemperatura(0.1);
        minimum.setFrecuenciaCardiaca(1);
        minimum.setFrecuenciaRespiratoria(1);

        CerrarConsultaRequest maximum = requestParaValidacion();
        maximum.setPesoEnConsulta(120.0);
        maximum.setTemperatura(45.0);
        maximum.setFrecuenciaCardiaca(300);
        maximum.setFrecuenciaRespiratoria(200);

        assertThat(validator.validate(minimum)).isEmpty();
        assertThat(validator.validate(maximum)).isEmpty();
    }

    @Test
    @DisplayName("[CP-RF30-03] Rechaza valores clínicos fuera de rango")
    void cpRf3003_rechazaValoresFueraDeRango() {
        assertThat(violaciones("pesoEnConsulta", 0.0)).isNotEmpty();
        assertThat(violaciones("pesoEnConsulta", 120.01)).isNotEmpty();
        assertThat(violaciones("temperatura", 0.0)).isNotEmpty();
        assertThat(violaciones("temperatura", 45.1)).isNotEmpty();
        assertThat(violaciones("frecuenciaCardiaca", 0)).isNotEmpty();
        assertThat(violaciones("frecuenciaCardiaca", 301)).isNotEmpty();
        assertThat(violaciones("frecuenciaRespiratoria", 0)).isNotEmpty();
        assertThat(violaciones("frecuenciaRespiratoria", 201)).isNotEmpty();
    }

    @Test
    @DisplayName("[CP-RF30-03] Rechaza un peso con más de dos decimales")
    void cpRf3003_rechazaPesoConMasDeDosDecimales() {
        CerrarConsultaRequest request = requestParaValidacion();
        request.setPesoEnConsulta(10.123);

        assertThat(validator.validateProperty(request, "pesoEnConsulta"))
                .as("CA-RF30-03 exige como máximo dos decimales")
                .isNotEmpty();
    }

    @Test
    @DisplayName("[CP-RF30-03][RVA-043] Rechaza una temperatura con más de un decimal")
    void cpRf3003_rechazaTemperaturaConMasDeUnDecimal() {
        CerrarConsultaRequest valida = requestParaValidacion();
        valida.setTemperatura(38.4);
        assertThat(validator.validateProperty(valida, "temperatura")).isEmpty();

        CerrarConsultaRequest request = requestParaValidacion();
        request.setTemperatura(38.44);

        assertThat(validator.validateProperty(request, "temperatura"))
                .as("RVA-043 exige como máximo un decimal en temperatura")
                .isNotEmpty();
    }

    @Test
    @DisplayName("[CP-RF30-03][RVA-044][RVA-047] Rechaza frecuencias no enteras en la frontera HTTP")
    void cpRf3003_rechazaFrecuenciasNoEnterasEnFronteraHttp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        assertThatCode(() -> objectMapper.readValue("{\"frecuenciaCardiaca\":80}", ConsultaRequest.class))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> objectMapper.readValue("{\"frecuenciaCardiaca\":80.5}", ConsultaRequest.class))
                .as("RVA-044 exige rechazar un decimal en frecuencia cardíaca");

        assertThatCode(() -> objectMapper.readValue("{\"frecuenciaRespiratoria\":30}", ConsultaRequest.class))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> objectMapper.readValue("{\"frecuenciaRespiratoria\":30.5}", ConsultaRequest.class))
                .as("RVA-047 exige rechazar un decimal en frecuencia respiratoria");
    }

    @Test
    @DisplayName("[CP-RF30-04] Conserva decisiones explícitas de vacunación y desparasitación")
    void cpRf3004_persisteDecisionesPreventivas() {
        Consulta consulta = crearConsultaAbierta();
        CerrarConsultaRequest request = cierreCompleto(consulta);
        request.setVacunacionAplicada(true);
        request.setObservacionVacunacion("Vacuna aplicada durante la consulta");
        request.setDesparasitacionAplicada(false);
        request.setObservacionDesparasitacion("No corresponde en esta atención");

        consultaService.cerrarConsulta(consulta.getId(), request);

        Consulta persisted = consultaRepository.findById(consulta.getId()).orElseThrow();
        assertThat(persisted.getVacunacionAplicada()).isTrue();
        assertThat(persisted.getObservacionVacunacion()).isEqualTo("Vacuna aplicada durante la consulta");
        assertThat(persisted.getDesparasitacionAplicada()).isFalse();
        assertThat(persisted.getObservacionDesparasitacion()).isEqualTo("No corresponde en esta atención");
    }

    @Test
    @DisplayName("[CP-RF30-04] Rechaza el cierre cuando faltan ambas decisiones preventivas")
    void cpRf3004_rechazaDecisionesPreventivasAusentes() {
        Consulta consulta = crearConsultaAbierta();
        CerrarConsultaRequest request = cierreCompleto(consulta);
        request.setVacunacionAplicada(null);
        request.setDesparasitacionAplicada(null);

        Long consultaId = consulta.getId();
        assertThatThrownBy(() -> consultaService.cerrarConsulta(consultaId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("decisiones preventivas");

        assertThat(consultaRepository.findById(consultaId).orElseThrow().getEstado())
                .isEqualTo(EstadoConsulta.ABIERTA);
    }

    @Test
    @DisplayName("[CP-RF30-05] Rechaza la edición de una consulta cerrada sin cambiar datos")
    void cpRf3005_rechazaEdicionDeConsultaCerrada() {
        Consulta consulta = crearConsultaAbierta();
        consulta.setEstado(EstadoConsulta.CERRADA);
        consulta.setFechaCierre(LocalDateTime.now());
        consulta = consultaRepository.saveAndFlush(consulta);
        double pesoOriginal = consulta.getPesoEnConsulta();
        ConsultaRequest request = new ConsultaRequest();
        request.setVersion(consulta.getVersion());
        request.setPesoEnConsulta(50.0);

        Long consultaId = consulta.getId();
        assertThatThrownBy(() -> consultaService.updateConsulta(consultaId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No se puede modificar una consulta cerrada");

        assertThat(consultaRepository.findById(consultaId).orElseThrow().getPesoEnConsulta())
                .isEqualTo(pesoOriginal);
    }

    @Test
    @DisplayName("[CP-RF30-06] Rechaza la segunda escritura basada en una versión desactualizada")
    void cpRf3006_rechazaSobrescrituraConcurrente() {
        Consulta consulta = crearConsultaAbierta();
        Long versionCompartida = consulta.getVersion();

        ConsultaRequest primeraSesion = new ConsultaRequest();
        primeraSesion.setVersion(versionCompartida);
        primeraSesion.setPesoEnConsulta(11.0);
        consultaService.updateConsulta(consulta.getId(), primeraSesion);

        ConsultaRequest segundaSesion = new ConsultaRequest();
        segundaSesion.setVersion(versionCompartida);
        segundaSesion.setPesoEnConsulta(25.0);

        Long consultaId = consulta.getId();
        assertThatThrownBy(() -> consultaService.updateConsulta(consultaId, segundaSesion))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consulta cambió mientras editabas");
        assertThat(consultaRepository.findById(consultaId).orElseThrow().getPesoEnConsulta())
                .isEqualTo(11.0);
    }

    private Set<ConstraintViolation<CerrarConsultaRequest>> violaciones(String property, Object value) {
        CerrarConsultaRequest request = requestParaValidacion();
        switch (property) {
            case "pesoEnConsulta" -> request.setPesoEnConsulta((Double) value);
            case "temperatura" -> request.setTemperatura((Double) value);
            case "frecuenciaCardiaca" -> request.setFrecuenciaCardiaca((Integer) value);
            case "frecuenciaRespiratoria" -> request.setFrecuenciaRespiratoria((Integer) value);
            default -> throw new IllegalArgumentException("Propiedad clínica no soportada: " + property);
        }
        return validator.validateProperty(request, property);
    }

    private CerrarConsultaRequest requestParaValidacion() {
        CerrarConsultaRequest request = new CerrarConsultaRequest();
        request.setVersion(0L);
        request.setTipoConsulta(TipoConsulta.CONTROL_RUTINA);
        request.setPesoEnConsulta(10.0);
        request.setAnamnesis("Paciente evaluado");
        request.setVacunacionAplicada(false);
        request.setDesparasitacionAplicada(false);
        return request;
    }

    private CerrarConsultaRequest cierreCompleto(Consulta consulta) {
        CerrarConsultaRequest request = requestParaValidacion();
        request.setVersion(consulta.getVersion());
        request.setAnamnesis("Paciente evaluado y clínicamente estable");
        return request;
    }

    private Consulta crearConsultaAbierta() {
        Cita cita = crearCita();
        HistoriaClinica historia = new HistoriaClinica();
        historia.setMascota(cita.getMascota());
        historia.setNumeroHc("HC-" + UUID.randomUUID().toString().substring(0, 8));
        historia.setActiva(true);
        historia = historiaClinicaRepository.save(historia);

        Consulta consulta = new Consulta();
        consulta.setHistoriaClinica(historia);
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

    private Cita crearCita() {
        Company company = new Company();
        company.setName("Empresa QA " + UUID.randomUUID());
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
        mascota.setNombreCompleto("Paciente QA");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setPeso(10.0);
        mascota.setApoderado(apoderado);
        mascota.setUuid(UUID.randomUUID().toString());
        mascota = mascotaRepository.save(mascota);

        Usuario veterinarioUser = usuario("veterinario", company);
        Empleado veterinario = new Empleado();
        veterinario.setUser(veterinarioUser);
        veterinario.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        veterinario.setNumeroDocumentoIdentidad(uniqueDigits(8));
        veterinario.setGenero(Genero.MASCULINO);
        veterinario.setEstado(true);
        veterinario = empleadoRepository.save(veterinario);

        ServiciosVeterinarios servicio = new ServiciosVeterinarios();
        servicio.setCompany(company);
        servicio.setNombre("Consulta QA");
        servicio.setDescripcion("Servicio controlado para RF-30");
        servicio.setPrecio(new BigDecimal("100.00"));
        servicio.setDisponible(true);
        servicio.setActivo(true);
        servicio.setDuracionEstimada(30);
        servicio.setPermiteEmergencia(false);
        servicio = serviciosVeterinariosRepository.save(servicio);

        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setEmpleado(veterinario);
        cita.setServicio(servicio);
        cita.setMotivoCita("Control general");
        cita.setFechaHoraInicio(LocalDateTime.now().minusMinutes(20));
        cita.setFechaHoraFin(LocalDateTime.now().plusMinutes(10));
        cita.setDuracionMinutos(30);
        cita.setEstado(EstadoCita.EN_PROCESO);
        cita.setTotalServicio(new BigDecimal("100.00"));
        cita.setMontoPagado(BigDecimal.ZERO);
        cita.setEliminada(false);
        cita.setEsEmergencia(false);
        return citaRepository.save(cita);
    }

    private Usuario usuario(String prefix, Company company) {
        Usuario usuario = new Usuario();
        usuario.setEmail(prefix + "-" + UUID.randomUUID() + "@vargasvet.test");
        usuario.setPassword("credencial-ficticia-no-utilizable");
        usuario.setNombre(prefix);
        usuario.setApellido("QA");
        usuario.setDni(uniqueDigits(8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(company);
        return usuarioRepository.save(usuario);
    }

    private void autenticarSuperAdmin() {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        var principal = new UsuarioPrincipal(
                1,
                "qa@vargasvet.test",
                "",
                authorities,
                null,
                1,
                RoleScope.PLATFORM,
                RolePurpose.PLATFORM_ADMIN,
                0L
        );
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal, null, authorities)
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
