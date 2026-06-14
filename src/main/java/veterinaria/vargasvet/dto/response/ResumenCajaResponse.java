package veterinaria.vargasvet.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResumenCajaResponse {

    private BigDecimal totalIngresos;
    private BigDecimal totalEgresos;
    private BigDecimal totalDevoluciones;
    private BigDecimal saldo;
}
