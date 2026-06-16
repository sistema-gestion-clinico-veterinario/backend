package veterinaria.vargasvet.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;

public interface StorageService {
    void init();
    String store(MultipartFile file);
    String storeBytes(byte[] content, String extension);
    String storeBytes(byte[] content, String extension, String mimeType, String originalFilename);
    Path load(String filename);
    Resource loadAsResource(String filename);
    void delete(String filename);
}
