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
import veterinaria.vargasvet.domain.enums.DiaSemana;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyOperatingHourRepository;
import veterinaria.vargasvet.repository.ConsultaRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/setup/e2e")
@Profile("e2e")
public class E2eSupportController {

    private final E2eFixtureRegistry fixtureRegistry;
    private final CompanyOperatingHourRepository operatingHourRepository;
    private final UsuarioRepository usuarioRepository;
    private final CitaRepository citaRepository;
    private final ConsultaRepository consultaRepository;
    private final Map<String, byte[]> storedFiles = new ConcurrentHashMap<>();

    public E2eSupportController(
            E2eFixtureRegistry fixtureRegistry,
            CompanyOperatingHourRepository operatingHourRepository,
            UsuarioRepository usuarioRepository,
            CitaRepository citaRepository,
            ConsultaRepository consultaRepository
    ) {
        this.fixtureRegistry = fixtureRegistry;
        this.operatingHourRepository = operatingHourRepository;
        this.usuarioRepository = usuarioRepository;
        this.citaRepository = citaRepository;
        this.consultaRepository = consultaRepository;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/fixtures")
    public Map<String, Object> fixtures() {
        return fixtureRegistry.snapshot();
    }

    @PostMapping("/scenario/clinic-closed-today")
    public Map<String, Object> setClinicClosedToday(@RequestParam boolean closed) {
        Integer companyId = ((Number) fixtureRegistry.snapshot().get("companyId")).intValue();
        DiaSemana today = switch (veterinaria.vargasvet.util.AppClock.today().getDayOfWeek()) {
            case MONDAY -> DiaSemana.LUNES;
            case TUESDAY -> DiaSemana.MARTES;
            case WEDNESDAY -> DiaSemana.MIERCOLES;
            case THURSDAY -> DiaSemana.JUEVES;
            case FRIDAY -> DiaSemana.VIERNES;
            case SATURDAY -> DiaSemana.SABADO;
            case SUNDAY -> DiaSemana.DOMINGO;
        };
        var hours = operatingHourRepository.findByCompanyIdAndDiaSemana(companyId, today).orElseThrow();
        hours.setIsOpen(!closed);
        operatingHourRepository.save(hours);
        return Map.of("closed", closed, "day", today.name());
    }

    @PostMapping("/scenario/reset-activation")
    public Map<String, Object> resetActivationUser() {
        String email = (String) fixtureRegistry.snapshot().get("activationEmail");
        String token = "selenium-e2e-activation-token";
        var user = usuarioRepository.findByEmail(email).orElseThrow();
        user.setActivo(false);
        user.setEmailVerified(false);
        user.setPasswordChanged(false);
        user.setVerificationToken(token);
        usuarioRepository.save(user);
        fixtureRegistry.put("activationToken", token);
        return Map.of("email", email, "token", token);
    }

    @PostMapping("/scenario/reset-clinical-appointment")
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> resetClinicalAppointment(@RequestParam String fixtureKey) {
        Object rawAppointmentId = fixtureRegistry.snapshot().get(fixtureKey);
        if (!(rawAppointmentId instanceof Number appointmentId)) {
            throw new IllegalArgumentException("Fixture clínico desconocido: " + fixtureKey);
        }

        var appointment = citaRepository.findById(appointmentId.longValue()).orElseThrow();
        consultaRepository.findByCitaId(appointment.getId()).ifPresent(consultaRepository::delete);
        consultaRepository.flush();
        appointment.setConsulta(null);
        appointment.setEstado(EstadoCita.PROGRAMADA);
        citaRepository.saveAndFlush(appointment);
        return Map.of("fixtureKey", fixtureKey, "appointmentId", appointment.getId(), "status", "PROGRAMADA");
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
