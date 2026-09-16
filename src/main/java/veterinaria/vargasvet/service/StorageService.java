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

    /**
     * Genera una URL firmada de corta duracion para acceder a un archivo del bucket
     * privado directamente desde Supabase (sin proxear los bytes por el backend).
     * downloadFilename != null fuerza Content-Disposition: attachment con ese nombre.
     */
    String createSignedUrl(String url, int expiresInSeconds, String downloadFilename);
}
