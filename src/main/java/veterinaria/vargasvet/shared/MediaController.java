package veterinaria.vargasvet.shared;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import veterinaria.vargasvet.shared.UploadMediaDTO;
import veterinaria.vargasvet.shared.StorageService;

import java.io.IOException;
import java.nio.file.Files;

@RequiredArgsConstructor
@RequestMapping("/media")
@RestController
public class MediaController {

    private final StorageService storageService;

    @PostMapping("/upload")
    public UploadMediaDTO uploadMedia(@RequestParam("file") MultipartFile multipartFile) {
        String path = storageService.store(multipartFile);
        return new UploadMediaDTO(path);
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getResource(@PathVariable String filename) throws IOException {
        Resource resource = storageService.loadAsResource(filename);

        String contentType = Files.probeContentType(resource.getFile().toPath());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(resource);
    }
}
