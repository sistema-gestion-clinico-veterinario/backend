package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.ConfirmarCancelacionRequest;
import veterinaria.vargasvet.dto.request.PedirCodigoCancelacionRequest;
import veterinaria.vargasvet.dto.response.CancelacionDatosElegibilidad;
import veterinaria.vargasvet.dto.response.SolicitudCancelacionResponse;
import veterinaria.vargasvet.service.CancelacionDatosService;

import java.util.Map;

@RestController
@RequestMapping("/me/privacidad/cancelacion")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CancelacionDatosTitularController {

    private final CancelacionDatosService servicio;

    @GetMapping
    public ResponseEntity<ApiResponse<CancelacionDatosElegibilidad>> estado() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Estado de la solicitud", servicio.elegibilidad()));
    }

    @PostMapping("/codigo")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pedirCodigo(
            @Valid @RequestBody PedirCodigoCancelacionRequest request) {
        long espera = servicio.pedirCodigo(request.getPassword());
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Te enviamos un código de 6 dígitos a tu correo. Vale 10 minutos.",
                Map.of("retryAfterSeconds", espera)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SolicitudCancelacionResponse>> solicitar(
            @Valid @RequestBody ConfirmarCancelacionRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Recibimos tu solicitud. La clínica te responderá dentro del plazo indicado.",
                servicio.solicitar(request.getCode(), request.getMotivo())));
    }
}
