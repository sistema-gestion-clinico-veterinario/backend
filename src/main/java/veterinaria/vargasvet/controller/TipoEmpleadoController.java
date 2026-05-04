package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.domain.entity.TipoEmpleado;
import veterinaria.vargasvet.service.TipoEmpleadoService;

import java.util.List;

@RestController
@RequestMapping("/admin/tipos-empleado")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
public class TipoEmpleadoController {

    private final TipoEmpleadoService tipoEmpleadoService;

    @GetMapping
    public List<TipoEmpleado> getAll() {
        return tipoEmpleadoService.findAll();
    }

    @PostMapping
    public ResponseEntity<TipoEmpleado> create(@RequestBody TipoEmpleado tipo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tipoEmpleadoService.create(tipo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tipoEmpleadoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
