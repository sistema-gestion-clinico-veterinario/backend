package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.IntervaloUnidad;

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

    /** Control pendiente que esta aplicacion completa. */
    private Long controlPreventivoId;

    /** Servicio preventivo configurado (tipo_control_preventivo) para precio y nombre. */
    private Long servicioId;

    /** Obligatorio si el tipo es VACUNACION. */
    private Long tipoVacunaId;

    /** Obligatorio si el tipo es DESPARASITACION (catálogo de desparasitantes). */
    private Long tipoDesparasitanteId;

    /** Obligatorio si el tipo es DESPARASITACION. */
    @Size(max = 100, message = "El producto no debe superar 100 caracteres")
    private String producto;

    @NotNull(message = "La fecha de aplicacion es requerida")
    private LocalDate fechaAplicacion;

    @Min(1) @Max(120)
    private Integer periodicidadMeses;

    /** Intervalo flexible para esquemas expresados en dias, semanas o meses. */
    @Min(1) @Max(3650)
    private Integer intervaloCantidad;

    private IntervaloUnidad intervaloUnidad;

    private LocalDate fechaProxima;

    /** Permite registrar una última dosis sin generar un control posterior. */
    private Boolean programarProximoControl = true;

    @Size(max = 80, message = "El lote no debe superar 80 caracteres")
    private String lote;

    private LocalDate fechaVencimientoProducto;

    @DecimalMin(value = "0.001", message = "La dosis debe ser mayor a cero")
    private BigDecimal dosis;

    @Size(max = 30, message = "La unidad de dosis no debe superar 30 caracteres")
    private String unidadDosis;

    @Size(max = 50, message = "La via de administracion no debe superar 50 caracteres")
    private String viaAdministracion;

    @Size(max = 100, message = "El sitio de aplicacion no debe superar 100 caracteres")
    private String sitioAplicacion;

    @DecimalMin(value = "0.01", message = "El peso debe ser mayor a cero")
    private BigDecimal pesoKg;

    @Size(max = 500, message = "Las observaciones no deben superar 500 caracteres")
    private String observaciones;

    /** Solo informativo; se toma del servicio si no llega. */
    private BigDecimal total;
}
