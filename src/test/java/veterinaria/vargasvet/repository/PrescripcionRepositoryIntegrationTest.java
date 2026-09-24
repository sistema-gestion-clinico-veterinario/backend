package veterinaria.vargasvet.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Consulta;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.HistoriaClinica;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Prescripcion;
import veterinaria.vargasvet.domain.entity.ServiciosVeterinarios;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.EstadoConsulta;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cubre el bug real que reporto el usuario: "busque Amoxicilina y no salio nada". La
 * causa era que buscar() filtraba por empresa via usuario.company_id, un campo que
 * CompanyMembershipService.syncLegacyCompanyField deja en null en cuanto un apoderado
 * tiene cero o mas de una membresia activa - la fuente de verdad es apoderado.company_id
 * (ver commit "Fase 3": ~35 consultas se corrigieron para usar el salto directo, esta
 * se quedo afuera). Este test reproduce exactamente ese escenario: Usuario.company nulo,
 * Apoderado.company correcto.
 */
@DataJpaTest
class PrescripcionRepositoryIntegrationTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private ServiciosVeterinariosRepository serviciosVeterinariosRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private HistoriaClinicaRepository historiaClinicaRepository;
    @Autowired private ConsultaRepository consultaRepository;
    @Autowired private PrescripcionRepository prescripcionRepository;

    @Test
    @DisplayName("buscar() encuentra la receta por companyId aunque Usuario.company este en null (cache legacy desactualizada)")
    void buscarEncuentraRecetaAunqueUsuarioCompanySeaNulo() {
        Company company = new Company();
        company.setName("VargasVet Test");
        company.setSlug("vargasvet-test-" + UUID.randomUUID());
        company.setRuc(uniqueDigits(11));
        company.setActivo(true);
        company = companyRepository.save(company);

        Usuario apoderadoUser = usuario("cliente", company);
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(apoderadoUser);
        apoderado.setCompany(company);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setNumeroDocumento(uniqueDigits(8));
        apoderado.setGenero(Genero.FEMENINO);
        apoderado = apoderadoRepository.save(apoderado);

        // Reproduce exactamente lo que syncLegacyCompanyField deja cuando el apoderado
        // tiene cero o varias membresias activas: Usuario.company en null, aunque
        // Apoderado.company (la fuente de verdad real) siga apuntando a la empresa correcta.
        apoderadoUser.setCompany(null);
        usuarioRepository.save(apoderadoUser);

        Mascota mascota = new Mascota();
        mascota.setNombreCompleto("Firulais");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(apoderado);
        mascota.setUuid(UUID.randomUUID().toString());
        mascota = mascotaRepository.save(mascota);

        Usuario empleadoUser = usuario("vet", company);
        Empleado empleado = new Empleado();
        empleado.setUser(empleadoUser);
        empleado.setCompany(company);
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
        cita.setFechaHoraInicio(LocalDateTime.now().plusDays(1));
        cita.setFechaHoraFin(LocalDateTime.now().plusDays(1).plusMinutes(30));
        cita.setDuracionMinutos(30);
        cita.setEstado(EstadoCita.PROGRAMADA);
        cita.setTotalServicio(new BigDecimal("100.00"));
        cita.setMontoPagado(BigDecimal.ZERO);
        cita.setEliminada(false);
        cita.setEsEmergencia(false);
        cita = citaRepository.save(cita);

        HistoriaClinica historia = new HistoriaClinica();
        historia.setMascota(mascota);
        historia.setNumeroHc("HC-" + UUID.randomUUID().toString().substring(0, 8));
        historia.setActiva(true);
        historia = historiaClinicaRepository.save(historia);

        Consulta consulta = new Consulta();
        consulta.setHistoriaClinica(historia);
        consulta.setCita(cita);
        consulta.setVeterinario(empleado);
        consulta.setEstado(EstadoConsulta.ABIERTA);
        consulta.setFechaConsulta(LocalDateTime.now());
        consulta.setMotivoConsulta("Control general");
        consulta.setTipoConsulta(veterinaria.vargasvet.domain.enums.TipoConsulta.CONTROL_RUTINA);
        consulta = consultaRepository.save(consulta);

        Prescripcion prescripcion = new Prescripcion();
        prescripcion.setConsulta(consulta);
        prescripcion.setVeterinario(empleado);
        prescripcion.setMedicamento("Amoxicilina");
        prescripcion.setDosis("Una tableta");
        prescripcion.setFrecuencia("Cada doce horas");
        prescripcion.setDuracionDias(7);
        prescripcion.setFechaInicio(LocalDate.now());
        prescripcion.setCreatedAt(LocalDateTime.now());
        prescripcionRepository.save(prescripcion);

        Page<Prescripcion> resultado = prescripcionRepository.buscar(
                false, company.getId(), "%amoxicilina%",
                null, null, null, null, null,
                LocalDate.of(1, 1, 1), LocalDate.of(9999, 12, 31),
                PageRequest.of(0, 10));

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getMedicamento()).isEqualTo("Amoxicilina");
    }

    private Usuario usuario(String prefix, Company company) {
        Usuario usuario = new Usuario();
        usuario.setEmail(prefix + "-" + UUID.randomUUID() + "@vargasvet.test");
        usuario.setUsername(prefix + "-" + UUID.randomUUID());
        usuario.setNombre(prefix);
        usuario.setApellido("Test");
        usuario.setDni(uniqueDigits(8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(company);
        return usuarioRepository.save(usuario);
    }

    private String uniqueDigits(int length) {
        String digits = String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        while (digits.length() < length) {
            digits += "0";
        }
        return digits.substring(0, length);
    }
}
