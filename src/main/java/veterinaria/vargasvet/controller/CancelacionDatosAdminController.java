package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import veterinaria.vargasvet.domain.enums.EstadoSolicitudCancelacion;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.RechazarCancelacionRequest;
import veterinaria.vargasvet.dto.request.RegistrarCancelacionAdminRequest;
import veterinaria.vargasvet.dto.response.SolicitudCancelacionResponse;
import veterinaria.vargasvet.service.CancelacionDatosService;

import java.util.List;

@RestController
@RequestMapping("/admin/privacidad/cancelaciones")
@RequiredArgsConstructor
@PreAuthorize("@accesoValidator.hasPurpose('COMPANY_ADMIN')")
public class CancelacionDatosAdminController {

    private final CancelacionDatosService servicio;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SolicitudCancelacionResponse>>> listar(
            @RequestParam(required = false) EstadoSolicitudCancelacion estado) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Solicitudes de cancelación", servicio.listar(estado)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SolicitudCancelacionResponse>> registrar(
            @Valid @RequestBody RegistrarCancelacionAdminRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Solicitud registrada",
                servicio.registrarPorAdministrador(request.getApoderadoId(), request.getMetodoVerificacion(), request.getMotivo())));
    }

    @PostMapping("/{id}/ejecutar")
    public ResponseEntity<ApiResponse<SolicitudCancelacionResponse>> ejecutar(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Los datos personales fueron cancelados", servicio.ejecutar(id)));
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<ApiResponse<SolicitudCancelacionResponse>> rechazar(
            @PathVariable Long id, @Valid @RequestBody RechazarCancelacionRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Solicitud rechazada", servicio.rechazar(id, request.getMotivo())));
    }
}
