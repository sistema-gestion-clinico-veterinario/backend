package veterinaria.vargasvet.service.horarios;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.CompanyException;
import veterinaria.vargasvet.domain.enums.DiaSemana;
import veterinaria.vargasvet.dto.request.HorarioEmpleadoRequest;
import veterinaria.vargasvet.repository.CompanyExceptionRepository;
import veterinaria.vargasvet.repository.CompanyOperatingHourRepository;
import veterinaria.vargasvet.repository.HorarioEmpleadoLoteRepository.NuevoHorario;
import veterinaria.vargasvet.repository.HorarioEmpleadoRepository;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class GuardadoDeHorariosDelEmpleado {

    private final HorarioEmpleadoRepository horarioEmpleadoRepository;
    private final CompanyOperatingHourRepository companyOperatingHourRepository;
    private final CompanyExceptionRepository companyExceptionRepository;

    public void agregar(Long empleadoId, Integer companyId, List<HorarioEmpleadoRequest> pedidos, String creadoPor) {
        horarioEmpleadoRepository.insertarEnLote(validarYConvertir(empleadoId, companyId, pedidos, creadoPor));
    }

    public void reemplazar(Long empleadoId, Integer companyId, List<HorarioEmpleadoRequest> pedidos, String creadoPor) {
        List<NuevoHorario> nuevos = validarYConvertir(empleadoId, companyId, pedidos, creadoPor);

        horarioEmpleadoRepository.eliminarPlantillaSemanal(empleadoId);
        nuevos.stream().map(NuevoHorario::fecha).filter(Objects::nonNull).distinct()
                .forEach(fecha -> horarioEmpleadoRepository.eliminarTurnosEntre(empleadoId, fecha, fecha));
        horarioEmpleadoRepository.insertarEnLote(nuevos);
    }

    private List<NuevoHorario> validarYConvertir(Long empleadoId, Integer companyId,
                                                 List<HorarioEmpleadoRequest> pedidos, String creadoPor) {
        List<HorarioEmpleadoRequest> aplicables = pedidos.stream()
                .filter(pedido -> pedido.getHoraInicio() != null && pedido.getHoraFin() != null)
                .filter(pedido -> pedido.getFecha() != null || pedido.getDiaSemana() != null)
                .toList();
        if (aplicables.isEmpty()) {
            return List.of();
        }

        ReglasHorarioEmpresa reglas = new ReglasHorarioEmpresa(
                excepcionesDeLasFechas(companyId, aplicables),
                companyOperatingHourRepository.findByCompanyId(companyId));
        LocalDateTime creadoEn = AppClock.now();

        List<NuevoHorario> nuevos = new ArrayList<>(aplicables.size());
        for (HorarioEmpleadoRequest pedido : aplicables) {
            LocalDate fecha = pedido.getFecha();
            DiaSemana dia;
            if (fecha != null) {
                dia = DiaSemana.desde(fecha.getDayOfWeek());
                reglas.validarTurno(fecha, pedido.getHoraInicio(), pedido.getHoraFin());
            } else {
                dia = pedido.getDiaSemana();
                reglas.validarTurnoSemanal(dia, pedido.getHoraInicio(), pedido.getHoraFin());
            }
            nuevos.add(new NuevoHorario(empleadoId, fecha, dia, pedido.getHoraInicio(), pedido.getHoraFin(),
                    !Boolean.FALSE.equals(pedido.getActivo()), creadoEn, creadoPor));
        }
        return nuevos;
    }

    private List<CompanyException> excepcionesDeLasFechas(
            Integer companyId, List<HorarioEmpleadoRequest> pedidos) {
        List<LocalDate> fechas = pedidos.stream().map(HorarioEmpleadoRequest::getFecha)
                .filter(fecha -> fecha != null).sorted().toList();
        if (fechas.isEmpty()) {
            return List.of();
        }
        return companyExceptionRepository.findByCompanyIdAndDateBetween(companyId, fechas.getFirst(), fechas.getLast());
    }
}
