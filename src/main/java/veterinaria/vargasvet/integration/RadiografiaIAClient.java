package veterinaria.vargasvet.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import veterinaria.vargasvet.dto.response.RadiografiaPrediccionResponse;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RadiografiaIAClient {

    private final RestTemplate restTemplate;

    @Value("${ia.radiografia.url}")
    private String iaUrl;

    public RadiografiaPrediccionResponse predict(MultipartFile file) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<RadiografiaPrediccionResponse> response = restTemplate.exchange(
                iaUrl + "/predict/radiografia",
                HttpMethod.POST,
                request,
                RadiografiaPrediccionResponse.class
        );

        return response.getBody();
    }
}
