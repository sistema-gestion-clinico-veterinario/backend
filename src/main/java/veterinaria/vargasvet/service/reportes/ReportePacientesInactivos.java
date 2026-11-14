package veterinaria.vargasvet.service.reportes;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.dto.response.PacientesInactivosPageDTO;
import veterinaria.vargasvet.dto.response.ReportesClinicosDTO;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReportePacientesInactivos {

    private static final int MESES_SIN_VISITA_PARA_INACTIVIDAD = 3;
    private static final int TAMANO_MAXIMO_PAGINA = 100;

    private final MascotaRepository mascotaRepository;
    private final CitaRepository citaRepository;
    private final AlcanceReportes alcance;

    public List<ReportesClinicosDTO.PacienteInactivo> listaCompleta(Integer companyId, LocalDate hoy) {
        return mascotaRepository.findInactivasParaReporte(companyId, umbralDeInactividad(hoy)).stream()
                .map(fila -> construir(
                        fila.getMascota(),
                        fila.getNombreApoderado() + " " + fila.getApellidoApoderado(),
                        fila.getUltimaVisita(),
                        fila.getCreadoEn(),
                        hoy))
                .sorted(Comparator.comparingLong(ReportesClinicosDTO.PacienteInactivo::getDiasSinVisitar).reversed())
                .toList();
    }

    public PacientesInactivosPageDTO pagina(Integer companyId, int page, int size) {
        Integer empresa = alcance.resolverEmpresa(companyId);
        if (empresa == null) {
            return PacientesInactivosPageDTO.builder()
                    .content(List.of()).page(page).size(size).totalElements(0).totalPages(0).build();
        }
        int tamano = Math.max(1, Math.min(size, TAMANO_MAXIMO_PAGINA));
        LocalDate hoy = AppClock.today();
        Long veterinarioId = alcance.tieneAlcanceEmpresa() ? null : alcance.resolverVeterinario(null);

        Page<Mascota> paginaMascotas = mascotaRepository.findInactivasByCompanyId(
                empresa, veterinarioId, umbralDeInactividad(hoy), PageRequest.of(Math.max(0, page), tamano));

        Map<Long, LocalDateTime> ultimaVisitaPorMascota = ultimasVisitasDe(paginaMascotas.getContent());

        List<ReportesClinicosDTO.PacienteInactivo> contenido = paginaMascotas.getContent().stream()
                .map(m -> construir(
                        m.getNombreCompleto(),
                        nombreDelPropietario(m),
                        ultimaVisitaPorMascota.get(m.getId()),
                        m.getCreatedAt(),
                        hoy))
                .toList();

        return PacientesInactivosPageDTO.builder()
                .content(contenido)
                .page(paginaMascotas.getNumber())
                .size(paginaMascotas.getSize())
                .totalElements(paginaMascotas.getTotalElements())
                .totalPages(paginaMascotas.getTotalPages())
                .build();
    }

    private Map<Long, LocalDateTime> ultimasVisitasDe(List<Mascota> mascotas) {
        if (mascotas.isEmpty()) return Map.of();
        List<Long> ids = mascotas.stream().map(Mascota::getId).toList();
        return citaRepository.findUltimaVisitaCompletadaByMascotaIds(ids).stream()
                .collect(Collectors.toMap(
                        CitaRepository.UltimaVisitaPorMascota::getMascotaId,
                        CitaRepository.UltimaVisitaPorMascota::getFecha));
    }

    private String nombreDelPropietario(Mascota mascota) {
        if (mascota.getApoderado() == null || mascota.getApoderado().getUser() == null) {
            return "Sin propietario";
        }
        return mascota.getApoderado().getUser().getNombre() + " " + mascota.getApoderado().getUser().getApellido();
    }

    private LocalDateTime umbralDeInactividad(LocalDate hoy) {
        return hoy.minusMonths(MESES_SIN_VISITA_PARA_INACTIVIDAD).atStartOfDay();
    }

    private ReportesClinicosDTO.PacienteInactivo construir(
            String mascota, String apoderado, LocalDateTime ultimaVisita, LocalDateTime registradaEn, LocalDate hoy) {
        LocalDate ultimaFecha = ultimaVisita != null ? ultimaVisita.toLocalDate() : null;
        LocalDate referencia = ultimaFecha != null ? ultimaFecha
                : (registradaEn != null ? registradaEn.toLocalDate() : hoy);
        return ReportesClinicosDTO.PacienteInactivo.builder()
                .mascota(mascota)
                .apoderado(apoderado)
                .ultimaVisita(ultimaFecha != null ? ultimaFecha.toString() : "Nunca visitó")
                .diasSinVisitar(ChronoUnit.DAYS.between(referencia, hoy))
                .build();
    }
}
