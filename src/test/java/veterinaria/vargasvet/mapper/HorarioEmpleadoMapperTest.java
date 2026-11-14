package veterinaria.vargasvet.mapper;

import org.junit.jupiter.api.Test;
import veterinaria.vargasvet.domain.enums.DiaSemana;
import veterinaria.vargasvet.dto.response.HorarioEmpleadoResponse;
import veterinaria.vargasvet.repository.HorarioDeEmpleado;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HorarioEmpleadoMapperTest {

    @Test
    void copiaTodosLosCamposYConvierteElDiaDeLaSemanaATexto() {
        HorarioDeEmpleado horario = new HorarioDeEmpleado(5L, 3L, LocalDate.of(2026, 12, 7), DiaSemana.LUNES,
                LocalTime.of(9, 0), LocalTime.of(13, 0), true);

        HorarioEmpleadoResponse respuesta = HorarioEmpleadoMapper.aRespuesta(horario);

        assertThat(respuesta.getId()).isEqualTo(5L);
        assertThat(respuesta.getFecha()).isEqualTo(LocalDate.of(2026, 12, 7));
        assertThat(respuesta.getDiaSemana()).isEqualTo("LUNES");
        assertThat(respuesta.getHoraInicio()).isEqualTo(LocalTime.of(9, 0));
        assertThat(respuesta.getHoraFin()).isEqualTo(LocalTime.of(13, 0));
        assertThat(respuesta.getActivo()).isTrue();
    }

    @Test
    void unTurnoSinFechaConservaLaFechaNula() {
        HorarioDeEmpleado recurrente = new HorarioDeEmpleado(6L, 3L, null, DiaSemana.MARTES,
                LocalTime.of(10, 0), LocalTime.of(12, 0), false);

        HorarioEmpleadoResponse respuesta = HorarioEmpleadoMapper.aRespuesta(recurrente);

        assertThat(respuesta.getFecha()).isNull();
        assertThat(respuesta.getActivo()).isFalse();
    }

    @Test
    void laListaConservaElOrdenRecibido() {
        List<HorarioDeEmpleado> horarios = List.of(
                new HorarioDeEmpleado(2L, 3L, null, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 0), true),
                new HorarioDeEmpleado(1L, 3L, null, DiaSemana.LUNES, LocalTime.of(7, 0), LocalTime.of(8, 0), true));

        assertThat(HorarioEmpleadoMapper.aRespuestas(horarios)).extracting(HorarioEmpleadoResponse::getId)
                .containsExactly(2L, 1L);
        assertThat(HorarioEmpleadoMapper.aRespuestas(List.of())).isEmpty();
    }
}
