package veterinaria.vargasvet.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import veterinaria.vargasvet.service.StorageService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@Service
public class StorageServiceImpl implements StorageService {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    private Cloudinary cloudinary;

    @Override
    @PostConstruct
    public void init() {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key",    apiKey,
                "api_secret", apiSecret,
                "secure",     true
        ));
    }

    @Override
    public String store(MultipartFile file) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("resource_type", "auto"));
            return (String) result.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Error al subir archivo a Cloudinary", e);
        }
    }

    @Override
    public String storeBytes(byte[] content, String extension) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(content,
                    ObjectUtils.asMap("resource_type", "auto"));
            return (String) result.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Error al subir archivo a Cloudinary", e);
        }
    }

    @Override
    public void delete(String url) {
        try {
            String publicId = extractPublicId(url);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new RuntimeException("Error al eliminar archivo de Cloudinary", e);
        }
    }

    @Override
    public Path load(String filename) {
        throw new UnsupportedOperationException("Archivos servidos directamente desde Cloudinary");
    }

    @Override
    public Resource loadAsResource(String filename) {
        throw new UnsupportedOperationException("Archivos servidos directamente desde Cloudinary");
    }

    private String extractPublicId(String url) {
        String[] parts = url.split("/upload/");
        if (parts.length < 2) return url;
        String afterUpload = parts[1].replaceFirst("v\\d+/", "");
        String resourceType = url.contains("/raw/upload/") ? "raw" : "image";
        int dot = afterUpload.lastIndexOf('.');
        String publicId = dot > 0 ? afterUpload.substring(0, dot) : afterUpload;
        return resourceType.equals("raw") ? publicId + "." + afterUpload.substring(dot + 1) : publicId;
    }
}
