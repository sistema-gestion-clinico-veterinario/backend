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
import veterinaria.vargasvet.service.ArchivoClinicoService;

import java.util.List;

@RestController
@RequestMapping("/consultas/{consultaId}/archivos")
@RequiredArgsConstructor
public class ArchivoClinicoController {

    private final ArchivoClinicoService archivoClinicoService;

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO')")
    public ResponseEntity<ApiResponse<ArchivoClinicoResponse>> subirArchivo(
            @PathVariable Long consultaId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("tipo") TipoArchivo tipo,
            @RequestParam(value = "descripcion", required = false) String descripcion) {

        ArchivoClinicoResponse response = archivoClinicoService.subirArchivo(consultaId, file, tipo, descripcion);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Archivo cargado exitosamente", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<ApiResponse<List<ArchivoClinicoResponse>>> listarArchivos(@PathVariable Long consultaId) {
        List<ArchivoClinicoResponse> archivos = archivoClinicoService.listarPorConsulta(consultaId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Archivos recuperados con éxito", archivos));
    }

    @GetMapping("/{id}/contenido")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<Resource> servirContenido(
            @PathVariable Long consultaId,
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean descargar) {
        ArchivoClinicoResponse meta = archivoClinicoService.obtenerPorId(id);
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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO')")
    public ResponseEntity<ApiResponse<Void>> eliminar(
            @PathVariable Long consultaId,
            @PathVariable Long id) {
        archivoClinicoService.eliminar(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Archivo eliminado exitosamente", null));
    }
}
