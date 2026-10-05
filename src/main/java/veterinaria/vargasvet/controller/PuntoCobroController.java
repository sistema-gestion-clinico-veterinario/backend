package veterinaria.vargasvet.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.PuntoCobroRequest;
import veterinaria.vargasvet.dto.response.EstadoEquipoResponse;
import veterinaria.vargasvet.dto.response.PuntoCobroResponse;
import veterinaria.vargasvet.service.PuntoCobroService;

import java.util.List;

@RestController
@RequestMapping("/caja/puntos")
@RequiredArgsConstructor
public class PuntoCobroController {

    private final PuntoCobroService puntoCobroService;

    @GetMapping
    @PreAuthorize("@accesoValidator.can('VISTA_CAJA', 'LEER')")
    public ResponseEntity<ApiResponse<List<PuntoCobroResponse>>> listar(@RequestParam Integer companyId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Puntos de cobro recuperados", puntoCobroService.listar(companyId)));
    }

    @GetMapping("/este-equipo")
    @PreAuthorize("@accesoValidator.can('VISTA_CAJA', 'LEER')")
    public ResponseEntity<ApiResponse<EstadoEquipoResponse>> esteEquipo(@RequestParam Integer companyId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Equipo consultado", puntoCobroService.estadoDeEsteEquipo(companyId)));
    }

    @PostMapping
    @PreAuthorize("@accesoValidator.can('VISTA_CAJA', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<PuntoCobroResponse>> crear(@Valid @RequestBody PuntoCobroRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Punto de cobro creado",
                puntoCobroService.crear(request.getCompanyId(), request.getNombre())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_CAJA', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<PuntoCobroResponse>> actualizar(@PathVariable Long id,
                                                                      @Valid @RequestBody PuntoCobroRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Punto de cobro actualizado",
                puntoCobroService.actualizar(request.getCompanyId(), id, request.getNombre(), request.getActiva())));
    }

    @PostMapping("/{id}/vincular")
    @PreAuthorize("@accesoValidator.can('VISTA_CAJA', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<PuntoCobroResponse>> vincular(@PathVariable Long id, @RequestParam Integer companyId,
                                                                    HttpServletRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Este equipo quedó registrado como punto de cobro",
                puntoCobroService.vincular(companyId, id, request, response)));
    }

    @PostMapping("/{id}/desvincular")
    @PreAuthorize("@accesoValidator.can('VISTA_CAJA', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<PuntoCobroResponse>> desvincular(@PathVariable Long id, @RequestParam Integer companyId,
                                                                       HttpServletRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Se quitó el equipo del punto de cobro",
                puntoCobroService.desvincular(companyId, id, request, response)));
    }
}
