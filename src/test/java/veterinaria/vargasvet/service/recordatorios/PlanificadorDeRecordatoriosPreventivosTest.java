package veterinaria.vargasvet.service.recordatorios;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.ControlPreventivo;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EstadoControlPreventivo;
import veterinaria.vargasvet.domain.enums.TipoAvisoRecordatorio;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;
import veterinaria.vargasvet.repository.ControlPreventivoRepository;
import veterinaria.vargasvet.repository.RecordatorioPreventivoRepository;
import veterinaria.vargasvet.service.CompanyComplianceService;
import veterinaria.vargasvet.service.ConsentimientoDatosService;
import veterinaria.vargasvet.service.OwnerContactPolicy;
import veterinaria.vargasvet.service.PetOwnershipService;
import veterinaria.vargasvet.service.recordatorios.AvisosParaPropietario.Aviso;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanificadorDeRecordatoriosPreventivosTest {

    private static final LocalDate HOY = AppClock.today();

    @Mock ControlPreventivoRepository controlRepository;
    @Mock RecordatorioPreventivoRepository recordatorioRepository;
    @Mock PetOwnershipService petOwnershipService;
    @Mock ConsentimientoDatosService consentimientoDatosService;
    @Mock CompanyComplianceService cumplimiento;

    private PlanificadorDeRecordatoriosPreventivos planificador;
    private final Company clinica = clinica();
    private final Map<Long, List<Apoderado>> destinatarios = new LinkedHashMap<>();

    @BeforeEach
    void preparar() {
        lenient().when(cumplimiento.gestionadoPorSoftvet(anyInt(), any())).thenReturn(true);
        planificador = new PlanificadorDeRecordatoriosPreventivos(controlRepository, recordatorioRepository,
                new OwnerContactPolicy(null), petOwnershipService, consentimientoDatosService, cumplimiento);
        lenient().when(recordatorioRepository.findExistingKeys(anyCollection())).thenReturn(List.of());
        lenient().when(recordatorioRepository.findApoderadoIdsWithRecentReminder(anyCollection(), any())).thenReturn(List.of());
        lenient().when(consentimientoDatosService.usuariosQueAutorizaron(anyCollection(), anyInt(), any()))
                .thenAnswer(invocation -> Set.copyOf(invocation.getArgument(0)));
        lenient().when(petOwnershipService.destinatariosDeAvisos(anyCollection())).thenReturn(destinatarios);
    }

    @Test
    void sinControlesCandidatosNoConsultaNiDevuelveNada() {
        when(controlRepository.findReminderCandidates(any(), any())).thenReturn(List.of());

        assertThat(planificador.planificar(HOY)).isEmpty();

        verify(recordatorioRepository, never()).findExistingKeys(anyCollection());
        verify(recordatorioRepository, never()).findApoderadoIdsWithRecentReminder(anyCollection(), any());
    }

    @Test
    void agrupaEnUnSoloPropietarioTodosLosControlesDeSusMascotas() {
        Apoderado ana = apoderado(1L, "Ana", "ana@example.test");
        ControlPreventivo rockyVacuna = control(10L, ana, "Rocky", "Antirrabica", HOY.plusDays(3));
        ControlPreventivo lunaDesparasitacion = control(11L, ana, "Luna", "Desparasitacion", HOY.plusDays(4));
        candidatos(rockyVacuna, lunaDesparasitacion);

        List<AvisosParaPropietario> plan = planificador.planificar(HOY);

        assertThat(plan).hasSize(1);
        AvisosParaPropietario propietario = plan.getFirst();
        assertThat(propietario.apoderadoId()).isEqualTo(1L);
        assertThat(propietario.correo()).isEqualTo("ana@example.test");
        assertThat(propietario.nombre()).isEqualTo("Ana Perez");
        assertThat(propietario.clinica().nombre()).isEqualTo("Patitas Felices");
        assertThat(propietario.avisos()).extracting(Aviso::mascota).containsExactly("Rocky", "Luna");
    }

    @Test
    void ordenaLosAvisosPrimeroLosAtrasadosLuegoLosDeHoyYLuegoLosProximosPorFecha() {
        Apoderado ana = apoderado(1L, "Ana", "ana@example.test");
        candidatos(control(10L, ana, "Rocky", "Proximo lejano", HOY.plusDays(6)),
                control(11L, ana, "Luna", "Pendiente de hoy", HOY),
                control(12L, ana, "Nube", "Proximo cercano", HOY.plusDays(2)),
                control(13L, ana, "Toby", "Atrasado", HOY.minusDays(4)));

        List<Aviso> avisos = planificador.planificar(HOY).getFirst().avisos();

        assertThat(avisos).extracting(Aviso::control)
                .containsExactly("Atrasado", "Pendiente de hoy", "Proximo cercano", "Proximo lejano");
        assertThat(avisos).extracting(Aviso::tipoAviso).containsExactly(TipoAvisoRecordatorio.ATRASADO,
                TipoAvisoRecordatorio.PENDIENTE, TipoAvisoRecordatorio.PROXIMO, TipoAvisoRecordatorio.PROXIMO);
    }

    @Test
    void separaLosAvisosDePropietariosDistintos() {
        Apoderado ana = apoderado(1L, "Ana", "ana@example.test");
        Apoderado beto = apoderado(2L, "Beto", "beto@example.test");
        candidatos(control(10L, ana, "Rocky", "Antirrabica", HOY.plusDays(3)),
                control(11L, beto, "Luna", "Antirrabica", HOY.plusDays(3)));

        List<AvisosParaPropietario> plan = planificador.planificar(HOY);

        assertThat(plan).extracting(AvisosParaPropietario::apoderadoId).containsExactly(1L, 2L);
        assertThat(plan).allSatisfy(propietario -> assertThat(propietario.avisos()).hasSize(1));
    }

    @Test
    void actualizaElEstadoDeLosControlesSegunLaFecha() {
        Apoderado ana = apoderado(1L, "Ana", "ana@example.test");
        ControlPreventivo atrasado = control(10L, ana, "Rocky", "Atrasado", HOY.minusDays(1));
        ControlPreventivo deHoy = control(11L, ana, "Luna", "De hoy", HOY);
        ControlPreventivo proximo = control(12L, ana, "Nube", "Proximo", HOY.plusDays(5));
        ControlPreventivo lejano = control(13L, ana, "Toby", "Lejano", HOY.plusDays(20));
        for (ControlPreventivo control : List.of(atrasado, deHoy, proximo, lejano)) {
            control.setEstado(EstadoControlPreventivo.PROGRAMADO);
        }
        candidatos(atrasado, deHoy, proximo, lejano);

        List<AvisosParaPropietario> plan = planificador.planificar(HOY);

        assertThat(atrasado.getEstado()).isEqualTo(EstadoControlPreventivo.ATRASADO);
        assertThat(deHoy.getEstado()).isEqualTo(EstadoControlPreventivo.PENDIENTE);
        assertThat(proximo.getEstado()).isEqualTo(EstadoControlPreventivo.PROXIMO);
        assertThat(lejano.getEstado()).isEqualTo(EstadoControlPreventivo.PROGRAMADO);
        assertThat(atrasado.getEstadoModificadoPor()).isEqualTo("SYSTEM");
        assertThat(lejano.getEstadoModificadoPor()).isNull();
        assertThat(plan.getFirst().avisos()).extracting(Aviso::control).doesNotContain("Lejano");
    }

    @Test
    void sinClinicaElPropietarioViajaSinDatosDeClinica() {
        Apoderado ana = apoderado(1L, "Ana", "ana@example.test");
        ana.getUser().setCompany(null);
        candidatos(control(10L, ana, "Rocky", "Antirrabica", HOY.plusDays(3)));

        assertThat(planificador.planificar(HOY).getFirst().clinica()).isNull();
    }

    @Test
    void noPlanificaAUnPropietarioInactivo() {
        Apoderado ana = apoderado(1L, "Ana", "ana@example.test");
        ana.getUser().setActivo(false);
        candidatos(control(10L, ana, "Rocky", "Antirrabica", HOY.plusDays(3)));

        assertThat(planificador.planificar(HOY)).isEmpty();
    }

    private void candidatos(ControlPreventivo... controles) {
        when(controlRepository.findReminderCandidates(any(), any())).thenReturn(List.of(controles));
        for (ControlPreventivo control : controles) {
            destinatarios.computeIfAbsent(control.getMascota().getId(), id -> new ArrayList<>())
                    .add(control.getMascota().getApoderado());
        }
    }

    private Company clinica() {
        Company company = new Company();
        company.setId(7);
        company.setName("Patitas Felices");
        company.setEmail("hola@patitas.test");
        return company;
    }

    private Apoderado apoderado(Long id, String nombre, String correo) {
        Usuario usuario = new Usuario();
        usuario.setId(Math.toIntExact(id));
        usuario.setNombre(nombre);
        usuario.setApellido("Perez");
        usuario.setEmail(correo);
        usuario.setEmailVerified(true);
        usuario.setActivo(true);
        usuario.setCompany(clinica);
        Apoderado apoderado = new Apoderado();
        apoderado.setId(id);
        apoderado.setUser(usuario);
        apoderado.setCompany(clinica);
        return apoderado;
    }

    private long siguienteMascota = 100;

    private ControlPreventivo control(Long id, Apoderado apoderado, String mascotaNombre, String nombre, LocalDate fecha) {
        Mascota mascota = new Mascota();
        mascota.setId(siguienteMascota++);
        mascota.setNombreCompleto(mascotaNombre);
        mascota.setApoderado(apoderado);
        ControlPreventivo control = new ControlPreventivo();
        control.setId(id);
        control.setMascota(mascota);
        control.setNombreControl(nombre);
        control.setTipo(TipoControlPreventivo.VACUNACION);
        control.setFechaRecomendada(fecha);
        control.setEstado(EstadoControlPreventivo.PROXIMO);
        return control;
    }
}
