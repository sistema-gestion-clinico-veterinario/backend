package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.CitaRequest;
import veterinaria.vargasvet.dto.response.*;
import veterinaria.vargasvet.service.ApoderadoPortalService;

import java.util.List;

@RestController
@RequestMapping("/clients/portal")
@RequiredArgsConstructor
public class ApoderadoPortalController {

    private final ApoderadoPortalService apoderadoPortalService;

    @GetMapping("/profile")
    @PreAuthorize("@accesoValidator.can('VISTA_PROFILE', 'LEER')")
    public ResponseEntity<ApiResponse<ApoderadoPerfilResponse>> getPerfil() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Perfil recuperado con éxito", apoderadoPortalService.getPerfil()));
    }

    @GetMapping("/pets")
    @PreAuthorize("@accesoValidator.can('VISTA_MIS_MASCOTAS', 'LEER')")
    public ResponseEntity<ApiResponse<List<MascotaResponse>>> getMascotas() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Mascotas recuperadas con éxito", apoderadoPortalService.getMascotas()));
    }

    @GetMapping("/pets/paginated")
    @PreAuthorize("@accesoValidator.can('VISTA_MIS_MASCOTAS', 'LEER')")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<MascotaResponse>>> getMascotasPaginated(
            @RequestParam(value = "nombre", required = false) String nombre,
            @RequestParam(value = "especie", required = false) veterinaria.vargasvet.domain.enums.EspecieMascota especie,
            @RequestParam(value = "activo", required = false) Boolean activo,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, "Mascotas recuperadas con éxito", apoderadoPortalService.getMascotasPaginated(nombre, especie, activo, pageable)));
    }

    @GetMapping("/pets/{petId}/medical-record")
    @PreAuthorize("@accesoValidator.can('VISTA_MI_HISTORIAL', 'LEER')")
    public ResponseEntity<ApiResponse<HistoriaClinicaDetalleResponse>> getHistoriaMascota(@PathVariable("petId") Long mascotaId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Historial clínico recuperado con éxito", apoderadoPortalService.getHistoriaMascota(mascotaId)));
    }

    @GetMapping("/appointments")
    @PreAuthorize("@accesoValidator.can('VISTA_MIS_CITAS', 'LEER')")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<CitaResponse>>> getCitas(
            @RequestParam(required = false) Long mascotaId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, "Citas recuperadas con éxito", apoderadoPortalService.getCitas(mascotaId, pageable)));
    }

    @GetMapping("/prescriptions")
    @PreAuthorize("@accesoValidator.can('VISTA_MIS_RECETAS', 'LEER')")
    public ResponseEntity<ApiResponse<List<PrescripcionResumenResponse>>> getRecetas() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Recetas recuperadas con éxito", apoderadoPortalService.getRecetas()));
    }

    @GetMapping("/services")
    @PreAuthorize("@accesoValidator.can('VISTA_MIS_CITAS', 'LEER')")
    public ResponseEntity<ApiResponse<List<ServicioResponse>>> getServicios() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Servicios disponibles recuperados con éxito", apoderadoPortalService.getServicios()));
    }

    @GetMapping("/employees")
    @PreAuthorize("@accesoValidator.can('VISTA_MIS_CITAS', 'LEER')")
    public ResponseEntity<ApiResponse<List<EmpleadoListResponse>>> getEmpleados(@RequestParam(required = false) Long servicioId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Profesionales disponibles recuperados con éxito", apoderadoPortalService.getEmpleados(servicioId)));
    }

    @GetMapping("/employees/{employeeId}/schedule")
    @PreAuthorize("@accesoValidator.can('VISTA_MIS_CITAS', 'LEER')")
    public ResponseEntity<ApiResponse<List<HorarioEmpleadoResponse>>> getHorarioEmpleado(@PathVariable("employeeId") Long empleadoId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Horario del profesional recuperado con éxito", apoderadoPortalService.getHorarioEmpleado(empleadoId)));
    }

    @GetMapping("/availability")
    @PreAuthorize("@accesoValidator.can('VISTA_MIS_CITAS', 'LEER')")
    public ResponseEntity<ApiResponse<List<String>>> getDisponibilidad(
            @RequestParam Long empleadoId,
            @RequestParam String fecha,
            @RequestParam Long servicioId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Horarios de disponibilidad recuperados con éxito", apoderadoPortalService.getDisponibilidad(empleadoId, fecha, servicioId)));
    }

    @PostMapping("/appointments")
    @PreAuthorize("@accesoValidator.can('VISTA_MIS_CITAS', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<CitaResponse>> createPortalCita(@jakarta.validation.Valid @RequestBody CitaRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Cita registrada con éxito", apoderadoPortalService.createPortalCita(request)));
    }

    @PutMapping("/appointments/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_MIS_CITAS', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<CitaResponse>> updatePortalCita(
            @PathVariable Long id,
            @jakarta.validation.Valid @RequestBody CitaRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment updated successfully", apoderadoPortalService.updatePortalCita(id, request)));
    }

    @PutMapping("/appointments/{id}/reschedule")
    @PreAuthorize("@accesoValidator.can('VISTA_MIS_CITAS', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<CitaResponse>> reschedulePortalCita(
            @PathVariable Long id,
            @jakarta.validation.Valid @RequestBody CitaRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment rescheduled successfully", apoderadoPortalService.reschedulePortalCita(id, request)));
    }

    @DeleteMapping("/appointments/{id}/cancel")
    @PreAuthorize("@accesoValidator.can('VISTA_MIS_CITAS', 'ELIMINAR')")
    public ResponseEntity<ApiResponse<Void>> cancelPortalCita(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo) {
        apoderadoPortalService.cancelPortalCita(id, motivo);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cita cancelada con éxito", null));
    }

    @GetMapping("/payments")
    @PreAuthorize("@accesoValidator.can('VISTA_MIS_PAGOS', 'LEER')")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<PagoPortalResponse>>> getPaymentHistory(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("fechaHoraInicio").descending());
        return ResponseEntity.ok(new ApiResponse<>(true, "Historial de pagos recuperado con éxito", apoderadoPortalService.getPaymentHistory(pageable)));
    }
}

