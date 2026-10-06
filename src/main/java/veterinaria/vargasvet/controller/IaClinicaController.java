package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.response.AutorizacionIaResponse;
import veterinaria.vargasvet.integration.DiagnosticoIAClient;
import veterinaria.vargasvet.integration.DiagnosticoIAClient.ArchivoParte;
import veterinaria.vargasvet.service.AutorizacionIaService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/ia")
@RequiredArgsConstructor
public class IaClinicaController {

    private static final Set<String> CAMPOS_PERMITIDOS =
            Set.of("motivo_consulta", "especie", "edad", "nombre_paciente", "sexo", "peso");

    private final AutorizacionIaService autorizacionIaService;
    private final DiagnosticoIAClient diagnosticoIAClient;

    @GetMapping("/autorizacion/mascotas/{mascotaId}")
    @PreAuthorize("@accesoValidator.can('VISTA_HISTORIAS', 'LEER') or @accesoValidator.can('VISTA_LABORATORIO', 'LEER')")
    public ResponseEntity<ApiResponse<AutorizacionIaResponse>> autorizacion(@PathVariable Long mascotaId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Autorización de IA", autorizacionIaService.estado(mascotaId)));
    }

    @PostMapping(value = "/diagnostico", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@accesoValidator.can('VISTA_HISTORIAS', 'LEER')")
    public ResponseEntity<StreamingResponseBody> diagnostico(
            @RequestParam("mascotaId") Long mascotaId,
            @RequestParam Map<String, String> campos,
            @RequestParam(value = "archivo_hemograma", required = false) List<MultipartFile> hemogramas,
            @RequestParam(value = "archivo_radiografia", required = false) List<MultipartFile> radiografias)
            throws IOException {
        AutorizacionIaResponse autorizacion = autorizacionIaService.exigir(mascotaId);

        Map<String, String> permitidos = new LinkedHashMap<>();
        campos.forEach((clave, valor) -> {
            if (CAMPOS_PERMITIDOS.contains(clave)) {
                permitidos.put(clave, valor);
            }
        });
        List<ArchivoParte> archivos = new ArrayList<>();
        agregar(archivos, "archivo_hemograma", hemogramas);
        agregar(archivos, "archivo_radiografia", radiografias);

        autorizacionIaService.registrarUso(mascotaId, "diagnóstico", autorizacion);

        StreamingResponseBody cuerpo = salida -> {
            try {
                diagnosticoIAClient.transmitir(permitidos, archivos, salida);
            } catch (RuntimeException ex) {
                log.warn("El servicio de IA no pudo responder el diagnóstico: {}", ex.getMessage());
                salida.write(("data: {\"type\":\"error\"}\n\n").getBytes(StandardCharsets.UTF_8));
                salida.flush();
            }
        };
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(cuerpo);
    }

    private void agregar(List<ArchivoParte> destino, String campo, List<MultipartFile> origen) throws IOException {
        if (origen == null) {
            return;
        }
        for (MultipartFile archivo : origen) {
            if (!archivo.isEmpty()) {
                destino.add(new ArchivoParte(campo, archivo.getOriginalFilename(), archivo.getBytes()));
            }
        }
    }
}
