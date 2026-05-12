package veterinaria.vargasvet.shared;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;

public interface StorageService {
    void init();
    String store(MultipartFile file);
    String storeBytes(byte[] content, String extension);
    Path load(String filename);
    Resource loadAsResource(String filename);
    void delete(String filename);
}
