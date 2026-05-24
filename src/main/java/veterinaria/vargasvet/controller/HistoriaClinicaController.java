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
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.service.HistoriaClinicaService;
import veterinaria.vargasvet.service.AuditLogService;

import java.time.LocalDate;

@RestController
@RequestMapping("/medical-records")
@RequiredArgsConstructor
public class HistoriaClinicaController {

    private final HistoriaClinicaService historiaClinicaService;
    private final AuditLogService auditLogService;
    private final AccesoValidator accesoValidator;

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

        accesoValidator.validarLeer("VISTA_HISTORIAS");
        Page<HistoriaClinicaListResponse> resultado = historiaClinicaService.buscar(
                numeroHc, nombrePaciente, nombrePropietario, fechaDesde, fechaHasta, companyId, page, size);

        auditLogService.log(companyId, "CONSULTAR_HISTORIAS_CLINICAS", "Historias Clínicas", "Consultó el listado de historias clínicas.");
        String mensaje = resultado.isEmpty() ? "No se encontraron historias clínicas" : "Historias clínicas recuperadas con éxito";
        return ResponseEntity.ok(new ApiResponse<>(true, mensaje, resultado));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLINICAL_RECORD_READ')")
    public ResponseEntity<ApiResponse<HistoriaClinicaDetalleResponse>> getDetalle(@PathVariable Long id) {
        accesoValidator.validarLeer("VISTA_HISTORIAS");
        HistoriaClinicaDetalleResponse detalle = historiaClinicaService.getDetalle(id);
        auditLogService.log("CONSULTAR_DETALLE_HISTORIA_CLINICA", "Historias Clínicas", "Consultó el detalle de la historia clínica con ID: " + id + ".");
        return ResponseEntity.ok(new ApiResponse<>(true, "Historia clínica recuperada con éxito", detalle));
    }

    @GetMapping("/pet/{petId}")
    @PreAuthorize("hasAuthority('CLINICAL_RECORD_READ')")
    public ResponseEntity<ApiResponse<HistoriaClinicaDetalleResponse>> getPorMascota(@PathVariable("petId") Long mascotaId) {
        accesoValidator.validarLeer("VISTA_HISTORIAS");
        HistoriaClinicaDetalleResponse detalle = historiaClinicaService.getPorMascota(mascotaId);
        auditLogService.log("CONSULTAR_HISTORIA_CLINICA_MASCOTA", "Historias Clínicas", "Consultó la historia clínica de la mascota con ID: " + mascotaId + ".");
        return ResponseEntity.ok(new ApiResponse<>(true, "Historia clínica recuperada con éxito", detalle));
    }
}
