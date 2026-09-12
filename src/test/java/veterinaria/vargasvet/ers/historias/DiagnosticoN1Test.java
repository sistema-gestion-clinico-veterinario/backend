package veterinaria.vargasvet.ers.historias;

import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.entity.*;
import veterinaria.vargasvet.domain.enums.*;
import veterinaria.vargasvet.repository.*;
import veterinaria.vargasvet.security.RolePermissionEvaluator;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.impl.ArchivoClinicoServiceImpl;
import veterinaria.vargasvet.service.impl.HistoriaClinicaServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;

/**
 * Diagnostico de consultas SQL para GET /medical-records/pet/{petId}, que
 * corresponde a HistoriaClinicaServiceImpl.getPorMascota. Reproduce la misma
 * forma de datos observada en produccion (historia con 3 consultas, 0
 * archivos) usando H2 y las estadisticas de Hibernate para contar SELECTs
 * reales, no estimados.
 */
@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class DiagnosticoN1Test {

    @Autowired private HistoriaClinicaRepository historiaClinicaRepository;
    @Autowired private ConsultaRepository consultaRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private DiagnosticoRepository diagnosticoRepository;
    @Autowired private TratamientoRepository tratamientoRepository;
    @Autowired private PrescripcionRepository prescripcionRepository;
    @Autowired private jakarta.persistence.EntityManagerFactory entityManagerFactory;
    @Autowired private jakarta.persistence.EntityManager entityManager;
    @Autowired private CitaRepository citaRepository;
    @Autowired private ServiciosVeterinariosRepository serviciosVeterinariosRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cuentaConsultasSqlParaGetPorMascotaConTresConsultas() {
        Company company = new Company();
        company.setName("Empresa QA N+1");
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
        mascota.setNombreCompleto("Paciente QA N+1");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(apoderado);
        mascota.setUuid(UUID.randomUUID().toString());
        mascota = mascotaRepository.save(mascota);

        Usuario vetUser = usuario("vet", company);
        Empleado veterinario = new Empleado();
        veterinario.setUser(vetUser);
        veterinario.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        veterinario.setNumeroDocumentoIdentidad(uniqueDigits(8));
        veterinario.setGenero(Genero.MASCULINO);
        veterinario.setEstado(true);
        veterinario = empleadoRepository.save(veterinario);

        HistoriaClinica historia = new HistoriaClinica();
        historia.setMascota(mascota);
        historia.setNumeroHc("HC-" + UUID.randomUUID().toString().substring(0, 8));
        historia.setActiva(true);
        historia = historiaClinicaRepository.save(historia);

        ServiciosVeterinarios servicio = new ServiciosVeterinarios();
        servicio.setCompany(company);
        servicio.setNombre("Consulta QA N+1");
        servicio.setDescripcion("Servicio de diagnostico");
        servicio.setPrecio(new BigDecimal("100.00"));
        servicio.setDisponible(true);
        servicio.setActivo(true);
        servicio.setDuracionEstimada(30);
        servicio.setPermiteEmergencia(false);
        servicio = serviciosVeterinariosRepository.save(servicio);

        for (int i = 0; i < 3; i++) {
            Cita cita = new Cita();
            cita.setMascota(mascota);
            cita.setEmpleado(veterinario);
            cita.setServicio(servicio);
            cita.setMotivoCita("Control " + i);
            cita.setFechaHoraInicio(LocalDateTime.now().minusDays(i).minusMinutes(30));
            cita.setFechaHoraFin(LocalDateTime.now().minusDays(i));
            cita.setDuracionMinutos(30);
            cita.setEstado(EstadoCita.COMPLETADA);
            cita.setTotalServicio(new BigDecimal("100.00"));
            cita.setMontoPagado(BigDecimal.ZERO);
            cita.setEliminada(false);
            cita.setEsEmergencia(false);
            cita = citaRepository.save(cita);

            Consulta consulta = new Consulta();
            consulta.setHistoriaClinica(historia);
            consulta.setCita(cita);
            consulta.setVeterinario(veterinario);
            consulta.setFechaConsulta(LocalDateTime.now().minusDays(i));
            consulta.setMotivoConsulta("Control " + i);
            consulta.setTipoConsulta(TipoConsulta.CONTROL_RUTINA);
            consulta.setEstado(EstadoConsulta.CERRADA);
            consulta.setPesoEnConsulta(10.0 + i);
            consulta.setAnamnesis("Paciente estable " + i);
            consulta.setVacunacionAplicada(false);
            consulta.setDesparasitacionAplicada(false);
            consulta = consultaRepository.save(consulta);

            Diagnostico diagnostico = new Diagnostico();
            diagnostico.setConsulta(consulta);
            diagnostico.setNombre("Diagnostico " + i);
            diagnostico.setTipo(TipoDiagnostico.PRESUNTIVO);
            diagnostico.setEstado(EstadoDiagnostico.ACTIVO);
            diagnosticoRepository.save(diagnostico);

            Tratamiento tratamiento = new Tratamiento();
            tratamiento.setConsulta(consulta);
            tratamiento.setNombre("Tratamiento " + i);
            tratamiento.setEstado(EstadoTratamiento.ACTIVO);
            tratamientoRepository.save(tratamiento);

            Prescripcion prescripcion = new Prescripcion();
            prescripcion.setConsulta(consulta);
            prescripcion.setMedicamento("Medicamento " + i);
            prescripcion.setDosis("1 tableta");
            prescripcion.setFrecuencia("Cada 12h");
            prescripcion.setViaAdministracion("Oral");
            prescripcion.setFechaInicio(java.time.LocalDate.now().minusDays(i));
            prescripcionRepository.save(prescripcion);
        }

        autenticarComoSuperAdmin();

        ArchivoClinicoServiceImpl archivoService = new ArchivoClinicoServiceImpl(
                consultaRepository, mock(ArchivoClinicoRepository.class), mock(veterinaria.vargasvet.service.StorageService.class),
                mock(veterinaria.vargasvet.service.AuditLogService.class));
        HistoriaClinicaServiceImpl service = new HistoriaClinicaServiceImpl(
                historiaClinicaRepository, consultaRepository, archivoService);
        ReflectionTestUtils.setField(service, "accesoValidator", mock(AccesoValidator.class));
        ReflectionTestUtils.setField(service, "empleadoRepository", empleadoRepository);
        ReflectionTestUtils.setField(service, "prescripcionRepository", prescripcionRepository);

        // Simula una peticion HTTP real: un contexto de persistencia nuevo, sin cache
        // de primer nivel heredado de la configuracion de datos de esta prueba.
        entityManager.flush();
        entityManager.clear();

        Statistics stats = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        stats.clear();

        Long mascotaId = mascota.getId();
        var respuesta = service.getPorMascota(mascotaId);

        System.out.println("=== DIAGNOSTICO N+1 ===");
        System.out.println("Consultas devueltas: " + respuesta.getConsultas().size());
        System.out.println("Numero de sentencias SQL (queries) ejecutadas: " + stats.getQueryExecutionCount());
        System.out.println("Numero de entity loads: " + stats.getEntityLoadCount());
        System.out.println("Numero de collection fetches: " + stats.getCollectionFetchCount());
        System.out.println("Numero de collection loads: " + stats.getCollectionLoadCount());
        System.out.println("Tiempo total en preparar sentencias (ns): " + stats.getPrepareStatementCount());

        // Verificacion de equivalencia funcional: no solo el conteo de SQL, sino que el
        // contenido devuelto sea exactamente el esperado (orden descendente por fecha,
        // y cada sub-coleccion con sus datos correctos).
        org.junit.jupiter.api.Assertions.assertEquals(3, respuesta.getConsultas().size());
        for (int i = 0; i < 3; i++) {
            var consultaResumen = respuesta.getConsultas().get(i);
            org.junit.jupiter.api.Assertions.assertEquals(1, consultaResumen.getDiagnosticos().size());
            org.junit.jupiter.api.Assertions.assertEquals("Diagnostico " + i, consultaResumen.getDiagnosticos().get(0).getNombre());
            org.junit.jupiter.api.Assertions.assertEquals(1, consultaResumen.getTratamientos().size());
            org.junit.jupiter.api.Assertions.assertEquals("Tratamiento " + i, consultaResumen.getTratamientos().get(0).getNombre());
            org.junit.jupiter.api.Assertions.assertEquals(1, consultaResumen.getPrescripciones().size());
            org.junit.jupiter.api.Assertions.assertEquals("Medicamento " + i, consultaResumen.getPrescripciones().get(0).getMedicamento());
            org.junit.jupiter.api.Assertions.assertEquals("1 tableta", consultaResumen.getPrescripciones().get(0).getDosis());
            org.junit.jupiter.api.Assertions.assertEquals("Cada 12h", consultaResumen.getPrescripciones().get(0).getFrecuencia());
            org.junit.jupiter.api.Assertions.assertEquals("Oral", consultaResumen.getPrescripciones().get(0).getViaAdministracion());
            org.junit.jupiter.api.Assertions.assertEquals(0, consultaResumen.getArchivos().size());
        }
    }

    private Usuario usuario(String prefix, Company company) {
        Usuario usuario = new Usuario();
        usuario.setEmail(prefix + "-" + UUID.randomUUID() + "@vargasvet.test");
        usuario.setPassword("hash-no-real");
        usuario.setNombre(prefix);
        usuario.setApellido("QA");
        usuario.setDni(uniqueDigits(8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(company);
        return usuarioRepository.save(usuario);
    }

    private void autenticarComoSuperAdmin() {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        var principal = new UsuarioPrincipal(
                1, "qa@vargasvet.test", "", authorities, null, 1,
                RoleScope.PLATFORM, RolePurpose.PLATFORM_ADMIN, 0L);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal, null, authorities));
    }

    private String uniqueDigits(int length) {
        String digits = String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        while (digits.length() < length) digits += "0";
        return digits.substring(0, length);
    }
}
