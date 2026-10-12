package veterinaria.vargasvet.dto.request;

import lombok.Builder;
import lombok.Value;
import veterinaria.vargasvet.domain.enums.EstadoStockReporte;
import veterinaria.vargasvet.domain.enums.MetodoPago;

import java.time.LocalDate;

/** Filtros del reporte operativo. El servicio siempre reemplaza companyId por la empresa
 * de la sesión, salvo para el administrador de plataforma. */
@Value
@Builder
public class ReporteVentasProductosFiltro {
    Integer companyId;
    LocalDate fechaDesde;
    LocalDate fechaHasta;
    MetodoPago metodoPago;
    Long categoriaId;
    Long productoId;
    Long marcaId;
    EstadoStockReporte estadoStock;
}
