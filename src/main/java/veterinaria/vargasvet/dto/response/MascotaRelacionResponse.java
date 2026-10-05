package veterinaria.vargasvet.dto.response;

import lombok.Data;
import veterinaria.vargasvet.domain.enums.TipoRelacionMascota;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MascotaRelacionResponse {
    private String uuid;
    private String mascotaUuid;
    private String mascotaNombre;
    private Long apoderadoId;
    private String personaNombre;
    private String numeroDocumento;
    private TipoRelacionMascota tipoRelacion;
    private Boolean puedeRecibirInformacion;
    private Boolean puedeAutorizarAtencion;
    private Boolean puedeRealizarPagos;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String observaciones;
    private Boolean activo;
    private Boolean porEmpezar;
    private Boolean cuentaActivada;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private String avisoMascota;
}
