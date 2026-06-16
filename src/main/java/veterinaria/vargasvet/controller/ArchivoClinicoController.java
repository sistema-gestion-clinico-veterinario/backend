package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import veterinaria.vargasvet.domain.enums.TipoArchivo;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.response.ArchivoClinicoResponse;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.service.ArchivoClinicoService;

import java.util.List;

@RestController
@RequestMapping("/consultations/{consultationId}/files")
@RequiredArgsConstructor
public class ArchivoClinicoController {

    private final ArchivoClinicoService archivoClinicoService;
    private final AccesoValidator accesoValidator;

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO') or hasAuthority('CLINICAL_RECORD_MANAGE')")
    public ResponseEntity<ApiResponse<ArchivoClinicoResponse>> subirArchivo(
            @PathVariable("consultationId") Long consultaId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("tipo") TipoArchivo tipo,
            @RequestParam(value = "descripcion", required = false) String descripcion) {

        accesoValidator.validarEscribir("VISTA_HISTORIAS");
        ArchivoClinicoResponse response = archivoClinicoService.subirArchivo(consultaId, file, tipo, descripcion);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Archivo cargado exitosamente", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA') or hasAuthority('CLINICAL_RECORD_READ')")
    public ResponseEntity<ApiResponse<List<ArchivoClinicoResponse>>> listarArchivos(@PathVariable("consultationId") Long consultaId) {
        accesoValidator.validarLeer("VISTA_HISTORIAS");
        List<ArchivoClinicoResponse> archivos = archivoClinicoService.listarPorConsulta(consultaId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Archivos recuperados con éxito", archivos));
    }

    @GetMapping("/{id}/content")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA') or hasAuthority('CLINICAL_RECORD_READ')")
    public ResponseEntity<?> servirContenido(
            @PathVariable("consultationId") Long consultaId,
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean descargar) {
        accesoValidator.validarLeer("VISTA_HISTORIAS");
        ArchivoClinicoResponse meta = archivoClinicoService.obtenerPorId(id);

        if (meta.getUrl() != null && meta.getUrl().startsWith("http")) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, meta.getUrl())
                    .build();
        }

        Resource resource = archivoClinicoService.servirContenido(id);
        String contentType = meta.getTipoMime() != null ? meta.getTipoMime() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String disposition = descargar
                ? "attachment; filename=\"" + meta.getNombre() + "\""
                : "inline; filename=\"" + meta.getNombre() + "\"";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO') or hasAuthority('CLINICAL_RECORD_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> eliminar(
            @PathVariable("consultationId") Long consultaId,
            @PathVariable Long id) {
        accesoValidator.validarEliminar("VISTA_HISTORIAS");
        archivoClinicoService.eliminar(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Archivo eliminado exitosamente", null));
    }
}
