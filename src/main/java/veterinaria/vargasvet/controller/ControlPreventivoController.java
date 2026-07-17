package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.*;
import veterinaria.vargasvet.dto.response.AplicacionPreventivaResponse;
import veterinaria.vargasvet.dto.response.ControlPreventivoResponse;
import veterinaria.vargasvet.dto.response.TipoVacunaResponse;
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
}
