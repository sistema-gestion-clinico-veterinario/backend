package veterinaria.vargasvet.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RequiredArgsConstructor
class HorarioEmpleadoLoteRepositoryImpl implements HorarioEmpleadoLoteRepository {

    private static final String INSERTAR = "INSERT INTO horario_empleado "
            + "(empleado_id, fecha, dia_semana, hora_inicio, hora_fin, activo, created_at, created_by) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String ELIMINAR_TURNO = "DELETE FROM horario_empleado "
            + "WHERE empleado_id = ? AND fecha = ? AND hora_inicio = ?";

    private static final String ELIMINAR_ENTRE_FECHAS = "DELETE FROM horario_empleado "
            + "WHERE empleado_id = ? AND fecha >= ? AND fecha <= ?";

    private static final String ELIMINAR_PLANTILLA = "DELETE FROM horario_empleado "
            + "WHERE empleado_id = ? AND fecha IS NULL";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void insertarEnLote(List<NuevoHorario> horarios) {
        if (horarios.isEmpty()) return;
        jdbcTemplate.batchUpdate(INSERTAR, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int indice) throws SQLException {
                NuevoHorario horario = horarios.get(indice);
                ps.setObject(1, horario.empleadoId());
                if (horario.fecha() == null) {
                    ps.setNull(2, Types.DATE);
                } else {
                    ps.setObject(2, horario.fecha());
                }
                ps.setString(3, horario.diaSemana().name());
                ps.setObject(4, horario.horaInicio());
                ps.setObject(5, horario.horaFin());
                ps.setBoolean(6, horario.activo());
                ps.setObject(7, horario.creadoEn());
                ps.setString(8, horario.creadoPor());
            }

            @Override
            public int getBatchSize() {
                return horarios.size();
            }
        });
    }

    @Override
    public void eliminarTurno(Long empleadoId, LocalDate fecha, LocalTime horaInicio) {
        jdbcTemplate.update(ELIMINAR_TURNO, empleadoId, fecha, horaInicio);
    }

    @Override
    public void eliminarTurnosEntre(Long empleadoId, LocalDate desde, LocalDate hasta) {
        jdbcTemplate.update(ELIMINAR_ENTRE_FECHAS, empleadoId, desde, hasta);
    }

    @Override
    public void eliminarPlantillaSemanal(Long empleadoId) {
        jdbcTemplate.update(ELIMINAR_PLANTILLA, empleadoId);
    }
}
