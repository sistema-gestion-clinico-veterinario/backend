package veterinaria.vargasvet.dto.request;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.DiaSemana;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Data
public class HorarioEmpleadoRequest {

    static class FlexibleLocalTimeDeserializer extends StdDeserializer<LocalTime> {
        FlexibleLocalTimeDeserializer() { super(LocalTime.class); }
        @Override
        public LocalTime deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            String value = p.getText().trim();
            if (value.length() == 5) return LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm"));
            return LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm:ss"));
        }
    }

    private java.time.LocalDate fecha;

    private DiaSemana diaSemana;

    @NotNull(message = "La hora de inicio es obligatoria")
    @JsonDeserialize(using = FlexibleLocalTimeDeserializer.class)
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    @JsonDeserialize(using = FlexibleLocalTimeDeserializer.class)
    private LocalTime horaFin;

    private Boolean activo = true;

    @AssertTrue(message = "Debe indicar una fecha o un dia de semana")
    public boolean isFechaODiaSemanaValido() {
        return fecha != null || diaSemana != null;
    }

    @AssertTrue(message = "La hora de fin debe ser posterior a la hora de inicio")
    public boolean isRangoHorarioValido() {
        return horaInicio == null || horaFin == null || horaFin.isAfter(horaInicio);
    }
}
