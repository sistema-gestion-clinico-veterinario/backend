package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.CerrarConsultaRequest;
import veterinaria.vargasvet.dto.request.ConsultaRequest;
import veterinaria.vargasvet.dto.response.ConsultaResponse;
import veterinaria.vargasvet.service.ConsultaService;

@RestController
@RequestMapping("/consultas")
@RequiredArgsConstructor
public class ConsultaController {

    private final ConsultaService consultaService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<ApiResponse<ConsultaResponse>> getConsultaById(@PathVariable Long id) {
        ConsultaResponse response = consultaService.getConsultaById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Consulta recuperada con éxito", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO')")
    public ResponseEntity<ApiResponse<ConsultaResponse>> updateConsulta(@PathVariable Long id, @Valid @RequestBody ConsultaRequest request) {
        ConsultaResponse response = consultaService.updateConsulta(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Historia Clínica guardada exitosamente", response));
    }

    @PatchMapping("/{id}/cerrar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO')")
    public ResponseEntity<ApiResponse<ConsultaResponse>> cerrarConsulta(@PathVariable Long id, @Valid @RequestBody CerrarConsultaRequest request) {
        ConsultaResponse response = consultaService.cerrarConsulta(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Historia clínica cerrada exitosamente", response));
    }
}
