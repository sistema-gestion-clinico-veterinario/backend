package veterinaria.vargasvet.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Consulta;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.HistoriaClinica;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Prescripcion;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.EstadoConsulta;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.TipoConsulta;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.dto.response.PrescripcionResumenResponse;
import veterinaria.vargasvet.mapper.ConsultaMapper;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.ConsultaRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.HistoriaClinicaRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.PrescripcionRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AuditLogService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class PrescripcionBuscarServiceTest {

    @Autowired private PrescripcionRepository prescripcionRepository;
    @Autowired private ConsultaRepository consultaRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private HistoriaClinicaRepository historiaClinicaRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private PrescripcionServiceImpl servicio;
    private Company clinica;
    private Company otraClinica;
    private Empleado veterinaria;
    private Empleado veterinarioDeOtra;

    @BeforeEach
    void datos() {
        servicio = new PrescripcionServiceImpl(prescripcionRepository, consultaRepository, empleadoRepository,
                new ConsultaMapper(Mockito.mock(UsuarioContactoService.class)), Mockito.mock(AuditLogService.class));
        clinica = clinica("A");
        otraClinica = clinica("B");
        veterinaria = empleado(clinica, "Lucia", "Rojas");
        veterinarioDeOtra = empleado(otraClinica, "Pedro", "Soto");
        comoUsuarioDe(clinica);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void devuelveSoloLasRecetasDeLaEmpresaDelUsuario() {
        Prescripcion propia = receta(clinica, veterinaria, "Luna", "Amoxicilina", "2026-11-10T10:00", "HC-001", null, null);
        receta(otraClinica, veterinarioDeOtra, "Toby", "Amoxicilina", "2026-11-10T11:00", "HC-002", null, null);

        assertThat(buscar(null, null, null, null, null, null, null, null, null).getContent())
                .extracting(PrescripcionResumenResponse::getId).containsExactly(propia.getId());
    }

    @Test
    void losOrdenaDeLaMasRecienteALaMasAntigua() {
        Prescripcion antigua = receta(clinica, veterinaria, "Luna", "A", "2026-11-01T10:00", "HC-001", null, null);
        Prescripcion reciente = receta(clinica, veterinaria, "Luna", "B", "2026-11-12T10:00", "HC-002", null, null);
        Prescripcion media = receta(clinica, veterinaria, "Luna", "C", "2026-11-05T10:00", "HC-003", null, null);

        assertThat(buscar(null, null, null, null, null, null, null, null, null).getContent())
                .extracting(PrescripcionResumenResponse::getId)
                .containsExactly(reciente.getId(), media.getId(), antigua.getId());
    }

    @Test
    void traeElPacienteElNumeroDeHistoriaYElVeterinario() {
        Prescripcion receta = receta(clinica, veterinaria, "Luna", "Amoxicilina", "2026-11-10T10:00", "HC-777", null, null);

        PrescripcionResumenResponse fila = buscar(null, null, null, null, null, null, null, null, null).getContent().get(0);

        assertThat(fila.getId()).isEqualTo(receta.getId());
        assertThat(fila.getMedicamento()).isEqualTo("Amoxicilina");
        assertThat(fila.getPrincipioActivo()).isEqualTo("principio de Amoxicilina");
        assertThat(fila.getDosis()).isEqualTo("1 tableta");
        assertThat(fila.getFrecuencia()).isEqualTo("Cada 12 horas");
        assertThat(fila.getDuracionDias()).isEqualTo(7);
        assertThat(fila.getFechaInicio()).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(fila.getFechaCreacion()).isEqualTo(LocalDateTime.parse("2026-11-10T10:00"));
        assertThat(fila.getConsultaId()).isEqualTo(receta.getConsulta().getId());
        assertThat(fila.getPacienteNombre()).isEqualTo("Luna");
        assertThat(fila.getNumeroHc()).isEqualTo("HC-777");
        assertThat(fila.getVeterinarioNombre()).isEqualTo("Lucia Rojas");
    }

    @Test
    void elTextoLibreBuscaEnMedicamentoPrincipioActivoMascotaYHistoria() {
        Prescripcion porMedicamento = receta(clinica, veterinaria, "Luna", "Amoxicilina", "2026-11-10T10:00", "HC-001", null, null);
        Prescripcion porMascota = receta(clinica, veterinaria, "Rocky", "Otro", "2026-11-10T11:00", "HC-002", null, null);
        Prescripcion porHistoria = receta(clinica, veterinaria, "Mila", "Otro", "2026-11-10T12:00", "HC-ZZZ9", null, null);
        receta(clinica, veterinaria, "Nala", "Otro", "2026-11-10T13:00", "HC-004", null, null);

        assertThat(ids(buscar("AMOXI", null, null, null, null, null, null, null, null))).containsExactly(porMedicamento.getId());
        assertThat(ids(buscar("principio de otro", null, null, null, null, null, null, null, null))).hasSize(3);
        assertThat(ids(buscar("rocky", null, null, null, null, null, null, null, null))).containsExactly(porMascota.getId());
        assertThat(ids(buscar("zzz9", null, null, null, null, null, null, null, null))).containsExactly(porHistoria.getId());
    }

    @Test
    void filtraPorNombreDeMascotaMicrochipDocumentosYNumeroDeHistoria() {
        Prescripcion buscada = receta(clinica, veterinaria, "Firulais", "Amoxicilina", "2026-11-10T10:00", "HC-555", "981000123", "70123456");
        receta(clinica, veterinaria, "Luna", "Amoxicilina", "2026-11-10T11:00", "HC-556", "981000999", "70999999");

        assertThat(ids(buscar(null, "firu", null, null, null, null, null, null, null))).containsExactly(buscada.getId());
        assertThat(ids(buscar(null, null, "1000123", null, null, null, null, null, null))).containsExactly(buscada.getId());
        assertThat(ids(buscar(null, null, null, "70123", null, null, null, null, null))).containsExactly(buscada.getId());
        assertThat(ids(buscar(null, null, null, null, veterinaria.getNumeroDocumentoIdentidad(), null, null, null, null))).hasSize(2);
        assertThat(ids(buscar(null, null, null, null, null, "hc-555", null, null, null))).containsExactly(buscada.getId());
    }

    @Test
    void elRangoDeFechasIncluyeLosDiasExtremosCompletos() {
        receta(clinica, veterinaria, "Luna", "A", "2026-10-31T23:59:59", "HC-001", null, null);
        Prescripcion primerMomento = receta(clinica, veterinaria, "Luna", "B", "2026-11-01T00:00:00", "HC-002", null, null);
        Prescripcion ultimoMomento = receta(clinica, veterinaria, "Luna", "C", "2026-11-30T23:59:59", "HC-003", null, null);
        receta(clinica, veterinaria, "Luna", "D", "2026-12-01T00:00:00", "HC-004", null, null);

        Page<PrescripcionResumenResponse> pagina = buscar(null, null, null, null, null, null,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null);

        assertThat(ids(pagina)).containsExactly(ultimoMomento.getId(), primerMomento.getId());
        assertThat(pagina.getTotalElements()).isEqualTo(2);
    }

    @Test
    void soloConFechaDesdeOSoloConFechaHasta() {
        Prescripcion vieja = receta(clinica, veterinaria, "Luna", "A", "2026-10-01T10:00", "HC-001", null, null);
        Prescripcion nueva = receta(clinica, veterinaria, "Luna", "B", "2026-11-20T10:00", "HC-002", null, null);

        assertThat(ids(buscar(null, null, null, null, null, null, LocalDate.of(2026, 11, 1), null, null))).containsExactly(nueva.getId());
        assertThat(ids(buscar(null, null, null, null, null, null, null, LocalDate.of(2026, 10, 31), null))).containsExactly(vieja.getId());
    }

    @Test
    void paginaYCuentaElTotal() {
        for (int dia = 1; dia <= 5; dia++) {
            receta(clinica, veterinaria, "Luna", "M" + dia, "2026-11-0" + dia + "T10:00", "HC-00" + dia, null, null);
        }

        Page<PrescripcionResumenResponse> segunda = servicio.buscar(null, null, null, null, null, null, null, null, null, 1, 2);

        assertThat(segunda.getTotalElements()).isEqualTo(5);
        assertThat(segunda.getTotalPages()).isEqualTo(3);
        assertThat(segunda.getContent()).extracting(PrescripcionResumenResponse::getMedicamento).containsExactly("M3", "M2");
    }

    @Test
    void elSuperAdminSinEmpresaVeTodasYConEmpresaSoloEsa() {
        Prescripcion deA = receta(clinica, veterinaria, "Luna", "A", "2026-11-10T10:00", "HC-001", null, null);
        Prescripcion deB = receta(otraClinica, veterinarioDeOtra, "Toby", "B", "2026-11-11T10:00", "HC-002", null, null);
        comoSuperAdmin();

        assertThat(ids(servicio.buscar(null, null, null, null, null, null, null, null, null, 0, 20)))
                .containsExactly(deB.getId(), deA.getId());
        assertThat(ids(servicio.buscar(null, otraClinica.getId(), null, null, null, null, null, null, null, 0, 20)))
                .containsExactly(deB.getId());
    }

    @Test
    void unUsuarioSinEmpresaNoVeNada() {
        receta(clinica, veterinaria, "Luna", "A", "2026-11-10T10:00", "HC-001", null, null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UsuarioPrincipal(1, "sin@empresa.test", "x", List.of(), null), null, List.of()));

        assertThat(servicio.buscar(null, null, null, null, null, null, null, null, null, 0, 20).getContent()).isEmpty();
    }

    @Test
    void unaPaginaDeVeinteNoLanzaUnaConsultaPorReceta() {
        for (int i = 1; i <= 20; i++) {
            receta(clinica, veterinaria, "Mascota " + i, "Medicamento " + i, "2026-11-" + String.format("%02d", i) + "T10:00", "HC-" + i, null, null);
        }
        entityManager.flush();
        entityManager.clear();
        Statistics estadisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        estadisticas.clear();

        Page<PrescripcionResumenResponse> pagina = servicio.buscar(null, null, null, null, null, null, null, null, null, 0, 20);

        assertThat(pagina.getContent()).hasSize(20);
        assertThat(estadisticas.getPrepareStatementCount()).as("sentencias por página de 20 recetas").isEqualTo(2);
    }

    private Page<PrescripcionResumenResponse> buscar(String texto, String nombreMascota, String microchip, String documentoApoderado,
                                                      String documentoEmpleado, String numeroHc, LocalDate desde, LocalDate hasta,
                                                      Integer empresa) {
        entityManager.flush();
        entityManager.clear();
        return servicio.buscar(texto, empresa, nombreMascota, microchip, documentoApoderado, documentoEmpleado, numeroHc, desde, hasta, 0, 20);
    }

    private List<Long> ids(Page<PrescripcionResumenResponse> pagina) {
        return pagina.getContent().stream().map(PrescripcionResumenResponse::getId).toList();
    }

    private void comoUsuarioDe(Company empresa) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UsuarioPrincipal(1, "vet@test", "x", List.of(), empresa.getId()), null, List.of()));
    }

    private void comoSuperAdmin() {
        UsuarioPrincipal superAdmin = new UsuarioPrincipal(999, "super@test", "x", List.of(), null, null, null,
                RolePurpose.PLATFORM_ADMIN, 0L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(superAdmin, null, List.of()));
    }

    private Company clinica(String sufijo) {
        Company company = new Company();
        company.setName("Clinica " + sufijo);
        company.setSlug("clinica-" + sufijo.toLowerCase() + "-" + UUID.randomUUID());
        company.setRuc(digitos(11));
        company.setActivo(true);
        return companyRepository.save(company);
    }

    private Usuario usuario(String nombre, String apellido, Company empresa) {
        Usuario usuario = new Usuario();
        usuario.setEmail(nombre + "-" + UUID.randomUUID() + "@recetas.test");
        usuario.setUsername(nombre + "-" + UUID.randomUUID());
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setDni(digitos(8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(empresa);
        return usuarioRepository.save(usuario);
    }

    private Empleado empleado(Company empresa, String nombre, String apellido) {
        Empleado empleado = new Empleado();
        empleado.setUser(usuario(nombre, apellido, empresa));
        empleado.setCompany(empresa);
        empleado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        empleado.setNumeroDocumentoIdentidad(digitos(8));
        empleado.setGenero(Genero.FEMENINO);
        empleado.setEstado(true);
        return empleadoRepository.save(empleado);
    }

    private Prescripcion receta(Company empresa, Empleado vet, String mascotaNombre, String medicamento, String creadaEn,
                                String numeroHc, String microchip, String documentoApoderado) {
        Usuario duenio = usuario("duenio", "Test", empresa);
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(duenio);
        apoderado.setCompany(empresa);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setNumeroDocumento(documentoApoderado != null ? documentoApoderado : digitos(8));
        apoderado.setGenero(Genero.FEMENINO);
        apoderado = apoderadoRepository.save(apoderado);

        Mascota mascota = new Mascota();
        mascota.setNombreCompleto(mascotaNombre);
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(apoderado);
        mascota.setNumeroMicrochip(microchip);
        mascota.setUuid(UUID.randomUUID().toString());
        mascota = mascotaRepository.save(mascota);

        HistoriaClinica historia = new HistoriaClinica();
        historia.setMascota(mascota);
        historia.setNumeroHc(numeroHc);
        historia.setActiva(true);
        historia = historiaClinicaRepository.save(historia);

        LocalDateTime fecha = LocalDateTime.parse(creadaEn);
        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setEmpleado(vet);
        cita.setNumeroCita("CIT-" + UUID.randomUUID().toString().substring(0, 12));
        cita.setMotivoCita("Control");
        cita.setFechaHoraInicio(fecha);
        cita.setFechaHoraFin(fecha.plusMinutes(30));
        cita.setDuracionMinutos(30);
        cita.setEstado(EstadoCita.COMPLETADA);
        cita.setTotalServicio(new BigDecimal("100.00"));
        cita.setMontoPagado(new BigDecimal("100.00"));
        cita.setEliminada(false);
        cita.setEsEmergencia(false);
        cita = citaRepository.save(cita);

        Consulta consulta = new Consulta();
        consulta.setHistoriaClinica(historia);
        consulta.setCita(cita);
        consulta.setVeterinario(vet);
        consulta.setEstado(EstadoConsulta.CERRADA);
        consulta.setFechaConsulta(fecha);
        consulta.setMotivoConsulta("Control");
        consulta.setTipoConsulta(TipoConsulta.CONTROL_RUTINA);
        consulta = consultaRepository.save(consulta);

        Prescripcion prescripcion = new Prescripcion();
        prescripcion.setConsulta(consulta);
        prescripcion.setVeterinario(vet);
        prescripcion.setMedicamento(medicamento);
        prescripcion.setPrincipioActivo("principio de " + medicamento);
        prescripcion.setDosis("1 tableta");
        prescripcion.setFrecuencia("Cada 12 horas");
        prescripcion.setDuracionDias(7);
        prescripcion.setFechaInicio(fecha.toLocalDate());
        prescripcion.setCreatedAt(fecha);
        return prescripcionRepository.save(prescripcion);
    }

    private String digitos(int largo) {
        String digitos = String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        while (digitos.length() < largo) {
            digitos += "0";
        }
        return digitos.substring(0, largo);
    }
}
