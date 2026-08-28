package veterinaria.vargasvet.controller;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import veterinaria.vargasvet.dto.UploadMediaDTO;
import veterinaria.vargasvet.service.StorageService;

@RequiredArgsConstructor
@RequestMapping("/media")
@RestController
public class MediaController {

    private final StorageService storageService;
    private final MeterRegistry meterRegistry;

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public UploadMediaDTO uploadMedia(
            @RequestParam("file") MultipartFile multipartFile,
            @RequestParam(value = "originalSize", required = false) Long originalSize) {
        String url = storageService.store(multipartFile);
        recordOriginalBytes(originalSize, multipartFile.getSize());
        return new UploadMediaDTO(url);
    }

    private void recordOriginalBytes(Long reportedOriginalBytes, long storedBytes) {
        // El valor del cliente se usa solo para observabilidad, nunca para autorización.
        long originalBytes = reportedOriginalBytes != null
                && reportedOriginalBytes >= storedBytes
                && reportedOriginalBytes <= 5L * 1024 * 1024
                ? reportedOriginalBytes
                : storedBytes;

        DistributionSummary.builder("systemvet.media.upload.original.bytes")
                .description("Bytes de las imágenes de presentación antes de optimizarlas")
                .baseUnit("bytes")
                .register(meterRegistry)
                .record(originalBytes);
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Void> getResource(@PathVariable String filename) {
        return ResponseEntity.notFound().build();
    }
}
