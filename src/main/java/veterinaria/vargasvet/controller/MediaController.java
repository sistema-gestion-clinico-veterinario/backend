package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import veterinaria.vargasvet.dto.UploadMediaDTO;
import veterinaria.vargasvet.service.StorageService;

@RequiredArgsConstructor
@RequestMapping("/media")
@RestController
public class MediaController {

    private final StorageService storageService;

    @PostMapping("/upload")
    public UploadMediaDTO uploadMedia(@RequestParam("file") MultipartFile multipartFile) {
        String url = storageService.store(multipartFile);
        return new UploadMediaDTO(url);
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Void> getResource(@PathVariable String filename) {
        return ResponseEntity.notFound().build();
    }
}
