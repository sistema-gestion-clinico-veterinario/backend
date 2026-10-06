package veterinaria.vargasvet.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Reenvía al servicio de IA la consulta ya autorizada y devuelve su respuesta a medida que llega. */
@Component
public class DiagnosticoIAClient {

    public record ArchivoParte(String campo, String nombre, byte[] contenido) {}

    private final RestTemplate restTemplate;
    private final String iaUrl;

    public DiagnosticoIAClient(
            @Value("${ia.diagnostico.url}") String iaUrl,
            @Value("${http.client.connect-timeout:5s}") Duration connectTimeout,
            @Value("${ia.diagnostico.read-timeout:180s}") Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        this.restTemplate = new RestTemplate(factory);
        this.iaUrl = iaUrl;
    }

    public void transmitir(Map<String, String> campos, List<ArchivoParte> archivos, OutputStream salida) {
        MultiValueMap<String, Object> cuerpo = new LinkedMultiValueMap<>();
        campos.forEach(cuerpo::add);
        for (ArchivoParte archivo : archivos) {
            cuerpo.add(archivo.campo(), new ByteArrayResource(archivo.contenido()) {
                @Override
                public String getFilename() {
                    return archivo.nombre();
                }
            });
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        restTemplate.execute(iaUrl + "/ia/diagnostico", HttpMethod.POST,
                restTemplate.httpEntityCallback(new HttpEntity<>(cuerpo, headers)),
                respuesta -> {
                    copiar(respuesta.getBody(), salida);
                    return null;
                });
    }

    private void copiar(InputStream entrada, OutputStream salida) throws IOException {
        byte[] buffer = new byte[2048];
        int leidos;
        while ((leidos = entrada.read(buffer)) != -1) {
            salida.write(buffer, 0, leidos);
            salida.flush();
        }
    }
}
