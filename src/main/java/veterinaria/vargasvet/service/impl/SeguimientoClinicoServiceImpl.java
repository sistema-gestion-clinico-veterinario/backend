package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Diagnostico;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Tratamiento;
import veterinaria.vargasvet.dto.response.SugerenciaControlResponse;
import veterinaria.vargasvet.repository.DiagnosticoRepository;
import veterinaria.vargasvet.repository.TratamientoRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.SeguimientoClinicoService;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeguimientoClinicoServiceImpl implements SeguimientoClinicoService {

    private final TratamientoRepository tratamientoRepository;
    private final DiagnosticoRepository diagnosticoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SugerenciaControlResponse> listarSugerenciasControl(Integer companyId, int diasVentana) {
        Integer targetCompanyId = resolveCompanyId(companyId);
        if (targetCompanyId == null) {
            return List.of();
        }

        LocalDate hoy = AppClock.today();
        LocalDate hasta = hoy.plusDays(Math.max(1, diasVentana));

        List<SugerenciaControlResponse> sugerencias = new ArrayList<>();

        for (Tratamiento t : tratamientoRepository.findProximosAFinalizar(targetCompanyId, hoy, hasta)) {
            Mascota mascota = t.getConsulta().getHistoriaClinica().getMascota();
            sugerencias.add(SugerenciaControlResponse.builder()
                    .origen("TRATAMIENTO")
                    .origenId(t.getId())
                    .nombre(t.getNombre())
                    .mascotaId(mascota.getId())
                    .mascotaNombre(mascota.getNombreCompleto())
                    .apoderadoId(mascota.getApoderado() != null ? mascota.getApoderado().getId() : null)
                    .apoderadoNombre(nombreApoderado(mascota))
                    .fechaControl(t.getFechaFin())
                    .diasRestantes(ChronoUnit.DAYS.between(hoy, t.getFechaFin()))
                    .build());
        }

        for (Diagnostico d : diagnosticoRepository.findProximosControles(targetCompanyId, hoy, hasta)) {
            Mascota mascota = d.getConsulta().getHistoriaClinica().getMascota();
            sugerencias.add(SugerenciaControlResponse.builder()
                    .origen("DIAGNOSTICO")
                    .origenId(d.getId())
                    .nombre(d.getNombre())
                    .mascotaId(mascota.getId())
                    .mascotaNombre(mascota.getNombreCompleto())
                    .apoderadoId(mascota.getApoderado() != null ? mascota.getApoderado().getId() : null)
                    .apoderadoNombre(nombreApoderado(mascota))
                    .fechaControl(d.getFechaProximoControl())
                    .diasRestantes(ChronoUnit.DAYS.between(hoy, d.getFechaProximoControl()))
                    .build());
        }

        return sugerencias.stream()
                .sorted(Comparator.comparingLong(SugerenciaControlResponse::getDiasRestantes))
                .toList();
    }

    private String nombreApoderado(Mascota mascota) {
        if (mascota.getApoderado() == null || mascota.getApoderado().getUser() == null) return null;
        return mascota.getApoderado().getUser().getNombre() + " " + mascota.getApoderado().getUser().getApellido();
    }

    private Integer resolveCompanyId(Integer requestedCompanyId) {
        if (SecurityUtils.isSuperAdmin()) {
            return requestedCompanyId;
        }
        return SecurityUtils.getCurrentCompanyId();
    }
}
