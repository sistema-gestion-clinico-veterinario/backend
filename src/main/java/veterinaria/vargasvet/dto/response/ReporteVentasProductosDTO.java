package veterinaria.vargasvet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteVentasProductosDTO {
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private Resumen resumen;
    private List<ItemMonto> ventasPorMetodo;
    private List<ItemMonto> ventasPorDia;
    private List<ProductoVendido> productosMasVendidos;
    private List<AlertaStock> alertasStock;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Resumen {
        private Long ventas;
        private BigDecimal ingresos;
        private Long unidadesVendidas;
        private BigDecimal ticketPromedio;
        private Long productosStockBajo;
        private Long productosSinStock;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ItemMonto {
        private String nombre;
        private Long cantidad;
        private BigDecimal monto;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProductoVendido {
        private String sku;
        private String nombre;
        private Long cantidad;
        private BigDecimal monto;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AlertaStock {
        private String sku;
        private String nombre;
        private String categoria;
        private Integer stock;
        private Integer stockMinimo;
    }
}
