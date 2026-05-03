package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.domain.entity.Especialidad;
import veterinaria.vargasvet.service.EspecialidadService;

import java.util.List;

@RestController
@RequestMapping("/admin/especialidades")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
public class EspecialidadController {

    private final EspecialidadService especialidadService;

    @GetMapping
    public List<Especialidad> getAll() {
        return especialidadService.findAll();
    }

    @PostMapping
    public ResponseEntity<Especialidad> create(@RequestBody Especialidad especialidad) {
        return ResponseEntity.status(HttpStatus.CREATED).body(especialidadService.create(especialidad));
    }

    @PutMapping("/{id}")
    public Especialidad update(@PathVariable Long id, @RequestBody Especialidad especialidad) {
        return especialidadService.update(id, especialidad);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        especialidadService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
