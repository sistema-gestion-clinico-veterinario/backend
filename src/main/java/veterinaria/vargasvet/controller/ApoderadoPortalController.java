package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<ApoderadoPerfilResponse>> getPerfil() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Perfil recuperado con éxito", apoderadoPortalService.getPerfil()));
    }

    @GetMapping("/pets")
    public ResponseEntity<ApiResponse<List<MascotaResponse>>> getMascotas() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Mascotas recuperadas con éxito", apoderadoPortalService.getMascotas()));
    }

    @GetMapping("/pets/paginated")
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
    public ResponseEntity<ApiResponse<HistoriaClinicaDetalleResponse>> getHistoriaMascota(@PathVariable("petId") Long mascotaId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Historial clínico recuperado con éxito", apoderadoPortalService.getHistoriaMascota(mascotaId)));
    }

    @GetMapping("/appointments")
    public ResponseEntity<ApiResponse<List<CitaResponse>>> getCitas(@RequestParam(required = false) Long mascotaId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Citas recuperadas con éxito", apoderadoPortalService.getCitas(mascotaId)));
    }

    @GetMapping("/prescriptions")
    public ResponseEntity<ApiResponse<List<PrescripcionResumenResponse>>> getRecetas() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Recetas recuperadas con éxito", apoderadoPortalService.getRecetas()));
    }

    @GetMapping("/services")
    public ResponseEntity<ApiResponse<List<ServicioResponse>>> getServicios() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Servicios disponibles recuperados con éxito", apoderadoPortalService.getServicios()));
    }

    @GetMapping("/employees")
    public ResponseEntity<ApiResponse<List<EmpleadoListResponse>>> getEmpleados(@RequestParam(required = false) Long servicioId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Profesionales disponibles recuperados con éxito", apoderadoPortalService.getEmpleados(servicioId)));
    }

    @GetMapping("/employees/{employeeId}/schedule")
    public ResponseEntity<ApiResponse<List<HorarioEmpleadoResponse>>> getHorarioEmpleado(@PathVariable("employeeId") Long empleadoId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Horario del profesional recuperado con éxito", apoderadoPortalService.getHorarioEmpleado(empleadoId)));
    }

    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<List<String>>> getDisponibilidad(
            @RequestParam Long empleadoId,
            @RequestParam String fecha,
            @RequestParam Long servicioId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Horarios de disponibilidad recuperados con éxito", apoderadoPortalService.getDisponibilidad(empleadoId, fecha, servicioId)));
    }

    @PostMapping("/appointments")
    public ResponseEntity<ApiResponse<CitaResponse>> createPortalCita(@jakarta.validation.Valid @RequestBody CitaRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Cita registrada con éxito", apoderadoPortalService.createPortalCita(request)));
    }

    @PutMapping("/appointments/{id}")
    public ResponseEntity<ApiResponse<CitaResponse>> updatePortalCita(
            @PathVariable Long id,
            @jakarta.validation.Valid @RequestBody CitaRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment updated successfully", apoderadoPortalService.updatePortalCita(id, request)));
    }

    @PutMapping("/appointments/{id}/reschedule")
    public ResponseEntity<ApiResponse<CitaResponse>> reschedulePortalCita(
            @PathVariable Long id,
            @jakarta.validation.Valid @RequestBody CitaRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment rescheduled successfully", apoderadoPortalService.reschedulePortalCita(id, request)));
    }

    @DeleteMapping("/appointments/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelPortalCita(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo) {
        apoderadoPortalService.cancelPortalCita(id, motivo);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cita cancelada con éxito", null));
    }
}

