package veterinaria.vargasvet.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MascotaInactivasParaReporteTest {

    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private ServiciosVeterinariosRepository serviciosRepository;

    private final LocalDateTime umbral = LocalDateTime.now().minusDays(90).withNano(0);

    private Company clinica;
    private Apoderado cliente;
    private Empleado veterinario;
    private ServiciosVeterinarios consulta;

    @BeforeEach
    void datos() {
        clinica = clinica("Clinica Patitas");
        cliente = apoderado(clinica, "Maria");
        veterinario = empleado(clinica);
        consulta = servicio(clinica);

        Mascota antigua = mascota(cliente, "Luna", true);
        cita(antigua, EstadoCita.COMPLETADA, umbral.minusDays(30), false);

        Mascota reciente = mascota(cliente, "Rocky", true);
        cita(reciente, EstadoCita.COMPLETADA, umbral.plusDays(60), false);

        Mascota conVisitaAntiguaYReciente = mascota(cliente, "Nala", true);
        cita(conVisitaAntiguaYReciente, EstadoCita.COMPLETADA, umbral.minusDays(60), false);
        cita(conVisitaAntiguaYReciente, EstadoCita.COMPLETADA, umbral.plusDays(10), false);

        mascota(cliente, "Mia", true);

        Mascota soloCitaCancelada = mascota(cliente, "Kira", true);
        cita(soloCitaCancelada, EstadoCita.COMPLETADA, umbral.minusDays(100), false);
        cita(soloCitaCancelada, EstadoCita.CANCELADA, umbral.plusDays(20), false);

        Mascota soloCitaEliminada = mascota(cliente, "Toby", true);
        cita(soloCitaEliminada, EstadoCita.COMPLETADA, umbral.minusDays(100), false);
        cita(soloCitaEliminada, EstadoCita.COMPLETADA, umbral.plusDays(20), true);

        Mascota justoEnElUmbral = mascota(cliente, "Limite", true);
        cita(justoEnElUmbral, EstadoCita.COMPLETADA, umbral, false);

        Mascota justoAntesDelUmbral = mascota(cliente, "JustoAntes", true);
        cita(justoAntesDelUmbral, EstadoCita.COMPLETADA, umbral.minusMinutes(1), false);

        mascota(cliente, "Bobby", false);

        Company otra = clinica("Clinica Ajena");
        Apoderado ajeno = apoderado(otra, "Pedro");
        mascota(ajeno, "Ajena", true);
    }

    private Map<String, MascotaRepository.PacienteInactivoFila> inactivas() {
        return mascotaRepository.findInactivasParaReporte(clinica.getId(), umbral).stream()
                .collect(Collectors.toMap(MascotaRepository.PacienteInactivoFila::getMascota, Function.identity()));
    }

    @Test
    void incluyeSoloLasMascotasActivasSinVisitaCompletadaDesdeElUmbral() {
        assertThat(inactivas().keySet()).containsExactlyInAnyOrder("Luna", "Mia", "Kira", "Toby", "JustoAntes");
    }

    @Test
    void unaVisitaJustoEnElUmbralYaCuentaComoReciente() {
        assertThat(inactivas()).doesNotContainKey("Limite");
    }

    @Test
    void unaCitaCanceladaNoCuentaComoVisita() {
        assertThat(inactivas().get("Kira").getUltimaVisita()).isEqualTo(umbral.minusDays(100));
    }

    @Test
    void unaCitaEliminadaNoCuentaComoVisita() {
        assertThat(inactivas().get("Toby").getUltimaVisita()).isEqualTo(umbral.minusDays(100));
    }

    @Test
    void laUltimaVisitaEsLaMasRecienteDeTodasLasCompletadas() {
        assertThat(inactivas().get("Luna").getUltimaVisita()).isEqualTo(umbral.minusDays(30));
    }

    @Test
    void unaMascotaQueNuncaVisitoNoTieneUltimaVisitaPeroTraeLaFechaDeRegistro() {
        MascotaRepository.PacienteInactivoFila fila = inactivas().get("Mia");

        assertThat(fila.getUltimaVisita()).isNull();
        assertThat(fila.getCreadoEn()).isNotNull();
    }

    @Test
    void traeElNombreDelPropietario() {
        MascotaRepository.PacienteInactivoFila fila = inactivas().get("Luna");

        assertThat(fila.getNombreApoderado()).isEqualTo("Maria");
        assertThat(fila.getApellidoApoderado()).isEqualTo("Test");
    }

    @Test
    void noMezclaMascotasDeOtraClinica() {
        assertThat(inactivas()).doesNotContainKey("Ajena");
    }

    private Company clinica(String nombre) {
        Company company = new Company();
        company.setName(nombre);
        company.setSlug("c-" + UUID.randomUUID());
        company.setRuc(digitos(11));
        company.setActivo(true);
        return companyRepository.save(company);
    }

    private Apoderado apoderado(Company company, String nombre) {
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(usuario(company, nombre));
        apoderado.setCompany(company);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setNumeroDocumento(digitos(8));
        apoderado.setGenero(Genero.FEMENINO);
        return apoderadoRepository.save(apoderado);
    }

    private Empleado empleado(Company company) {
        Empleado empleado = new Empleado();
        empleado.setUser(usuario(company, "Vet"));
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

    private Mascota mascota(Apoderado dueno, String nombre, boolean activa) {
        Mascota mascota = new Mascota();
        mascota.setNombreCompleto(nombre);
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(dueno);
        mascota.setUuid(UUID.randomUUID().toString());
        mascota.setActivo(activa);
        return mascotaRepository.save(mascota);
    }

    private Cita cita(Mascota mascota, EstadoCita estado, LocalDateTime inicio, boolean eliminada) {
        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setEmpleado(veterinario);
        cita.setServicio(consulta);
        cita.setMotivoCita("Control");
        cita.setFechaHoraInicio(inicio);
        cita.setFechaHoraFin(inicio.plusMinutes(30));
        cita.setDuracionMinutos(30);
        cita.setEstado(estado);
        cita.setTotalServicio(new BigDecimal("100.00"));
        cita.setMontoPagado(BigDecimal.ZERO);
        cita.setEliminada(eliminada);
        cita.setEsEmergencia(false);
        return citaRepository.save(cita);
    }

    private Usuario usuario(Company company, String nombre) {
        Usuario usuario = new Usuario();
        usuario.setEmail(nombre + "-" + UUID.randomUUID() + "@patitas.test");
        usuario.setUsername(nombre + "-" + UUID.randomUUID());
        usuario.setNombre(nombre);
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
