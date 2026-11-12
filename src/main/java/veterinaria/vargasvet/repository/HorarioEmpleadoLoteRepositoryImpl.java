package veterinaria.vargasvet.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RequiredArgsConstructor
class HorarioEmpleadoLoteRepositoryImpl implements HorarioEmpleadoLoteRepository {

    private static final String INSERTAR = "INSERT INTO horario_empleado "
            + "(empleado_id, fecha, dia_semana, hora_inicio, hora_fin, activo, created_at, created_by) "
            + "VALUES (?, ?, ?, ?, ?, TRUE, ?, ?)";

    private static final String ELIMINAR_TURNO = "DELETE FROM horario_empleado "
            + "WHERE empleado_id = ? AND fecha = ? AND hora_inicio = ?";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void insertarEnLote(List<NuevoHorario> horarios) {
        if (horarios.isEmpty()) return;
        jdbcTemplate.batchUpdate(INSERTAR, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int indice) throws SQLException {
                NuevoHorario horario = horarios.get(indice);
                ps.setObject(1, horario.empleadoId());
                ps.setObject(2, horario.fecha());
                ps.setString(3, horario.diaSemana().name());
                ps.setObject(4, horario.horaInicio());
                ps.setObject(5, horario.horaFin());
                ps.setObject(6, horario.creadoEn());
                ps.setString(7, horario.creadoPor());
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
}
