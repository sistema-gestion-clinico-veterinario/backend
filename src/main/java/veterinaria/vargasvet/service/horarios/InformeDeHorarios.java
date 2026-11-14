package veterinaria.vargasvet.service.horarios;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.dto.response.EmployeeScheduleReportResponse;
import veterinaria.vargasvet.dto.response.HorarioEmpleadoResponse;
import veterinaria.vargasvet.mapper.HorarioEmpleadoMapper;
import veterinaria.vargasvet.repository.EmpleadoDelInforme;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.HorarioDeEmpleado;
import veterinaria.vargasvet.repository.HorarioEmpleadoRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InformeDeHorarios {

    private final EmpleadoRepository empleadoRepository;
    private final HorarioEmpleadoRepository horarioEmpleadoRepository;

    public List<EmployeeScheduleReportResponse> completo(Integer companyId) {
        List<EmpleadoDelInforme> empleados = empleadoRepository.findEmpleadosDelInformeDeHorarios(companyId);
        if (empleados.isEmpty()) {
            return List.of();
        }
        Map<Long, List<HorarioEmpleadoResponse>> horariosPorEmpleado = horarioEmpleadoRepository
                .findHorariosDeLaEmpresa(companyId).stream()
                .collect(Collectors.groupingBy(
                        HorarioDeEmpleado::empleadoId,
                        LinkedHashMap::new,
                        Collectors.mapping(HorarioEmpleadoMapper::aRespuesta, Collectors.toList())));

        return empleados.stream()
                .map(empleado -> EmployeeScheduleReportResponse.builder()
                        .empleadoId(empleado.empleadoId())
                        .nombreCompleto(empleado.nombreCompleto())
                        .cargo(empleado.cargo())
                        .horarios(horariosPorEmpleado.getOrDefault(empleado.empleadoId(), List.of()))
                        .build())
                .toList();
    }
}
