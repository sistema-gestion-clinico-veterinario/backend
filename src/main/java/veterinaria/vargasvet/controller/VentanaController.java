package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import veterinaria.vargasvet.domain.entity.Ventana;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.response.MenuItemDTO;
import veterinaria.vargasvet.service.VentanaService;

import java.util.List;

@RestController
@RequestMapping("/admin/ventanas")
@RequiredArgsConstructor
public class VentanaController {

    private final VentanaService ventanaService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuItemDTO>>> listar() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Ventanas obtenidas", ventanaService.listarTodas()));
    }

    @GetMapping("/arbol")
    public ResponseEntity<ApiResponse<List<MenuItemDTO>>> listarArbol() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Árbol de ventanas obtenido", ventanaService.listarArbol()));
    }

    @GetMapping("/arbol/completo")
    public ResponseEntity<ApiResponse<List<MenuItemDTO>>> listarArbolCompleto() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Árbol completo", ventanaService.listarArbolCompleto()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Ventana>> crear(@RequestBody Ventana ventana) {
        Ventana creada = ventanaService.crear(ventana);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Ventana creada exitosamente", creada));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Ventana>> actualizar(@PathVariable Integer id, @RequestBody Ventana ventana) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Ventana actualizada", ventanaService.actualizar(id, ventana)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Integer id) {
        ventanaService.eliminar(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Ventana eliminada", null));
    }
}
