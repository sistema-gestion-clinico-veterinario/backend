package veterinaria.vargasvet.e2e;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import veterinaria.vargasvet.dto.response.LaboratorioIAResponse;
import veterinaria.vargasvet.dto.response.RadiografiaPrediccionResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/setup/e2e")
@Profile("e2e")
public class E2eSupportController {

    private final E2eFixtureRegistry fixtureRegistry;
    private final Map<String, byte[]> storedFiles = new ConcurrentHashMap<>();

    public E2eSupportController(E2eFixtureRegistry fixtureRegistry) {
        this.fixtureRegistry = fixtureRegistry;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/fixtures")
    public Map<String, Object> fixtures() {
        return fixtureRegistry.snapshot();
    }

    @PostMapping(value = "/storage/v1/object/e2e/{fileName}", consumes = MediaType.ALL_VALUE)
    public void storeFile(@PathVariable String fileName, @RequestBody byte[] content) {
        storedFiles.put(fileName, content);
    }

    @GetMapping("/storage/v1/object/public/e2e/{fileName}")
    public ResponseEntity<byte[]> getFile(@PathVariable String fileName) {
        byte[] content = storedFiles.get(fileName);
        return content == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(content);
    }

    @PostMapping(value = "/ia/laboratorio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LaboratorioIAResponse laboratorio(
            @RequestPart("archivo") MultipartFile archivo,
            @RequestParam("especie") String especie
    ) {
        LaboratorioIAResponse response = new LaboratorioIAResponse();
        response.setFuente("stub-e2e");
        response.setTipo("laboratorio");
        response.setEspecie(especie);
        response.setSeccionesPresentes(List.of("hematologia"));
        response.setComentariosClinicos(List.of("Correlacionar con la evaluacion clinica"));
        response.setAlertas(List.of("Leucocitos ligeramente elevados"));
        return response;
    }

    @PostMapping(value = "/predict/radiografia", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RadiografiaPrediccionResponse radiografia(@RequestPart("file") MultipartFile file) {
        RadiografiaPrediccionResponse response = new RadiografiaPrediccionResponse();
        response.setModel("rx-vargasvet-e2e");
        response.setFileType("png");
        response.setPredictions(Map.of());
        response.setDiagnoses(List.of("Sin hallazgos oseos agudos"));
        response.setInferenceMs(18.5);
        return response;
    }

    @PostMapping(value = "/ia/diagnostico", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public String diagnostico() {
        return String.join("\n",
                "data: {\"type\":\"meta\",\"escenario\":\"HC_Radiografia\",\"modelo\":\"ia-e2e\",\"image_quality\":{\"parece_radiografia\":true,\"issues\":[]}}",
                "",
                "data: {\"type\":\"chunk\",\"text\":\"Analisis E2E: sin hallazgos oseos agudos.\"}",
                "",
                "data: {\"type\":\"done\"}",
                "");
    }
}
