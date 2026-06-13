package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.RazaRequest;
import veterinaria.vargasvet.dto.response.RazaResponse;
import veterinaria.vargasvet.service.RazaService;

import java.util.List;

@RestController
@RequestMapping("/breeds")
@RequiredArgsConstructor
public class RazaController {

    private final RazaService razaService;

    @GetMapping
    @PreAuthorize("hasAuthority('PET_READ')")
    public ResponseEntity<ApiResponse<List<RazaResponse>>> listar(
            @RequestParam(required = false) String especie) {
        List<RazaResponse> resultado = razaService.listarPorEspecie(especie);
        return ResponseEntity.ok(new ApiResponse<>(true, "Razas obtenidas", resultado));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PET_CREATE')")
    public ResponseEntity<ApiResponse<RazaResponse>> crear(@Valid @RequestBody RazaRequest request) {
        RazaResponse response = razaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Raza creada exitosamente", response));
    }
}
