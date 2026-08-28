package veterinaria.vargasvet.service.impl;

import jakarta.annotation.PostConstruct;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import veterinaria.vargasvet.service.StorageService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private static final long MAX_MEDIA_BYTES = 5L * 1024 * 1024;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;

    @Value("${supabase.bucket}")
    private String bucketName;

    private final MeterRegistry meterRegistry;

    private RestTemplate restTemplate;

    @Override
    @PostConstruct
    public void init() {
        restTemplate = new RestTemplate();
    }

    @Override
    public String store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("El archivo está vacío");
            }
            if (file.getSize() > MAX_MEDIA_BYTES) {
                throw new IllegalArgumentException("La imagen supera el tamaño máximo permitido de 5 MB");
            }
            byte[] content = file.getBytes();
            DetectedImage image = detectImage(content);
            if (image == null) {
                throw new IllegalArgumentException("Solo se permiten imágenes JPEG, PNG o WebP válidas");
            }
            String ext = image.extension();
            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
            String mime = image.mimeType();

            String url = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + fileName;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + serviceRoleKey);
            headers.setContentType(MediaType.parseMediaType(mime));
            HttpEntity<byte[]> entity = new HttpEntity<>(content, headers);
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
            recordStoredBytes("media", content.length);

            return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Error al subir archivo a Supabase Storage", e);
        }
    }

    private DetectedImage detectImage(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return null;
        if ((bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) {
            return new DetectedImage(".jpg", "image/jpeg");
        }
        if ((bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47
                && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) {
            return new DetectedImage(".png", "image/png");
        }
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return new DetectedImage(".webp", "image/webp");
        }
        return null;
    }

    private record DetectedImage(String extension, String mimeType) {}

    @Override
    public String storeBytes(byte[] content, String extension) {
        return storeBytes(content, extension, null, null);
    }

    @Override
    public String storeBytes(byte[] content, String extension, String mimeType, String originalFilename) {
        String ext = extension != null ? extension.toLowerCase() : "";
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        String effectiveMime = mimeType != null ? mimeType : "application/octet-stream";

        String url = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + fileName;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceRoleKey);
        headers.setContentType(MediaType.parseMediaType(effectiveMime));
        HttpEntity<byte[]> entity = new HttpEntity<>(content, headers);
        restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
        recordStoredBytes("clinical", content.length);

        return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + fileName;
    }

    @Override
    public void delete(String url) {
        String fileName = extractFileName(url);
        String deleteUrl = supabaseUrl + "/storage/v1/object/" + bucketName;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceRoleKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, List<String>>> entity = new HttpEntity<>(
                Map.of("prefixes", List.of(fileName)), headers);
        restTemplate.exchange(deleteUrl, HttpMethod.DELETE, entity, Void.class);
    }

    @Override
    public byte[] fetch(String url) {
        java.net.URI requested = java.net.URI.create(url);
        java.net.URI storageBase = java.net.URI.create(supabaseUrl);
        String expectedPrefix = "/storage/v1/object/public/" + bucketName + "/";
        boolean localStorage = "localhost".equalsIgnoreCase(storageBase.getHost())
                || "127.0.0.1".equals(storageBase.getHost());
        boolean secureScheme = "https".equalsIgnoreCase(requested.getScheme())
                || (localStorage && "http".equalsIgnoreCase(requested.getScheme()));
        if (!secureScheme
                || !java.util.Objects.equals(requested.getScheme(), storageBase.getScheme())
                || !java.util.Objects.equals(requested.getHost(), storageBase.getHost())
                || requested.getPort() != storageBase.getPort()
                || requested.normalize().getPath() == null
                || !requested.normalize().getPath().startsWith(expectedPrefix)) {
            throw new IllegalArgumentException("URL de almacenamiento no permitida");
        }
        return restTemplate.getForObject(url, byte[].class);
    }

    @Override
    public Path load(String filename) {
        throw new UnsupportedOperationException("Archivos servidos directamente desde Supabase Storage");
    }

    @Override
    public Resource loadAsResource(String filename) {
        throw new UnsupportedOperationException("Archivos servidos directamente desde Supabase Storage");
    }

    private String extractFileName(String url) {
        String prefix = "/storage/v1/object/public/" + bucketName + "/";
        int idx = url.indexOf(prefix);
        if (idx == -1) return url;
        return url.substring(idx + prefix.length());
    }

    private void recordStoredBytes(String category, int bytes) {
        DistributionSummary.builder("systemvet.storage.upload.bytes")
                .description("Bytes almacenados correctamente en el proveedor de archivos")
                .baseUnit("bytes")
                .tag("category", category)
                .register(meterRegistry)
                .record(bytes);
    }
}
