package veterinaria.vargasvet.service.reportes;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.ControlPreventivo;
import veterinaria.vargasvet.domain.enums.EstadoControlPreventivo;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;
import veterinaria.vargasvet.dto.response.ReportesClinicosDTO;
import veterinaria.vargasvet.repository.ControlPreventivoRepository;
import veterinaria.vargasvet.repository.RegistroDesparasitacionRepository;
import veterinaria.vargasvet.repository.RegistroVacunaRepository;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static veterinaria.vargasvet.service.reportes.EtiquetasReporte.contarPor;
import static veterinaria.vargasvet.service.reportes.EtiquetasReporte.item;

@Component
@RequiredArgsConstructor
public class ReportePreventivos {

    private static final int DIAS_VENTANA_PROXIMAS = 30;
    private static final int MAXIMO_PROXIMAS = 5;
    private static final String SIN_ESPECIFICAR = "Sin especificar";

    private final RegistroVacunaRepository registroVacunaRepository;
    private final RegistroDesparasitacionRepository registroDesparasitacionRepository;
    private final ControlPreventivoRepository controlPreventivoRepository;

    public List<ReportesClinicosDTO.ItemCount> cumplimiento(Integer companyId, TipoControlPreventivo tipo) {
        List<EstadoControlPreventivo> estadosRelevantes = List.of(
                EstadoControlPreventivo.PROGRAMADO, EstadoControlPreventivo.PROXIMO,
                EstadoControlPreventivo.PENDIENTE, EstadoControlPreventivo.ATRASADO);

        List<ControlPreventivo> controles = controlPreventivoRepository
                .findPendientesByCompany(companyId, estadosRelevantes).stream()
                .filter(cp -> cp.getTipo() == tipo)
                .toList();

        Set<Long> mascotasAtrasadas = controles.stream()
                .filter(cp -> cp.getEstado() == EstadoControlPreventivo.ATRASADO)
                .map(cp -> cp.getMascota().getId())
                .collect(Collectors.toSet());
        Set<Long> mascotasConControl = controles.stream()
                .map(cp -> cp.getMascota().getId())
                .collect(Collectors.toCollection(HashSet::new));

        long alDia = (long) mascotasConControl.size() - mascotasAtrasadas.size();
        long atrasados = mascotasAtrasadas.size();

        List<ReportesClinicosDTO.ItemCount> resultado = new ArrayList<>();
        if (alDia > 0) resultado.add(item("Al día", alDia));
        if (atrasados > 0) resultado.add(item("Atrasado", atrasados));
        return resultado;
    }

    public List<ReportesClinicosDTO.ProximaAplicacion> proximasVacunas(Integer companyId) {
        LocalDate hoy = AppClock.today();
        return registroVacunaRepository.findProximasVacunas(
                        companyId, hoy, hoy.plusDays(DIAS_VENTANA_PROXIMAS), PageRequest.of(0, MAXIMO_PROXIMAS)).stream()
                .map(r -> ReportesClinicosDTO.ProximaAplicacion.builder()
                        .mascota(r.getHistoriaClinica().getMascota().getNombreCompleto())
                        .producto(r.getNombreVacuna())
                        .fechaProxima(r.getFechaProximaDosis().toString())
                        .tipoControl("VACUNACION")
                        .build())
                .toList();
    }

    public List<ReportesClinicosDTO.ProximaAplicacion> proximasDesparasitaciones(Integer companyId) {
        LocalDate hoy = AppClock.today();
        return registroDesparasitacionRepository.findProximasDesparasitaciones(
                        companyId, hoy, hoy.plusDays(DIAS_VENTANA_PROXIMAS), PageRequest.of(0, MAXIMO_PROXIMAS)).stream()
                .map(r -> ReportesClinicosDTO.ProximaAplicacion.builder()
                        .mascota(r.getHistoriaClinica().getMascota().getNombreCompleto())
                        .producto(r.getProducto())
                        .fechaProxima(r.getFechaProximaAplicacion().toString())
                        .tipoControl("DESPARASITACION")
                        .build())
                .toList();
    }

    public List<ReportesClinicosDTO.ProximaAplicacion> controlesProximos(Integer companyId) {
        LocalDate hoy = AppClock.today();
        List<EstadoControlPreventivo> estados = List.of(
                EstadoControlPreventivo.PROGRAMADO,
                EstadoControlPreventivo.PROXIMO,
                EstadoControlPreventivo.PENDIENTE);
        return controlPreventivoRepository.findProximosByCompany(
                        companyId, hoy, hoy.plusDays(DIAS_VENTANA_PROXIMAS), estados, PageRequest.of(0, MAXIMO_PROXIMAS)).stream()
                .map(cp -> ReportesClinicosDTO.ProximaAplicacion.builder()
                        .mascota(cp.getMascota().getNombreCompleto())
                        .producto(cp.getNombreControl())
                        .fechaProxima(cp.getFechaRecomendada().toString())
                        .tipoControl(cp.getTipo().name())
                        .build())
                .toList();
    }

    public List<ReportesClinicosDTO.ItemCount> vacunasMasAplicadas(Integer companyId, RangoDeReporte rango) {
        return contarPor(
                registroVacunaRepository.findAplicadasByCompanyAndFecha(companyId, rango.desde(), rango.hasta()),
                r -> r.getNombreVacuna() != null ? r.getNombreVacuna() : SIN_ESPECIFICAR);
    }

    public List<ReportesClinicosDTO.ItemCount> desparasitantesMasAplicados(Integer companyId, RangoDeReporte rango) {
        return contarPor(
                registroDesparasitacionRepository.findAplicadasByCompanyAndFecha(companyId, rango.desde(), rango.hasta()),
                r -> r.getProducto() != null ? r.getProducto() : SIN_ESPECIFICAR);
    }
}
