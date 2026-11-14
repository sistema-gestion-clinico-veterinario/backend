package veterinaria.vargasvet.mapper;

import veterinaria.vargasvet.dto.response.HorarioEmpleadoResponse;
import veterinaria.vargasvet.repository.HorarioDeEmpleado;

import java.util.List;

public final class HorarioEmpleadoMapper {

    private HorarioEmpleadoMapper() {
    }

    public static HorarioEmpleadoResponse aRespuesta(HorarioDeEmpleado horario) {
        HorarioEmpleadoResponse respuesta = new HorarioEmpleadoResponse();
        respuesta.setId(horario.id());
        respuesta.setFecha(horario.fecha());
        respuesta.setDiaSemana(horario.diaSemana().name());
        respuesta.setHoraInicio(horario.horaInicio());
        respuesta.setHoraFin(horario.horaFin());
        respuesta.setActivo(horario.activo());
        return respuesta;
    }

    public static List<HorarioEmpleadoResponse> aRespuestas(List<HorarioDeEmpleado> horarios) {
        return horarios.stream().map(HorarioEmpleadoMapper::aRespuesta).toList();
    }
}
