package veterinaria.vargasvet.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManager;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.ServiciosVeterinarios;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.dto.response.ReportesClinicosDTO;
import veterinaria.vargasvet.service.reportes.RangoDeReporte;
import veterinaria.vargasvet.service.reportes.ReporteActividadClinica;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CitaResumenPorEmpresaTest {

    @Autowired private CitaRepository citaRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private ServiciosVeterinariosRepository serviciosRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;

    private final ReporteActividadClinica actividad = new ReporteActividadClinica();
    private final LocalDate hoy = LocalDate.now();
    private final RangoDeReporte rango = new RangoDeReporte(hoy.minusDays(10), hoy);

    private final List<Company> clinicas = new ArrayList<>();

    @BeforeEach
    void datos() {
        for (int numero = 1; numero <= 3; numero++) {
            Company clinica = clinica("Clinica " + numero);
            clinicas.add(clinica);
            sembrar(clinica, numero);
        }
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void cadaEmpresaCoincideConElResumenCalculadoSobreSusCitas() {
        assertCoincideConElResumenDeCitas(null);
    }

    @Test
    void elFiltroPorEspecieCoincideConElResumenCalculadoSobreSusCitas() {
        assertCoincideConElResumenDeCitas(EspecieMascota.GATO);
        assertCoincideConElResumenDeCitas(EspecieMascota.PERRO);
    }

    @Test
    void losDatosDePruebaProducenCifrasNoTriviales() {
        List<Integer> ids = clinicas.stream().map(Company::getId).toList();

        List<CitaRepository.ResumenPorEmpresa> filas = citaRepository.resumirPorEmpresa(
                ids, rango.inicio(), rango.finExclusivo(), null);

        assertThat(filas).hasSize(clinicas.size());
        assertThat(filas).allSatisfy(fila -> {
            assertThat(fila.getConsultas()).isPositive();
            assertThat(fila.getIngresos()).isPositive();
            assertThat(fila.getNuevosPacientes()).isPositive();
            assertThat(fila.getNoAsistieron()).isPositive();
            assertThat(fila.getCompletadas()).isPositive();
            assertThat(fila.getCerradas()).isGreaterThanOrEqualTo(fila.getCompletadas());
        });
    }

    @Test
    void soloDevuelveLasEmpresasPedidas() {
        List<CitaRepository.ResumenPorEmpresa> filas = citaRepository.resumirPorEmpresa(
                List.of(clinicas.get(0).getId()), rango.inicio(), rango.finExclusivo(), null);

        assertThat(filas).extracting(CitaRepository.ResumenPorEmpresa::getCompanyId)
                .containsExactly(clinicas.get(0).getId());
    }

    @Test
    void unaEmpresaSinCitasEnElPeriodoNoApareceEnElResultado() {
        List<CitaRepository.ResumenPorEmpresa> filas = citaRepository.resumirPorEmpresa(
                List.of(clinicas.get(0).getId()),
                hoy.minusYears(5).atStartOfDay(), hoy.minusYears(4).atStartOfDay(), null);

        assertThat(filas).isEmpty();
    }

    private void assertCoincideConElResumenDeCitas(EspecieMascota especie) {
        List<Integer> ids = clinicas.stream().map(Company::getId).toList();
        Map<Integer, CitaRepository.ResumenPorEmpresa> agregados = citaRepository
                .resumirPorEmpresa(ids, rango.inicio(), rango.finExclusivo(), especie).stream()
                .collect(Collectors.toMap(CitaRepository.ResumenPorEmpresa::getCompanyId, Function.identity()));

        for (Company clinica : clinicas) {
            List<Cita> citas = citaRepository.findForClinicalReport(
                    clinica.getId(), rango.inicio(), rango.finExclusivo(), null, especie);
            ReportesClinicosDTO.Resumen esperado = actividad.resumen(citas, rango, true);
            CitaRepository.ResumenPorEmpresa real = agregados.get(clinica.getId());

            if (citas.isEmpty()) {
                assertThat(real).as("empresa %s sin citas", clinica.getName()).isNull();
                continue;
            }
            assertThat(real.getConsultas()).as("consultas de %s", clinica.getName()).isEqualTo(esperado.getConsultas());
            assertThat(real.getPacientesAtendidos()).as("pacientes atendidos de %s", clinica.getName())
                    .isEqualTo(esperado.getPacientesAtendidos());
            assertThat(real.getIngresos().setScale(2)).as("ingresos de %s", clinica.getName())
                    .isEqualByComparingTo(esperado.getIngresos());
            assertThat(real.getNuevosPacientes()).as("nuevos pacientes de %s", clinica.getName())
                    .isEqualTo(esperado.getNuevosPacientes());
            assertThat(real.getNoAsistieron()).as("no asistieron de %s", clinica.getName())
                    .isEqualTo(esperado.getNoAsistieron());
            assertThat(ReporteActividadClinica.porcentajeDeCompletadas(real.getCompletadas(), real.getCerradas()))
                    .as("porcentaje completadas de %s", clinica.getName())
                    .isEqualTo(esperado.getPorcentajeCitasCompletadas());
        }
    }

    private void sembrar(Company clinica, int numero) {
        Apoderado dueno = apoderado(clinica);
        Empleado veterinario = empleado(clinica);
        ServiciosVeterinarios servicio = servicio(clinica);
        EstadoCita[] estados = EstadoCita.values();

        for (int i = 0; i < 12 + numero * 3; i++) {
            Mascota mascota = mascota(dueno, i % 3 == 0 ? EspecieMascota.GATO : EspecieMascota.PERRO);
            entityManager.flush();
            LocalDate registro = i % 2 == 0 ? hoy.minusDays(i % 8) : hoy.minusDays(40 + i);
            jdbc.update("UPDATE mascota SET created_at = ? WHERE id = ?", registro.atTime(10, 0), mascota.getId());
            for (int k = 0; k < 1 + i % 3; k++) {
                LocalDateTime inicio = hoy.minusDays(((i + k) % 12)).atTime(8 + (i + k) % 9, (i * 7 + k) % 60);
                Cita cita = cita(mascota, veterinario, servicio, estados[(i + k + numero) % estados.length], inicio);
                cita.setEliminada((i + k) % 11 == 0);
                aplicarMontos(cita, i + k);
                citaRepository.save(cita);
            }
        }
        Mascota enLosLimites = mascota(dueno, EspecieMascota.PERRO);
        cita(enLosLimites, veterinario, servicio, EstadoCita.COMPLETADA, rango.inicio());
        cita(enLosLimites, veterinario, servicio, EstadoCita.COMPLETADA, rango.finExclusivo());
        cita(enLosLimites, veterinario, servicio, EstadoCita.COMPLETADA, rango.finExclusivo().minusSeconds(1));
        cita(enLosLimites, veterinario, servicio, EstadoCita.COMPLETADA, rango.inicio().minusSeconds(1));
    }

    private void aplicarMontos(Cita cita, int variante) {
        switch (variante % 5) {
            case 0 -> { cita.setTotalServicio(new BigDecimal("100.00")); cita.setMontoPagado(new BigDecimal("40.00")); }
            case 1 -> { cita.setTotalServicio(new BigDecimal("100.00")); cita.setMontoPagado(new BigDecimal("250.00")); }
            case 2 -> { cita.setTotalServicio(BigDecimal.ZERO); cita.setMontoPagado(new BigDecimal("30.00")); }
            case 3 -> { cita.setTotalServicio(new BigDecimal("80.50")); cita.setMontoPagado(BigDecimal.ZERO); }
            default -> { cita.setTotalServicio(new BigDecimal("60.00")); cita.setMontoPagado(new BigDecimal("60.00")); }
        }
    }

    private Company clinica(String nombre) {
        Company company = new Company();
        company.setName(nombre);
        company.setSlug("c-" + UUID.randomUUID());
        company.setRuc(digitos(11));
        company.setActivo(true);
        return companyRepository.save(company);
    }

    private Apoderado apoderado(Company company) {
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(usuario(company, "cliente"));
        apoderado.setCompany(company);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setNumeroDocumento(digitos(8));
        apoderado.setGenero(Genero.FEMENINO);
        return apoderadoRepository.save(apoderado);
    }

    private Empleado empleado(Company company) {
        Empleado empleado = new Empleado();
        empleado.setUser(usuario(company, "vet"));
        empleado.setCompany(company);
        empleado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        empleado.setNumeroDocumentoIdentidad(digitos(8));
        empleado.setGenero(Genero.MASCULINO);
        empleado.setEstado(true);
        return empleadoRepository.save(empleado);
    }

    private ServiciosVeterinarios servicio(Company company) {
        ServiciosVeterinarios servicio = new ServiciosVeterinarios();
        servicio.setCompany(company);
        servicio.setNombre("Consulta general");
        servicio.setDescripcion("Consulta veterinaria general");
        servicio.setPrecio(new BigDecimal("100.00"));
        servicio.setDisponible(true);
        servicio.setActivo(true);
        servicio.setDuracionEstimada(30);
        servicio.setPermiteEmergencia(false);
        return serviciosRepository.save(servicio);
    }

    private Mascota mascota(Apoderado dueno, EspecieMascota especie) {
        Mascota mascota = new Mascota();
        mascota.setNombreCompleto("Paciente " + UUID.randomUUID());
        mascota.setEspecie(especie);
        mascota.setApoderado(dueno);
        mascota.setUuid(UUID.randomUUID().toString());
        return mascotaRepository.save(mascota);
    }

    private Cita cita(Mascota mascota, Empleado veterinario, ServiciosVeterinarios servicio,
                      EstadoCita estado, LocalDateTime inicio) {
        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setEmpleado(veterinario);
        cita.setServicio(servicio);
        cita.setMotivoCita("Control");
        cita.setFechaHoraInicio(inicio);
        cita.setFechaHoraFin(inicio.plusMinutes(30));
        cita.setDuracionMinutos(30);
        cita.setEstado(estado);
        cita.setTotalServicio(new BigDecimal("100.00"));
        cita.setMontoPagado(BigDecimal.ZERO);
        cita.setEliminada(false);
        cita.setEsEmergencia(false);
        return citaRepository.save(cita);
    }

    private Usuario usuario(Company company, String prefijo) {
        Usuario usuario = new Usuario();
        usuario.setEmail(prefijo + "-" + UUID.randomUUID() + "@clinica.test");
        usuario.setUsername(prefijo + "-" + UUID.randomUUID());
        usuario.setNombre(prefijo);
        usuario.setApellido("Test");
        usuario.setDni(digitos(8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(company);
        return usuarioRepository.save(usuario);
    }

    private String digitos(int largo) {
        String valor = String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        while (valor.length() < largo) {
            valor += "0";
        }
        return valor.substring(0, largo);
    }
}
