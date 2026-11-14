package veterinaria.vargasvet.service.horarios;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.enums.DiaSemana;
import veterinaria.vargasvet.dto.request.BulkScheduleRequest;
import veterinaria.vargasvet.dto.request.HorarioEmpleadoRequest;
import veterinaria.vargasvet.repository.CompanyExceptionRepository;
import veterinaria.vargasvet.repository.CompanyOperatingHourRepository;
import veterinaria.vargasvet.repository.HorarioEmpleadoLoteRepository.NuevoHorario;
import veterinaria.vargasvet.repository.HorarioEmpleadoRepository;
import veterinaria.vargasvet.repository.HorarioEmpleadoRepository.TurnoOcupado;
import veterinaria.vargasvet.util.AppClock;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AsignacionMasivaDeHorarios {

    private static final long MINUTOS_MINIMOS_DE_REFRIGERIO = 60;

    private final HorarioEmpleadoRepository horarioEmpleadoRepository;
    private final CompanyOperatingHourRepository companyOperatingHourRepository;
    private final CompanyExceptionRepository companyExceptionRepository;

    public void asignar(Long empleadoId, Integer companyId, BulkScheduleRequest request, String creadoPor) {
        LocalDate inicio = request.getStartDate();
        LocalDate fin = request.getEndDate();
        ReglasHorarioEmpresa reglas = new ReglasHorarioEmpresa(
                companyExceptionRepository.findByCompanyIdAndDateBetween(companyId, inicio, fin),
                companyOperatingHourRepository.findByCompanyId(companyId));
        Map<LocalDate, List<TurnoOcupado>> ocupados = horarioEmpleadoRepository
                .findTurnosOcupados(empleadoId, inicio, fin).stream()
                .collect(Collectors.groupingBy(TurnoOcupado::getFecha, HashMap::new, Collectors.toCollection(ArrayList::new)));
        boolean reemplazaTurno = Boolean.TRUE.equals(request.getOverwrite()) && request.getOriginalStartTime() != null;

        List<NuevoHorario> nuevos = new ArrayList<>();
        for (LocalDate dia = inicio; !dia.isAfter(fin); dia = dia.plusDays(1)) {
            if (!reglas.estaAbierta(dia)) {
                continue;
            }
            List<HorarioEmpleadoRequest> turnosDelDia = turnosQueAplicanA(request, dia);
            if (turnosDelDia.isEmpty()) {
                continue;
            }

            List<TurnoOcupado> existentes = ocupados.computeIfAbsent(dia, ignorado -> new ArrayList<>());
            if (reemplazaTurno) {
                LocalTime horaOriginal = request.getOriginalStartTime();
                horarioEmpleadoRepository.eliminarTurno(empleadoId, dia, horaOriginal);
                existentes.removeIf(turno -> turno.getHoraInicio().equals(horaOriginal));
            }

            for (HorarioEmpleadoRequest turno : turnosDelDia) {
                if (seTraslapaConAlguno(existentes, turno)) {
                    throw new IllegalStateException("Conflicto de horario el día " + dia + ": el nuevo rango ("
                            + turno.getHoraInicio() + "-" + turno.getHoraFin() + ") se traslapa con otro turno existente.");
                }
            }
            if (turnosDelDia.size() > 1) {
                validarRefrigerio(turnosDelDia);
            }

            DiaSemana diaSemana = DiaSemana.desde(dia.getDayOfWeek());
            for (HorarioEmpleadoRequest turno : turnosDelDia) {
                reglas.validarTurno(dia, turno.getHoraInicio(), turno.getHoraFin());
                nuevos.add(new NuevoHorario(empleadoId, dia, diaSemana, turno.getHoraInicio(), turno.getHoraFin(),
                        AppClock.now(), creadoPor));
            }
        }
        horarioEmpleadoRepository.insertarEnLote(nuevos);
    }

    private List<HorarioEmpleadoRequest> turnosQueAplicanA(BulkScheduleRequest request, LocalDate dia) {
        DiaSemana diaSemana = DiaSemana.desde(dia.getDayOfWeek());
        return request.getShifts().stream()
                .filter(turno -> turno.getDiaSemana() == null || turno.getDiaSemana().equals(diaSemana))
                .toList();
    }

    private boolean seTraslapaConAlguno(List<TurnoOcupado> existentes, HorarioEmpleadoRequest turno) {
        return existentes.stream().anyMatch(existente ->
                turno.getHoraInicio().isBefore(existente.getHoraFin()) && turno.getHoraFin().isAfter(existente.getHoraInicio()));
    }

    private void validarRefrigerio(List<HorarioEmpleadoRequest> turnos) {
        List<HorarioEmpleadoRequest> ordenados = turnos.stream()
                .sorted(Comparator.comparing(HorarioEmpleadoRequest::getHoraInicio))
                .toList();

        for (int i = 0; i < ordenados.size() - 1; i++) {
            LocalTime finActual = ordenados.get(i).getHoraFin();
            LocalTime inicioSiguiente = ordenados.get(i + 1).getHoraInicio();

            if (inicioSiguiente.isBefore(finActual)) {
                throw new IllegalArgumentException(String.format(
                        "Los turnos no pueden traslaparse o cruzarse (%s - %s con %s - %s)",
                        ordenados.get(i).getHoraInicio(), finActual,
                        inicioSiguiente, ordenados.get(i + 1).getHoraFin()));
            }

            long minutosDeSeparacion = Duration.between(finActual, inicioSiguiente).toMinutes();
            if (minutosDeSeparacion < MINUTOS_MINIMOS_DE_REFRIGERIO) {
                throw new IllegalArgumentException("Debe haber al menos 1 hora de refrigerio entre turnos ("
                        + finActual + " - " + inicioSiguiente + ")");
            }
        }
    }
}
