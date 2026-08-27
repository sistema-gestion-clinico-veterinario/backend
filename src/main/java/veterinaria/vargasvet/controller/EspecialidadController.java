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
import veterinaria.vargasvet.domain.entity.Especialidad;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.service.EspecialidadService;

@RestController
@RequestMapping("/admin/specialties")
@RequiredArgsConstructor
public class EspecialidadController {

    private final EspecialidadService especialidadService;

    @GetMapping
    @PreAuthorize("hasAuthority('ESPECIALIDAD_READ')")
    public ResponseEntity<ApiResponse<Page<Especialidad>>> getAll(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Especialidad> resultado = especialidadService.findAll(
                companyId, PageRequest.of(page, size, Sort.by("nombre").ascending()));
        return ResponseEntity.ok(new ApiResponse<>(true, "Especialidades recuperadas con éxito", resultado));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ESPECIALIDAD_CREATE')")
    public ResponseEntity<Especialidad> create(@Valid @RequestBody Especialidad especialidad) {
        return ResponseEntity.status(HttpStatus.CREATED).body(especialidadService.create(especialidad));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ESPECIALIDAD_UPDATE')")
    public Especialidad update(@PathVariable Long id, @Valid @RequestBody Especialidad especialidad) {
        return especialidadService.update(id, especialidad);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ESPECIALIDAD_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        especialidadService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
