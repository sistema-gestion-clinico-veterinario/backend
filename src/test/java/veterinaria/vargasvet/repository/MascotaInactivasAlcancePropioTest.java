package veterinaria.vargasvet.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MascotaInactivasAlcancePropioTest {

    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private ServiciosVeterinariosRepository serviciosRepository;

    private Company clinica;
    private Empleado ana;
    private Empleado beto;
    private Mascota atendidaPorAna;
    private Mascota atendidaPorBeto;
    private Mascota soloConCitaPendienteDeAna;
    private ServiciosVeterinarios consulta;
    private Apoderado cliente;

    @BeforeEach
    void datos() {
        clinica = new Company();
        clinica.setName("Clínica Patitas");
        clinica.setSlug("patitas-" + UUID.randomUUID());
        clinica.setRuc(digitos(11));
        clinica.setActivo(true);
        clinica = companyRepository.save(clinica);

        cliente = new Apoderado();
        cliente.setUser(usuario("cliente"));
        cliente.setCompany(clinica);
        cliente.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        cliente.setNumeroDocumento(digitos(8));
        cliente.setGenero(Genero.FEMENINO);
        cliente = apoderadoRepository.save(cliente);

        ana = empleado("ana");
        beto = empleado("beto");

        consulta = new ServiciosVeterinarios();
        consulta.setCompany(clinica);
        consulta.setNombre("Consulta general");
        consulta.setDescripcion("Consulta veterinaria general");
        consulta.setPrecio(new BigDecimal("100.00"));
        consulta.setDisponible(true);
        consulta.setActivo(true);
        consulta.setDuracionEstimada(30);
        consulta.setPermiteEmergencia(false);
        consulta = serviciosRepository.save(consulta);

        atendidaPorAna = mascota("Luna");
        atendidaPorBeto = mascota("Rocky");
        soloConCitaPendienteDeAna = mascota("Nala");

        cita(atendidaPorAna, ana, EstadoCita.COMPLETADA, 40);
        cita(atendidaPorBeto, beto, EstadoCita.COMPLETADA, 30);
        cita(soloConCitaPendienteDeAna, ana, EstadoCita.PROGRAMADA, 20);
    }

    private java.util.List<String> inactivas(Long veterinarioId) {
        return mascotaRepository.findInactivasByCompanyId(
                        clinica.getId(), veterinarioId, LocalDateTime.now().plusDays(1), PageRequest.of(0, 20))
                .getContent().stream().map(Mascota::getNombreCompleto).toList();
    }

    @Test
    void sinFiltroDeProfesionalLaClinicaVeTodosLosPacientesInactivos() {
        assertThat(inactivas(null)).containsExactlyInAnyOrder("Luna", "Rocky", "Nala");
    }

    @Test
    void conAlcancePropioCadaProfesionalSoloVeALosQueAtendio() {
        assertThat(inactivas(ana.getId())).containsExactly("Luna");
        assertThat(inactivas(beto.getId())).containsExactly("Rocky");
    }

    @Test
    void unaCitaPendienteNoCuentaComoHaberAtendidoAlPaciente() {
        assertThat(inactivas(ana.getId())).doesNotContain("Nala");
    }

    @Test
    void sinFichaDeEmpleadoElFiltroNoDevuelveNingunPaciente() {
        assertThat(inactivas(-1L)).isEmpty();
    }

    @Test
    void elTotalPaginadoRespetaElMismoFiltro() {
        long total = mascotaRepository.findInactivasByCompanyId(
                clinica.getId(), ana.getId(), LocalDateTime.now().plusDays(1), PageRequest.of(0, 20))
                .getTotalElements();

        assertThat(total).isEqualTo(1);
    }

    private Mascota mascota(String nombre) {
        Mascota mascota = new Mascota();
        mascota.setNombreCompleto(nombre);
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(cliente);
        mascota.setUuid(UUID.randomUUID().toString());
        return mascotaRepository.save(mascota);
    }

    private Empleado empleado(String prefijo) {
        Empleado empleado = new Empleado();
        empleado.setUser(usuario(prefijo));
        empleado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        empleado.setNumeroDocumentoIdentidad(digitos(8));
        empleado.setGenero(Genero.MASCULINO);
        empleado.setEstado(true);
        return empleadoRepository.save(empleado);
    }

    private Cita cita(Mascota mascota, Empleado empleado, EstadoCita estado, int diasAtras) {
        LocalDateTime inicio = LocalDateTime.now().minusDays(diasAtras);
        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setEmpleado(empleado);
        cita.setServicio(consulta);
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

    private Usuario usuario(String prefijo) {
        Usuario usuario = new Usuario();
        usuario.setEmail(prefijo + "-" + UUID.randomUUID() + "@patitas.test");
        usuario.setUsername(prefijo + "-" + UUID.randomUUID());
        usuario.setNombre(prefijo);
        usuario.setApellido("Test");
        usuario.setDni(digitos(8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(clinica);
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
