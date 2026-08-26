package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class RecordatorioWhatsAppResponse {
    private Long controlId;
    private String mascotaNombre;
    private Long apoderadoId;
    private String apoderadoNombre;
    private String apoderadoTelefono;
    private String tipoControl;
    private String nombreControl;
    private LocalDate fechaRecomendada;
    private String estado;
    private int diasRestantes;
    private String resumenDias;
    private String mensajeWhatsApp;
}
