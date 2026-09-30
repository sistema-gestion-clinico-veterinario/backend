package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.TipoRelacionMascota;
import veterinaria.vargasvet.validation.MeaningfulText;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDate;

@Data
public class MascotaRelacionRequest {

    @NotNull(message = "Debe seleccionar una persona")
    private Long apoderadoId;

    @NotNull(message = "Debe seleccionar el tipo de relación")
    private TipoRelacionMascota tipoRelacion;

    private Boolean puedeRecibirInformacion = false;
    private Boolean puedeAutorizarAtencion = false;
    private Boolean puedeRealizarPagos = false;
    private LocalDate fechaFin;

    @Size(max = 500, message = "Las observaciones no deben superar 500 caracteres")
    @MeaningfulText(message = "Las observaciones deben contener texto real")
    private String observaciones;

    @AssertTrue(message = "La fecha de fin no puede ser anterior a hoy")
    public boolean isFechaFinValida() {
        return fechaFin == null || !fechaFin.isBefore(AppClock.today());
    }
}
