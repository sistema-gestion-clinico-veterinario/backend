package veterinaria.vargasvet.modules.users.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.modules.users.domain.entity.TipoEmpleado;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.modules.users.service.TipoEmpleadoService;

import java.util.List;

@RestController
@RequestMapping("/admin/tipos-empleado")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
public class TipoEmpleadoController {

    private final TipoEmpleadoService tipoEmpleadoService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TipoEmpleado>>> getAll(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<TipoEmpleado> todos = tipoEmpleadoService.findAll(companyId);
        int start = Math.min(page * size, todos.size());
        int end = Math.min(start + size, todos.size());
        Page<TipoEmpleado> resultado = new PageImpl<>(todos.subList(start, end), PageRequest.of(page, size), todos.size());
        return ResponseEntity.ok(new ApiResponse<>(true, "Tipos de empleado recuperados con éxito", resultado));
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
