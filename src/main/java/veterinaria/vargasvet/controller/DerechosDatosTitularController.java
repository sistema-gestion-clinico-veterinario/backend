package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.CrearSolicitudDerechoRequest;
import veterinaria.vargasvet.dto.response.SolicitudDerechoDatosResponse;
import veterinaria.vargasvet.service.SolicitudDerechoDatosService;
import veterinaria.vargasvet.security.ClientIpResolver;

import java.util.List;

@RestController
@RequestMapping("/me/privacidad/derechos")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DerechosDatosTitularController {
    private final SolicitudDerechoDatosService service;
    private final ClientIpResolver clientIpResolver;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SolicitudDerechoDatosResponse>>> listar() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Solicitudes de derechos de datos", service.listarMias()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SolicitudDerechoDatosResponse>> crear(
            @Valid @RequestBody CrearSolicitudDerechoRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Solicitud registrada",
                service.crear(request, clientIpResolver.resolve(http), http.getHeader("User-Agent"))));
    }
}
