package veterinaria.vargasvet.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.AcceptLegalRequest;
import veterinaria.vargasvet.dto.response.LegalDocumentDTO;
import veterinaria.vargasvet.dto.response.LegalStatusDTO;
import veterinaria.vargasvet.security.ClientIpResolver;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.LegalDocumentService;

import java.util.List;

@RestController
@RequestMapping("/legal")
@RequiredArgsConstructor
public class LegalController {

    private final LegalDocumentService legalDocumentService;
    private final ClientIpResolver clientIpResolver;

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<List<LegalDocumentDTO>>> getCurrentDocuments() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Documentos legales vigentes",
                legalDocumentService.getActiveDocuments()));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<LegalStatusDTO>> getStatus() {
        Integer usuarioId = currentUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Estado de aceptación legal",
                legalDocumentService.getStatus(usuarioId)));
    }

    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<Void>> accept(@Valid @RequestBody AcceptLegalRequest request,
                                                      HttpServletRequest httpRequest) {
        Integer usuarioId = currentUserId();
        legalDocumentService.accept(usuarioId, request.getLegalDocumentIds(),
                clientIpResolver.resolve(httpRequest), httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(new ApiResponse<>(true, "Documentos aceptados exitosamente", null));
    }

    private Integer currentUserId() {
        UsuarioPrincipal principal = (UsuarioPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return principal.getId();
    }
}
