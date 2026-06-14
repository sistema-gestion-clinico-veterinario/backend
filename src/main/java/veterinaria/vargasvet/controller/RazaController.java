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
            @RequestParam(required = false) String especie,
            @RequestParam(required = false) Long companyId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Razas obtenidas",
                razaService.listarPorEspecie(especie, companyId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PET_CREATE')")
    public ResponseEntity<ApiResponse<RazaResponse>> crear(
            @Valid @RequestBody RazaRequest request,
            @RequestParam Long companyId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Raza creada exitosamente",
                        razaService.crear(request, companyId)));
    }
}
