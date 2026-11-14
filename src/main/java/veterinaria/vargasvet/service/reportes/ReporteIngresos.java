package veterinaria.vargasvet.service.reportes;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Purchase;
import veterinaria.vargasvet.dto.response.ReportesClinicosDTO;
import veterinaria.vargasvet.repository.PurchaseRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static veterinaria.vargasvet.service.reportes.EtiquetasReporte.humanizar;

@Component
@RequiredArgsConstructor
public class ReporteIngresos {

    private final PurchaseRepository purchaseRepository;

    public List<ReportesClinicosDTO.ItemMonto> porMetodoDePago(List<Cita> citas) {
        return sumarPorEtiqueta(citas, pago -> pago.getMetodoPago() != null
                ? humanizar(pago.getMetodoPago().name())
                : "Sin especificar");
    }

    public List<ReportesClinicosDTO.ItemMonto> porServicio(List<Cita> citas) {
        return sumarPorEtiqueta(citas, pago -> pago.getCita() != null
                ? EtiquetasReporte.servicio(pago.getCita())
                : "Sin servicio");
    }

    private List<ReportesClinicosDTO.ItemMonto> sumarPorEtiqueta(List<Cita> citas, Function<Purchase, String> etiqueta) {
        Map<String, BigDecimal> montos = new LinkedHashMap<>();
        for (Purchase pago : pagosDe(citas)) {
            montos.merge(etiqueta.apply(pago), montoReal(pago), BigDecimal::add);
        }
        return montos.entrySet().stream()
                .map(e -> itemMonto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(ReportesClinicosDTO.ItemMonto::getMonto).reversed())
                .toList();
    }

    private List<Purchase> pagosDe(List<Cita> citas) {
        List<Long> citaIds = citas.stream().map(Cita::getId).toList();
        if (citaIds.isEmpty()) return List.of();
        return purchaseRepository.findPagadosByCitaIds(citaIds);
    }

    private BigDecimal montoReal(Purchase pago) {
        return pago.getTotal() != null ? pago.getTotal() : BigDecimal.ZERO;
    }

    private ReportesClinicosDTO.ItemMonto itemMonto(String etiqueta, BigDecimal monto) {
        return ReportesClinicosDTO.ItemMonto.builder()
                .label(etiqueta)
                .monto(monto.setScale(2, RoundingMode.HALF_UP))
                .build();
    }
}
