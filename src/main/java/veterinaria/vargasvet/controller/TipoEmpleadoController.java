package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.domain.entity.TipoEmpleado;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.service.TipoEmpleadoService;

import java.util.List;

@RestController
@RequestMapping("/admin/tipos-empleado")
@RequiredArgsConstructor
public class TipoEmpleadoController {

    private final TipoEmpleadoService tipoEmpleadoService;

    @GetMapping
    @PreAuthorize("hasAuthority('TIPO_EMPLEADO_READ')")
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
    @PreAuthorize("hasAuthority('TIPO_EMPLEADO_CREATE')")
    public ResponseEntity<ApiResponse<TipoEmpleado>> create(@RequestBody TipoEmpleado tipo) {
        TipoEmpleado created = tipoEmpleadoService.create(tipo);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, "Tipo de empleado creado", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TIPO_EMPLEADO_UPDATE')")
    public ResponseEntity<ApiResponse<TipoEmpleado>> update(@PathVariable Long id, @RequestBody TipoEmpleado tipo) {
        TipoEmpleado updated = tipoEmpleadoService.update(id, tipo);
        return ResponseEntity.ok(new ApiResponse<>(true, "Tipo de empleado actualizado", updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('TIPO_EMPLEADO_STATUS')")
    public ResponseEntity<ApiResponse<Void>> cambiarEstado(@PathVariable Long id, @RequestParam Boolean active) {
        tipoEmpleadoService.cambiarEstado(id, active);
        String msg = active ? "Tipo de empleado activado" : "Tipo de empleado desactivado";
        return ResponseEntity.ok(new ApiResponse<>(true, msg, null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TIPO_EMPLEADO_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tipoEmpleadoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
