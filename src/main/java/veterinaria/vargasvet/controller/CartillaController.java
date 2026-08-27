package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.CartillaAplicacionEditRequest;
import veterinaria.vargasvet.dto.request.CartillaAplicacionRequest;
import veterinaria.vargasvet.dto.response.AplicacionPreventivaResponse;
import veterinaria.vargasvet.dto.response.CartillaAplicacionResponse;
import veterinaria.vargasvet.dto.response.CartillaDetalleResponse;
import veterinaria.vargasvet.dto.response.MascotaCartillaResponse;
import veterinaria.vargasvet.dto.response.RecordatorioWhatsAppResponse;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.security.SecurityUtils;
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

    @GetMapping("/pets/{petId}/detail")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA') or hasAuthority('CLINICAL_RECORD_READ')")
    public ResponseEntity<ApiResponse<CartillaDetalleResponse>> detalleCartilla(@PathVariable Long petId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Detalle de cartilla recuperado",
                controlPreventivoService.obtenerDetalleCartilla(petId)));
    }

    @GetMapping("/pets")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA') or hasAuthority('CLINICAL_RECORD_READ')")
    public ResponseEntity<ApiResponse<Page<MascotaCartillaResponse>>> listarMascotasConCartilla(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) EspecieMascota especie,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        if (companyId == null) {
            return ResponseEntity.ok(new ApiResponse<>(true, "Mascotas con cartilla",
                    Page.empty(PageRequest.of(safePage, safeSize))));
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Mascotas con cartilla",
                cartillaService.listarMascotasConCartilla(companyId, nombre, especie, true,
                        PageRequest.of(safePage, safeSize, Sort.by("nombreCompleto").ascending()))));
    }

    @PutMapping("/vaccinations/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO') or hasAuthority('CLINICAL_RECORD_MANAGE')")
    public ResponseEntity<ApiResponse<CartillaAplicacionResponse>> editarVacunacion(
            @PathVariable Long id, @Valid @RequestBody CartillaAplicacionEditRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Vacunacion actualizada",
                cartillaService.editarVacunacion(id, request)));
    }

    @PatchMapping("/vaccinations/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO') or hasAuthority('CLINICAL_RECORD_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> cambiarEstadoVacunacion(
            @PathVariable Long id, @RequestParam boolean activo) {
        cartillaService.cambiarEstadoVacunacion(id, activo);
        return ResponseEntity.ok(new ApiResponse<>(true, activo ? "Vacunacion activada" : "Vacunacion desactivada", null));
    }

    @PutMapping("/dewormings/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO') or hasAuthority('CLINICAL_RECORD_MANAGE')")
    public ResponseEntity<ApiResponse<CartillaAplicacionResponse>> editarDesparasitacion(
            @PathVariable Long id, @Valid @RequestBody CartillaAplicacionEditRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Desparasitacion actualizada",
                cartillaService.editarDesparasitacion(id, request)));
    }

    @PatchMapping("/dewormings/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO') or hasAuthority('CLINICAL_RECORD_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> cambiarEstadoDesparasitacion(
            @PathVariable Long id, @RequestParam boolean activo) {
        cartillaService.cambiarEstadoDesparasitacion(id, activo);
        return ResponseEntity.ok(new ApiResponse<>(true, activo ? "Desparasitacion activada" : "Desparasitacion desactivada", null));
    }

    @GetMapping("/recordatorios-whatsapp")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA') or hasAuthority('CLINICAL_RECORD_READ')")
    public ResponseEntity<ApiResponse<java.util.List<veterinaria.vargasvet.dto.response.RecordatorioWhatsAppResponse>>> recordatoriosWhatsApp() {
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Recordatorios recuperados",
                cartillaService.listarRecordatoriosWhatsApp(companyId)));
    }

}
