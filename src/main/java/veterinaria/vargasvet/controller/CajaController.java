package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.MovimientoEgresoRequest;
import veterinaria.vargasvet.dto.response.MovimientoCajaResponse;
import veterinaria.vargasvet.dto.response.ResumenCajaResponse;
import veterinaria.vargasvet.service.CajaService;

import java.time.LocalDate;

@RestController
@RequestMapping("/caja")
@RequiredArgsConstructor
public class CajaController {

    private final CajaService cajaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('SALE_READ')")
    public ResponseEntity<ApiResponse<Page<MovimientoCajaResponse>>> listar(
            @RequestParam Integer companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Movimientos obtenidos",
                cajaService.listar(companyId, desde, hasta, page, size)));
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('SALE_READ')")
    public ResponseEntity<ApiResponse<ResumenCajaResponse>> resumen(
            @RequestParam Integer companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Resumen obtenido",
                cajaService.getResumen(companyId, desde, hasta)));
    }

    @PostMapping("/egreso")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('SALE_CREATE')")
    public ResponseEntity<ApiResponse<MovimientoCajaResponse>> registrarEgreso(
            @Valid @RequestBody MovimientoEgresoRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Egreso registrado",
                cajaService.registrarEgreso(request)));
    }

    @PostMapping("/devolucion/{citaId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('SALE_CREATE')")
    public ResponseEntity<ApiResponse<MovimientoCajaResponse>> registrarDevolucion(
            @PathVariable Long citaId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Devolución registrada",
                cajaService.registrarDevolucion(citaId)));
    }
}
