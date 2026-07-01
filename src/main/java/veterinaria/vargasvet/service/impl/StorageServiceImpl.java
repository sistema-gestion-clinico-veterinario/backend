package veterinaria.vargasvet.service.impl;

import jakarta.annotation.PostConstruct;
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
public class StorageServiceImpl implements StorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;

    @Value("${supabase.bucket}")
    private String bucketName;

    private RestTemplate restTemplate;

    @Override
    @PostConstruct
    public void init() {
        restTemplate = new RestTemplate();
    }

    @Override
    public String store(MultipartFile file) {
        try {
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
            String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf(".")).toLowerCase() : "";
            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
            String mime = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

            String url = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + fileName;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + serviceRoleKey);
            headers.setContentType(MediaType.parseMediaType(mime));
            HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);

            return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Error al subir archivo a Supabase Storage", e);
        }
    }

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
}
