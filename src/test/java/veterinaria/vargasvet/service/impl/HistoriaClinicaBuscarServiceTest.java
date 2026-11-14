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
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Consulta;
import veterinaria.vargasvet.domain.entity.HistoriaClinica;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Raza;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.EstadoConsulta;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.TipoConsulta;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.dto.response.HistoriaClinicaListResponse;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.ConsultaRepository;
import veterinaria.vargasvet.repository.HistoriaClinicaRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.RazaRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.security.UsuarioPrincipal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class HistoriaClinicaBuscarServiceTest {

    @Autowired private HistoriaClinicaRepository historiaClinicaRepository;
    @Autowired private ConsultaRepository consultaRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private RazaRepository razaRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private HistoriaClinicaServiceImpl servicio;
    private Company clinica;
    private Company otraClinica;

    @BeforeEach
    void datos() {
        servicio = new HistoriaClinicaServiceImpl(historiaClinicaRepository, consultaRepository,
                Mockito.mock(ArchivoClinicoServiceImpl.class), Mockito.mock(UsuarioContactoService.class));
        AccesoValidator acceso = Mockito.mock(AccesoValidator.class);
        Mockito.when(acceso.canAccessCompanyData("VISTA_HISTORIAS")).thenReturn(true);
        ReflectionTestUtils.setField(servicio, "accesoValidator", acceso);
        clinica = clinica("A");
        otraClinica = clinica("B");
        comoUsuarioDe(clinica);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void devuelveSoloLasHistoriasDeLaEmpresaDelUsuario() {
        HistoriaClinica propia = historia(clinica, "Luna", "HC-001");
        historia(otraClinica, "Toby", "HC-002");

        assertThat(ids(buscar(null, null, null))).containsExactly(propia.getId());
    }

    @Test
    void lasOrdenaPorNumeroDeHistoria() {
        HistoriaClinica c = historia(clinica, "Luna", "HC-300");
        HistoriaClinica a = historia(clinica, "Rocky", "HC-100");
        HistoriaClinica b = historia(clinica, "Mila", "HC-200");

        assertThat(ids(buscar(null, null, null))).containsExactly(a.getId(), b.getId(), c.getId());
    }

    @Test
    void traeLosDatosDeLaMascotaDelPropietarioYDeLaEmpresa() {
        Raza labrador = new Raza();
        labrador.setNombre("Labrador");
        labrador.setEspecie(EspecieMascota.PERRO);
        labrador = razaRepository.save(labrador);
        HistoriaClinica historia = historia(clinica, "Luna", "HC-777");
        historia.getMascota().setRaza(labrador);
        consulta(historia, "2026-11-10T10:00");

        HistoriaClinicaListResponse fila = buscar(null, null, null).getContent().get(0);

        assertThat(fila.getId()).isEqualTo(historia.getId());
        assertThat(fila.getNumeroHc()).isEqualTo("HC-777");
        assertThat(fila.getActiva()).isTrue();
        assertThat(fila.getMascotaId()).isEqualTo(historia.getMascota().getId());
        assertThat(fila.getMascotaNombre()).isEqualTo("Luna");
        assertThat(fila.getEspecie()).isEqualTo("PERRO");
        assertThat(fila.getRaza()).isEqualTo("Labrador");
        assertThat(fila.getPropietarioNombre()).isEqualTo("Ana Perez");
        assertThat(fila.getCompanyId()).isEqualTo(clinica.getId());
        assertThat(fila.getCompanyName()).isEqualTo("Clinica A");
        assertThat(fila.getFechaUltimaConsulta()).isEqualTo(LocalDateTime.parse("2026-11-10T10:00"));
    }

    @Test
    void sinRazaLaRazaVieneNula() {
        historia(clinica, "Kiwi", "HC-001");

        assertThat(buscar(null, null, null).getContent().get(0).getRaza()).isNull();
    }

    @Test
    void laFechaDeLaUltimaConsultaEsLaMasRecienteOSinConsultasNula() {
        HistoriaClinica conConsultas = historia(clinica, "Luna", "HC-001");
        consulta(conConsultas, "2026-10-01T09:00");
        consulta(conConsultas, "2026-11-05T09:00");
        consulta(conConsultas, "2026-09-01T09:00");
        historia(clinica, "Rocky", "HC-002");

        List<HistoriaClinicaListResponse> filas = buscar(null, null, null).getContent();

        assertThat(filas.get(0).getFechaUltimaConsulta()).isEqualTo(LocalDateTime.parse("2026-11-05T09:00"));
        assertThat(filas.get(1).getFechaUltimaConsulta()).isNull();
    }

    @Test
    void filtraPorNumeroDeHistoriaExacto() {
        HistoriaClinica buscada = historia(clinica, "Luna", "HC-555");
        historia(clinica, "Rocky", "HC-5556");

        assertThat(ids(servicio.buscar("HC-555", null, null, null, null, null, 0, 20))).containsExactly(buscada.getId());
    }

    @Test
    void filtraPorElRangoDeFechasDeSusConsultas() {
        HistoriaClinica dentro = historia(clinica, "Luna", "HC-001");
        consulta(dentro, "2026-11-30T23:59:59");
        HistoriaClinica antes = historia(clinica, "Rocky", "HC-002");
        consulta(antes, "2026-10-31T23:59:59");
        HistoriaClinica despues = historia(clinica, "Mila", "HC-003");
        consulta(despues, "2026-12-01T00:00:00");
        historia(clinica, "Nala", "HC-004");

        Page<HistoriaClinicaListResponse> pagina = buscar(null, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertThat(ids(pagina)).containsExactly(dentro.getId());
        assertThat(pagina.getTotalElements()).isEqualTo(1);
    }

    @Test
    void paginaYCuentaElTotal() {
        for (int i = 1; i <= 5; i++) {
            historia(clinica, "Mascota" + i, "HC-00" + i);
        }

        Page<HistoriaClinicaListResponse> segunda = servicio.buscar(null, null, null, null, null, null, 1, 2);

        assertThat(segunda.getTotalElements()).isEqualTo(5);
        assertThat(segunda.getTotalPages()).isEqualTo(3);
        assertThat(segunda.getContent()).extracting(HistoriaClinicaListResponse::getNumeroHc).containsExactly("HC-003", "HC-004");
    }

    @Test
    void elSuperAdminSinEmpresaVeTodasYConEmpresaSoloEsa() {
        HistoriaClinica deA = historia(clinica, "Luna", "HC-001");
        HistoriaClinica deB = historia(otraClinica, "Toby", "HC-002");
        comoSuperAdmin();

        assertThat(ids(servicio.buscar(null, null, null, null, null, null, 0, 20))).containsExactly(deA.getId(), deB.getId());
        assertThat(ids(servicio.buscar(null, null, null, null, null, otraClinica.getId(), 0, 20))).containsExactly(deB.getId());
    }

    @Test
    void unUsuarioSinEmpresaNoVeNada() {
        historia(clinica, "Luna", "HC-001");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UsuarioPrincipal(1, "sin@empresa.test", "x", List.of(), null), null, List.of()));

        assertThat(servicio.buscar(null, null, null, null, null, null, 0, 20).getContent()).isEmpty();
    }

    @Test
    void unaPaginaDeVeinteNoLanzaUnaConsultaPorHistoria() {
        for (int i = 1; i <= 20; i++) {
            HistoriaClinica historia = historia(clinica, "Mascota" + i, String.format("HC-%03d", i));
            consulta(historia, "2026-11-" + String.format("%02d", i) + "T10:00");
        }
        entityManager.flush();
        entityManager.clear();
        Statistics estadisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        estadisticas.clear();

        Page<HistoriaClinicaListResponse> pagina = servicio.buscar(null, null, null, null, null, null, 0, 20);

        assertThat(pagina.getContent()).hasSize(20);
        assertThat(estadisticas.getPrepareStatementCount()).as("sentencias por página de 20 historias").isEqualTo(3);
    }

    private Page<HistoriaClinicaListResponse> buscar(String numeroHc, LocalDate desde, LocalDate hasta) {
        entityManager.flush();
        entityManager.clear();
        return servicio.buscar(numeroHc, null, null, desde, hasta, null, 0, 20);
    }

    private List<Long> ids(Page<HistoriaClinicaListResponse> pagina) {
        return pagina.getContent().stream().map(HistoriaClinicaListResponse::getId).toList();
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

    private HistoriaClinica historia(Company empresa, String nombreMascota, String numeroHc) {
        Usuario duenio = new Usuario();
        duenio.setEmail("duenio-" + UUID.randomUUID() + "@historias.test");
        duenio.setUsername("duenio-" + UUID.randomUUID());
        duenio.setNombre("Ana");
        duenio.setApellido("Perez");
        duenio.setDni(digitos(8));
        duenio.setActivo(true);
        duenio.setEmailVerified(true);
        duenio.setCompany(empresa);
        duenio = usuarioRepository.save(duenio);

        Apoderado apoderado = new Apoderado();
        apoderado.setUser(duenio);
        apoderado.setCompany(empresa);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setNumeroDocumento(digitos(8));
        apoderado.setGenero(Genero.FEMENINO);
        apoderado = apoderadoRepository.save(apoderado);

        Mascota mascota = new Mascota();
        mascota.setNombreCompleto(nombreMascota);
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(apoderado);
        mascota.setUuid(UUID.randomUUID().toString());
        mascota = mascotaRepository.save(mascota);

        HistoriaClinica historia = new HistoriaClinica();
        historia.setMascota(mascota);
        historia.setNumeroHc(numeroHc);
        historia.setActiva(true);
        return historiaClinicaRepository.save(historia);
    }

    private Consulta consulta(HistoriaClinica historia, String fecha) {
        LocalDateTime momento = LocalDateTime.parse(fecha);
        Cita cita = new Cita();
        cita.setMascota(historia.getMascota());
        cita.setNumeroCita("CIT-" + UUID.randomUUID().toString().substring(0, 12));
        cita.setMotivoCita("Control");
        cita.setFechaHoraInicio(momento);
        cita.setFechaHoraFin(momento.plusMinutes(30));
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
        consulta.setEstado(EstadoConsulta.CERRADA);
        consulta.setFechaConsulta(momento);
        consulta.setMotivoConsulta("Control");
        consulta.setTipoConsulta(TipoConsulta.CONTROL_RUTINA);
        return consultaRepository.save(consulta);
    }

    private String digitos(int largo) {
        String digitos = String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        while (digitos.length() < largo) {
            digitos += "0";
        }
        return digitos.substring(0, largo);
    }
}
