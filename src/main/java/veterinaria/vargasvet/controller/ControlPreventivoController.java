package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.*;
import veterinaria.vargasvet.dto.response.AplicacionPreventivaResponse;
import veterinaria.vargasvet.dto.response.ControlPreventivoResponse;
import veterinaria.vargasvet.dto.response.TipoDesparasitanteResponse;
import veterinaria.vargasvet.dto.response.TipoVacunaResponse;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.ControlPreventivoService;

import java.util.List;

@RestController
@RequestMapping("/preventive-controls")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO') or hasAuthority('CLINICAL_RECORD_MANAGE')")
public class ControlPreventivoController {
    private final ControlPreventivoService service;

    @GetMapping("/pets/{petId}/vaccine-types")
    public ResponseEntity<ApiResponse<List<TipoVacunaResponse>>> listarTipos(@PathVariable Long petId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Vacunas recuperadas", service.listarTiposVacuna(petId)));
    }

    @PostMapping("/vaccine-types")
    public ResponseEntity<ApiResponse<TipoVacunaResponse>> crearTipo(@Valid @RequestBody TipoVacunaRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Vacuna creada", service.crearTipoVacuna(request)));
    }

    @GetMapping("/pets/{petId}/deworming-products")
    public ResponseEntity<ApiResponse<List<TipoDesparasitanteResponse>>> listarDesparasitantes(@PathVariable Long petId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Desparasitantes recuperados", service.listarTiposDesparasitante(petId)));
    }

    @PostMapping("/deworming-products")
    public ResponseEntity<ApiResponse<TipoDesparasitanteResponse>> crearDesparasitante(@Valid @RequestBody TipoDesparasitanteRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Desparasitante creado", service.crearTipoDesparasitante(request)));
    }

    @GetMapping("/pets/{petId}")
    public ResponseEntity<ApiResponse<List<ControlPreventivoResponse>>> listar(@PathVariable Long petId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Controles recuperados", service.listarControles(petId)));
    }

    @GetMapping("/pets/{petId}/applications")
    public ResponseEntity<ApiResponse<List<AplicacionPreventivaResponse>>> aplicaciones(@PathVariable Long petId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Aplicaciones recuperadas", service.listarAplicaciones(petId)));
    }

    @PostMapping("/pets/{petId}")
    public ResponseEntity<ApiResponse<ControlPreventivoResponse>> programar(@PathVariable Long petId,
            @Valid @RequestBody ControlPreventivoRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Control programado", service.programar(petId, request)));
    }

    @PutMapping("/{controlId}/schedule")
    public ResponseEntity<ApiResponse<ControlPreventivoResponse>> reprogramar(@PathVariable Long controlId,
            @Valid @RequestBody ReprogramarControlPreventivoRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Control reprogramado", service.reprogramar(controlId, request)));
    }

    @PatchMapping("/{controlId}/cancel")
    public ResponseEntity<ApiResponse<ControlPreventivoResponse>> cancelar(@PathVariable Long controlId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Control cancelado", service.cancelar(controlId)));
    }

    @PostMapping("/consultations/{consultationId}/vaccinations")
    public ResponseEntity<ApiResponse<ControlPreventivoResponse>> vacunar(@PathVariable Long consultationId,
            @Valid @RequestBody RegistroVacunacionRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Vacunacion registrada", service.registrarVacunacion(consultationId, request)));
    }

    @PostMapping("/consultations/{consultationId}/dewormings")
    public ResponseEntity<ApiResponse<ControlPreventivoResponse>> desparasitar(@PathVariable Long consultationId,
            @Valid @RequestBody RegistroDesparasitacionRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Desparasitacion registrada", service.registrarDesparasitacion(consultationId, request)));
    }

    @GetMapping("/company/vaccine-types")
    public ResponseEntity<ApiResponse<Page<TipoVacunaResponse>>> listarTiposVacunaPorCompany(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vacunas recuperadas",
                service.listarTiposVacunaPorCompany(companyId, PageRequest.of(safePage, safeSize, Sort.by("nombre").ascending()))));
    }

    @GetMapping("/company/deworming-products")
    public ResponseEntity<ApiResponse<Page<TipoDesparasitanteResponse>>> listarDesparasitantesPorCompany(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ResponseEntity.ok(new ApiResponse<>(true, "Desparasitantes recuperados",
                service.listarTiposDesparasitantePorCompany(companyId, PageRequest.of(safePage, safeSize, Sort.by("nombre").ascending()))));
    }

    @PutMapping("/vaccine-types/{id}")
    public ResponseEntity<ApiResponse<TipoVacunaResponse>> actualizarTipoVacuna(
            @PathVariable Long id, @Valid @RequestBody TipoVacunaRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Vacuna actualizada", service.actualizarTipoVacuna(id, request)));
    }

    @PatchMapping("/vaccine-types/{id}/status")
    public ResponseEntity<ApiResponse<Void>> cambiarEstadoTipoVacuna(
            @PathVariable Long id, @RequestParam boolean activo) {
        service.cambiarEstadoTipoVacuna(id, activo);
        return ResponseEntity.ok(new ApiResponse<>(true, activo ? "Vacuna activada" : "Vacuna desactivada", null));
    }

    @DeleteMapping("/vaccine-types/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminarTipoVacuna(@PathVariable Long id) {
        service.eliminarTipoVacuna(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vacuna eliminada", null));
    }

    @PutMapping("/deworming-products/{id}")
    public ResponseEntity<ApiResponse<TipoDesparasitanteResponse>> actualizarTipoDesparasitante(
            @PathVariable Long id, @Valid @RequestBody TipoDesparasitanteRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Desparasitante actualizado", service.actualizarTipoDesparasitante(id, request)));
    }

    @PatchMapping("/deworming-products/{id}/status")
    public ResponseEntity<ApiResponse<Void>> cambiarEstadoTipoDesparasitante(
            @PathVariable Long id, @RequestParam boolean activo) {
        service.cambiarEstadoTipoDesparasitante(id, activo);
        return ResponseEntity.ok(new ApiResponse<>(true, activo ? "Desparasitante activado" : "Desparasitante desactivado", null));
    }

    @DeleteMapping("/deworming-products/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminarTipoDesparasitante(@PathVariable Long id) {
        service.eliminarTipoDesparasitante(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Desparasitante eliminado", null));
    }
}
