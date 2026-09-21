package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ReportesComparativoEmpresasDTO {
    private String fechaDesde;
    private String fechaHasta;
    private List<EmpresaResumen> empresas;

    @Data
    @Builder
    public static class EmpresaResumen {
        private Integer companyId;
        private String companyName;
        private long consultas;
        private long pacientesAtendidos;
        private BigDecimal ingresos;
        private long nuevosPacientes;
        private double porcentajeCitasCompletadas;
        private long noAsistieron;
    }
}
