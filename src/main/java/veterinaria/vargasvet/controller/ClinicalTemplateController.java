package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.ClinicalConceptRequest;
import veterinaria.vargasvet.dto.request.ClinicalTemplateDraftRequest;
import veterinaria.vargasvet.dto.request.ClinicalTemplateCreateRequest;
import veterinaria.vargasvet.dto.response.*;
import veterinaria.vargasvet.service.ClinicalTemplateService;

import java.util.List;

@RestController
@RequestMapping("/clinical-templates")
@RequiredArgsConstructor
public class ClinicalTemplateController {
    private final ClinicalTemplateService service;

    @GetMapping("/current")
    @PreAuthorize("@accesoValidator.can('VISTA_HISTORIAS', 'LEER')")
    public ResponseEntity<ApiResponse<ClinicalTemplateResponse>> current(
            @RequestParam(required = false) String species,
            @RequestParam(required = false) Long serviceId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Ficha clínica vigente", service.current(species, serviceId)));
    }

    @GetMapping
    @PreAuthorize("@accesoValidator.can('VISTA_COMPLEMENTARIO', 'LEER')")
    public ResponseEntity<ApiResponse<List<ClinicalTemplateSummaryResponse>>> list() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Versiones de ficha clínica", service.list()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_HISTORIAS', 'LEER')")
    public ResponseEntity<ApiResponse<ClinicalTemplateResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Ficha clínica recuperada", service.get(id)));
    }

    @GetMapping("/concepts")
    @PreAuthorize("@accesoValidator.can('VISTA_COMPLEMENTARIO', 'LEER')")
    public ResponseEntity<ApiResponse<List<ClinicalConceptResponse>>> concepts() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Campos clínicos disponibles", service.concepts()));
    }

    @PostMapping("/concepts")
    @PreAuthorize("@accesoValidator.can('VISTA_COMPLEMENTARIO', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<ClinicalConceptResponse>> createConcept(
            @Valid @RequestBody ClinicalConceptRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Campo clínico creado", service.createConcept(request)));
    }

    @PostMapping("/drafts")
    @PreAuthorize("@accesoValidator.can('VISTA_COMPLEMENTARIO', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<ClinicalTemplateResponse>> createDraft() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Borrador preparado", service.createDraft()));
    }

    @PostMapping
    @PreAuthorize("@accesoValidator.can('VISTA_COMPLEMENTARIO', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<ClinicalTemplateResponse>> create(
            @Valid @RequestBody ClinicalTemplateCreateRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Ficha clínica creada", service.createTemplate(request)));
    }

    @PostMapping("/{id}/drafts")
    @PreAuthorize("@accesoValidator.can('VISTA_COMPLEMENTARIO', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<ClinicalTemplateResponse>> createDraftFrom(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Borrador preparado", service.createDraft(id)));
    }

    @PostMapping("/{id}/default")
    @PreAuthorize("@accesoValidator.can('VISTA_COMPLEMENTARIO', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<ClinicalTemplateResponse>> makeDefault(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Ficha predeterminada actualizada", service.makeDefault(id)));
    }

    @PutMapping("/drafts/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_COMPLEMENTARIO', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<ClinicalTemplateResponse>> updateDraft(
            @PathVariable Long id, @Valid @RequestBody ClinicalTemplateDraftRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Borrador actualizado", service.updateDraft(id, request)));
    }

    @PostMapping("/drafts/{id}/publish")
    @PreAuthorize("@accesoValidator.can('VISTA_COMPLEMENTARIO', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<ClinicalTemplateResponse>> publish(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Nueva versión publicada", service.publish(id)));
    }
}
