package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.EspecialidadRequest;
import veterinaria.vargasvet.dto.response.EspecialidadResponse;
import veterinaria.vargasvet.service.EspecialidadService;

@RestController
@RequestMapping("/admin/specialties")
@RequiredArgsConstructor
public class EspecialidadController {

    private final EspecialidadService especialidadService;

    @GetMapping
    @PreAuthorize("@accesoValidator.can('VISTA_COMPLEMENTARIO', 'LEER')")
    public ResponseEntity<ApiResponse<Page<EspecialidadResponse>>> getAll(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<EspecialidadResponse> resultado = especialidadService.findAll(
                companyId, PageRequest.of(page, size, Sort.by("nombre").ascending()))
                .map(especialidad -> EspecialidadResponse.de(especialidad, especialidadService.canDelete(especialidad.getId())));
        return ResponseEntity.ok(new ApiResponse<>(true, "Especialidades recuperadas con éxito", resultado));
    }

    @PostMapping
    @PreAuthorize("@accesoValidator.can('VISTA_COMPLEMENTARIO', 'ESCRIBIR')")
    public ResponseEntity<EspecialidadResponse> create(@Valid @RequestBody EspecialidadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EspecialidadResponse.de(especialidadService.create(request.aEntidad())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_COMPLEMENTARIO', 'MODIFICAR')")
    public EspecialidadResponse update(@PathVariable Long id, @Valid @RequestBody EspecialidadRequest request) {
        var especialidad = especialidadService.update(id, request.aEntidad());
        return EspecialidadResponse.de(especialidad, especialidadService.canDelete(especialidad.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_COMPLEMENTARIO', 'ELIMINAR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        especialidadService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
