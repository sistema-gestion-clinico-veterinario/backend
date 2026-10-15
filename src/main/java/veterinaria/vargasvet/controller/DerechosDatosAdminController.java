package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.domain.enums.EstadoSolicitudDerecho;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.ResolverSolicitudDerechoRequest;
import veterinaria.vargasvet.dto.response.SolicitudDerechoDatosResponse;
import veterinaria.vargasvet.service.SolicitudDerechoDatosService;

import java.util.List;

@RestController
@RequestMapping("/admin/privacidad/derechos")
@RequiredArgsConstructor
@PreAuthorize("@accesoValidator.hasPurpose('COMPANY_ADMIN')")
public class DerechosDatosAdminController {
    private final SolicitudDerechoDatosService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SolicitudDerechoDatosResponse>>> listar(
            @RequestParam(required = false) EstadoSolicitudDerecho estado) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Solicitudes de derechos de datos", service.listar(estado)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SolicitudDerechoDatosResponse>> actualizar(
            @PathVariable Long id, @Valid @RequestBody ResolverSolicitudDerechoRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Solicitud actualizada", service.actualizar(id, request)));
    }
}
