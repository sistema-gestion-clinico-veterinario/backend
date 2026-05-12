package veterinaria.vargasvet.servicios;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.servicios.Especialidad;
import veterinaria.vargasvet.shared.ApiResponse;
import veterinaria.vargasvet.servicios.EspecialidadService;

import java.util.List;

@RestController
@RequestMapping("/admin/especialidades")
@RequiredArgsConstructor
public class EspecialidadController {

    private final EspecialidadService especialidadService;

    @GetMapping
    @PreAuthorize("hasAuthority('ESPECIALIDAD_READ')")
    public ResponseEntity<ApiResponse<Page<Especialidad>>> getAll(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<Especialidad> todas = especialidadService.findAll(companyId);
        int start = Math.min(page * size, todas.size());
        int end = Math.min(start + size, todas.size());
        Page<Especialidad> resultado = new PageImpl<>(todas.subList(start, end), PageRequest.of(page, size), todas.size());
        return ResponseEntity.ok(new ApiResponse<>(true, "Especialidades recuperadas con éxito", resultado));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ESPECIALIDAD_CREATE')")
    public ResponseEntity<Especialidad> create(@RequestBody Especialidad especialidad) {
        return ResponseEntity.status(HttpStatus.CREATED).body(especialidadService.create(especialidad));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ESPECIALIDAD_UPDATE')")
    public Especialidad update(@PathVariable Long id, @RequestBody Especialidad especialidad) {
        return especialidadService.update(id, especialidad);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ESPECIALIDAD_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        especialidadService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
