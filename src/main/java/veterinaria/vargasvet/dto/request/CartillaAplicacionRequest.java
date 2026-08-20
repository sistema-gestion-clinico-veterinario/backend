package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Solicitud para registrar una aplicacion preventiva (vacunacion o desparasitacion)
 * a traves de la cartilla, SIN vincularla a una cita medica ni a una ficha de consulta.
 * El cobro se genera internamente como una cita-cobro para que aparezca en Caja.
 */
@Data
public class CartillaAplicacionRequest {

    @NotNull(message = "Debe seleccionar la mascota")
    private Long mascotaId;

    /** Veterinario que aplica. Si no se envía, se usa el usuario autenticado. */
    private Long empleadoId;

    /** Servicio preventivo configurado (tipo_control_preventivo) para precio y nombre. */
    @NotNull(message = "Debe seleccionar el servicio preventivo")
    private Long servicioId;

    /** Obligatorio si el tipo es VACUNACION. */
    private Long tipoVacunaId;

    /** Obligatorio si el tipo es DESPARASITACION. */
    @Size(max = 100, message = "El producto no debe superar 100 caracteres")
    private String producto;

    @NotNull(message = "La fecha de aplicacion es requerida")
    private LocalDate fechaAplicacion;

    @NotNull(message = "La periodicidad es requerida")
    @Min(1) @Max(120)
    private Integer periodicidadMeses;

    private LocalDate fechaProxima;

    /** Solo informativo; se toma del servicio si no llega. */
    private BigDecimal total;
}