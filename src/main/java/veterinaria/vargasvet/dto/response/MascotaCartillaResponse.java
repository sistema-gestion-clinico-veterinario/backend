package veterinaria.vargasvet.dto.response;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MascotaCartillaResponse {
    private Long id;
    private String nombreCompleto;
    private String especie;
    private String razaNombre;
    private String apoderadoNombreCompleto;
    private String apoderadoTelefono;
    private Long apoderadoId;
    private Boolean activo;
    private LocalDate fechaUltimaAplicacion;
    private String controlPendienteNombre;
    private String controlPendienteTipo;
    private LocalDate controlPendienteFecha;
    private String controlPendienteEstado;
    private Integer controlPendienteDiasRestantes;
    private String controlPendienteResumen;
    private Long controlPendienteId;
}
