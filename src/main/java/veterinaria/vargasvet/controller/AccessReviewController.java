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
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.AccessReviewRequest;
import veterinaria.vargasvet.dto.response.AccessReviewDetailDTO;
import veterinaria.vargasvet.dto.response.AccessReviewOverviewDTO;
import veterinaria.vargasvet.dto.response.AccessReviewSummaryDTO;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AccessReviewService;

import java.util.List;

@RestController
@RequestMapping("/admin/access-reviews")
@RequiredArgsConstructor
public class AccessReviewController {

    private final AccessReviewService accessReviewService;

    @GetMapping
    @PreAuthorize("@accesoValidator.can('VISTA_ROLES', 'LEER')")
    public ResponseEntity<ApiResponse<AccessReviewOverviewDTO>> overview(
            @RequestParam(required = false) Integer companyId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Accesos privilegiados por revisar",
                accessReviewService.overview(resolveCompanyId(companyId))));
    }

    @PostMapping
    @PreAuthorize("@accesoValidator.can('VISTA_ROLES', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<AccessReviewSummaryDTO>> register(
            @RequestParam(required = false) Integer companyId,
            @Valid @RequestBody AccessReviewRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Revisión de accesos registrada",
                accessReviewService.register(resolveCompanyId(companyId), request)));
    }

    @GetMapping("/history")
    @PreAuthorize("@accesoValidator.can('VISTA_ROLES', 'LEER')")
    public ResponseEntity<ApiResponse<List<AccessReviewSummaryDTO>>> history(
            @RequestParam(required = false) Integer companyId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Historial de revisiones",
                accessReviewService.history(resolveCompanyId(companyId))));
    }

    @GetMapping("/{reviewId}")
    @PreAuthorize("@accesoValidator.can('VISTA_ROLES', 'LEER')")
    public ResponseEntity<ApiResponse<AccessReviewDetailDTO>> detail(
            @PathVariable Long reviewId,
            @RequestParam(required = false) Integer companyId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Detalle de la revisión",
                accessReviewService.detail(resolveCompanyId(companyId), reviewId)));
    }

    private Integer resolveCompanyId(Integer companyId) {
        Integer effectiveId = SecurityUtils.isSuperAdmin()
                ? companyId
                : SecurityUtils.getCurrentCompanyId();
        if (effectiveId == null) {
            throw new IllegalArgumentException("Debe indicar el companyId");
        }
        return effectiveId;
    }
}
