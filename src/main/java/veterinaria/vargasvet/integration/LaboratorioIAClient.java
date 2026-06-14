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
import veterinaria.vargasvet.dto.response.LaboratorioIAResponse;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LaboratorioIAClient {

    private final RestTemplate restTemplate;

    @Value("${ia.laboratorio.url}")
    private String iaUrl;

    public LaboratorioIAResponse analizar(MultipartFile archivo, String especie) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ByteArrayResource fileResource = new ByteArrayResource(archivo.getBytes()) {
            @Override
            public String getFilename() {
                return archivo.getOriginalFilename();
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("archivo", fileResource);
        body.add("especie", especie);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<LaboratorioIAResponse> response = restTemplate.exchange(
                iaUrl + "/ia/laboratorio",
                HttpMethod.POST,
                request,
                LaboratorioIAResponse.class
        );

        return response.getBody();
    }
}
