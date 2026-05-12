package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.response.HistoriaClinicaDetalleResponse;
import veterinaria.vargasvet.dto.response.HistoriaClinicaListResponse;
import veterinaria.vargasvet.service.HistoriaClinicaService;

import java.time.LocalDate;

@RestController
@RequestMapping("/historias-clinicas")
@RequiredArgsConstructor
public class HistoriaClinicaController {

    private final HistoriaClinicaService historiaClinicaService;

    @GetMapping
    @PreAuthorize("hasAuthority('CLINICAL_RECORD_READ')")
    public ResponseEntity<ApiResponse<Page<HistoriaClinicaListResponse>>> buscar(
            @RequestParam(required = false) String numeroHc,
            @RequestParam(required = false) String nombrePaciente,
            @RequestParam(required = false) String nombrePropietario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) Integer companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<HistoriaClinicaListResponse> resultado = historiaClinicaService.buscar(
                numeroHc, nombrePaciente, nombrePropietario, fechaDesde, fechaHasta, companyId, page, size);

        String mensaje = resultado.isEmpty() ? "No se encontraron historias clínicas" : "Historias clínicas recuperadas con éxito";
        return ResponseEntity.ok(new ApiResponse<>(true, mensaje, resultado));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLINICAL_RECORD_READ')")
    public ResponseEntity<ApiResponse<HistoriaClinicaDetalleResponse>> getDetalle(@PathVariable Long id) {
        HistoriaClinicaDetalleResponse detalle = historiaClinicaService.getDetalle(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Historia clínica recuperada con éxito", detalle));
    }

    @GetMapping("/mascota/{mascotaId}")
    @PreAuthorize("hasAuthority('CLINICAL_RECORD_READ')")
    public ResponseEntity<ApiResponse<HistoriaClinicaDetalleResponse>> getPorMascota(@PathVariable Long mascotaId) {
        HistoriaClinicaDetalleResponse detalle = historiaClinicaService.getPorMascota(mascotaId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Historia clínica recuperada con éxito", detalle));
    }
}
