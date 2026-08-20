package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.CartillaAplicacionRequest;
import veterinaria.vargasvet.dto.response.AplicacionPreventivaResponse;
import veterinaria.vargasvet.dto.response.CartillaAplicacionResponse;
import veterinaria.vargasvet.service.CartillaService;
import veterinaria.vargasvet.service.ControlPreventivoService;

import java.util.List;

@RestController
@RequestMapping("/cartilla")
@RequiredArgsConstructor
public class CartillaController {

    private final CartillaService cartillaService;
    private final ControlPreventivoService controlPreventivoService;

    @PostMapping("/vaccinations")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO') or hasAuthority('CLINICAL_RECORD_MANAGE')")
    public ResponseEntity<ApiResponse<CartillaAplicacionResponse>> registrarVacunacion(
            @Valid @RequestBody CartillaAplicacionRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Vacunacion registrada y cobro generado",
                cartillaService.registrarVacunacion(request)));
    }

    @PostMapping("/dewormings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO') or hasAuthority('CLINICAL_RECORD_MANAGE')")
    public ResponseEntity<ApiResponse<CartillaAplicacionResponse>> registrarDesparasitacion(
            @Valid @RequestBody CartillaAplicacionRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Desparasitacion registrada y cobro generado",
                cartillaService.registrarDesparasitacion(request)));
    }

    @GetMapping("/pets/{petId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA') or hasAuthority('CLINICAL_RECORD_READ')")
    public ResponseEntity<ApiResponse<List<AplicacionPreventivaResponse>>> cartilla(@PathVariable Long petId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Cartilla de la mascota recuperada",
                controlPreventivoService.listarAplicaciones(petId)));
    }
}