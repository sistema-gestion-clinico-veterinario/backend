package veterinaria.vargasvet.dto.response;

import lombok.Data;
import veterinaria.vargasvet.domain.enums.EstadoSesionCaja;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SesionCajaResponse {
    private Long id;
    private Integer companyId;
    private EstadoSesionCaja estado;
    private BigDecimal montoApertura;
    private BigDecimal efectivoEsperado;
    private BigDecimal efectivoContado;
    private BigDecimal diferencia;
    private LocalDateTime abiertaAt;
    private LocalDateTime cerradaAt;
    private String abiertaPor;
    private String cerradaPor;
    private String observaciones;
}
