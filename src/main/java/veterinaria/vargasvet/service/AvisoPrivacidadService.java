package veterinaria.vargasvet.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.AvisoPrivacidad;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.dto.request.CamposAvisoPrivacidad;
import veterinaria.vargasvet.dto.request.PublicarAvisoPrivacidadRequest;
import veterinaria.vargasvet.dto.response.AvisoPrivacidadResponse;
import veterinaria.vargasvet.dto.response.AvisoPublicoResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.AvisoPrivacidadRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.util.AppClock;
import veterinaria.vargasvet.util.AvisoPrivacidadTexto;
import veterinaria.vargasvet.util.LegalText;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvisoPrivacidadService {

    private final AvisoPrivacidadRepository avisoRepository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Optional<AvisoPrivacidad> vigente(Integer companyId) {
        return avisoRepository.findByCompanyIdAndActivoTrue(companyId);
    }

    @Transactional(readOnly = true)
    public AvisoPublicoResponse publico(String slug) {
        Company company = companyRepository.findBySlug(slug == null ? "" : slug.trim().toLowerCase(java.util.Locale.ROOT))
                .filter(Company::isActivo)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos el aviso de privacidad"));
        AvisoPrivacidad aviso = avisoRepository.findByCompanyIdAndActivoTrue(company.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Esta clínica aún no publicó su aviso de privacidad"));
        return new AvisoPublicoResponse(company.getName(), company.getLogoUrl(), company.getColorPrimario(),
                aviso.getVersion(), aviso.getVigenteDesde(), aviso.getContenido());
    }

    @Transactional(readOnly = true)
    public AvisoPublicoResponse vigenteDeLaClinica(Integer companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        return avisoRepository.findByCompanyIdAndActivoTrue(companyId)
                .map(aviso -> new AvisoPublicoResponse(company.getName(), company.getLogoUrl(), company.getColorPrimario(),
                        aviso.getVersion(), aviso.getVigenteDesde(), aviso.getContenido()))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<AvisoPrivacidadResponse> historial(Integer companyId) {
        return avisoRepository.findByCompanyIdOrderByVersionDesc(requerirEmpresa(companyId)).stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public CamposAvisoPrivacidad plantilla(Integer companyId) {
        Integer id = requerirEmpresa(companyId);
        Optional<AvisoPrivacidad> vigente = avisoRepository.findByCompanyIdAndActivoTrue(id);
        if (vigente.isPresent()) {
            return leerCampos(vigente.get());
        }
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        CamposAvisoPrivacidad campos = new CamposAvisoPrivacidad();
        campos.setRazonSocial(company.getName());
        campos.setRuc(company.getRuc());
        campos.setDomicilio(company.getAddress());
        campos.setCorreoDerechos(company.getEmail());
        campos.setEncargadoTratamiento("VetSoft, proveedor de la plataforma de gestión que aloja la información");
        campos.setFinalidades(new ArrayList<>(List.of(
                "Registrarle como cliente y gestionar la atención veterinaria de su mascota",
                "Programar sus citas y avisarle de ellas",
                "Emitir comprobantes y gestionar pagos y cobranzas",
                "Mantener el historial clínico de su mascota y comunicarle información sobre ella")));
        campos.setDatosObligatorios(new ArrayList<>(List.of(
                "Nombres y apellidos", "Tipo y número de documento de identidad", "Correo electrónico",
                "Teléfono", "Dirección", "Género")));
        campos.setDatosFacultativos(new ArrayList<>(List.of("Referencias", "Observaciones")));
        campos.setDestinatarios(new ArrayList<>(List.of(
                "El personal y los veterinarios de la clínica que atienden a su mascota",
                "VetSoft, como encargado del tratamiento por alojar la información")));
        return campos;
    }

    @Transactional
    public AvisoPrivacidadResponse publicar(Integer companyId, PublicarAvisoPrivacidadRequest request) {
        Integer id = requerirEmpresa(companyId);
        companyRepository.lockById(id).orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        CamposAvisoPrivacidad campos = AvisoPrivacidadTexto.limpiar(request.getCampos());
        verificarCompletos(campos);

        Optional<AvisoPrivacidad> vigente = avisoRepository.findByCompanyIdAndActivoTrue(id);
        if (vigente.isPresent() && campos.equals(leerCampos(vigente.get()))) {
            throw new IllegalArgumentException("No hay cambios respecto al aviso vigente; no hay nada que publicar");
        }

        int version = avisoRepository.ultimaVersion(id) + 1;
        String texto = LegalText.normalize(AvisoPrivacidadTexto.componer(campos, version));
        Set<String> borradores = LegalText.draftMarkers(texto);
        if (!borradores.isEmpty()) {
            throw new IllegalArgumentException("El aviso tiene campos sin completar o texto de borrador ("
                    + borradores.stream().limit(3).collect(Collectors.joining(", "))
                    + "). Complétalos antes de publicarlo.");
        }

        vigente.ifPresent(anterior -> {
            anterior.setActivo(false);
            avisoRepository.saveAndFlush(anterior);
        });

        java.time.LocalDateTime ahora = AppClock.now();
        AvisoPrivacidad nuevo = new AvisoPrivacidad();
        nuevo.setCompany(companyRepository.getReferenceById(id));
        nuevo.setVersion(version);
        nuevo.setContenido(texto);
        nuevo.setContenidoHash(LegalText.sha256Hex(texto));
        nuevo.setCampos(escribirCampos(campos));
        nuevo.setVigenteDesde(ahora);
        nuevo.setCreadoEn(ahora);
        nuevo.setCreadoPor(SecurityUtils.getCurrentUserEmail());
        nuevo.setActivo(true);
        AvisoPrivacidad guardado;
        try {
            guardado = avisoRepository.saveAndFlush(nuevo);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Otra publicación del aviso se acaba de realizar. Actualiza la pantalla y revisa la versión vigente.");
        }

        auditLogService.log("PUBLICAR_AVISO_PRIVACIDAD", "Seguridad",
                "Se publicó la versión " + version + " del aviso de privacidad"
                        + vigente.map(anterior -> " (reemplaza la " + anterior.getVersion() + ")").orElse("")
                        + ". Huella " + guardado.getContenidoHash());
        return aRespuesta(guardado);
    }

    private void verificarCompletos(CamposAvisoPrivacidad campos) {
        if (campos.getRazonSocial().isEmpty() || campos.getRuc().isEmpty() || campos.getDomicilio().isEmpty()
                || campos.getCorreoDerechos().isEmpty() || campos.getTransferencias().isEmpty()
                || campos.getPlazoConservacion().isEmpty()) {
            throw new IllegalArgumentException("Completa todos los campos obligatorios del aviso");
        }
        if (campos.getFinalidades().isEmpty() || campos.getDatosObligatorios().isEmpty() || campos.getDestinatarios().isEmpty()) {
            throw new IllegalArgumentException("Indica al menos una finalidad, un dato obligatorio y un destinatario");
        }
    }

    private Integer requerirEmpresa(Integer companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Esta acción corresponde a una clínica; ingresa con la cuenta de la clínica");
        }
        return companyId;
    }

    private AvisoPrivacidadResponse aRespuesta(AvisoPrivacidad aviso) {
        return new AvisoPrivacidadResponse(aviso.getVersion(), aviso.getContenido(), aviso.getContenidoHash(),
                aviso.getVigenteDesde(), aviso.isActivo(), aviso.getCreadoPor(), leerCampos(aviso));
    }

    private CamposAvisoPrivacidad leerCampos(AvisoPrivacidad aviso) {
        try {
            return objectMapper.readValue(aviso.getCampos(), CamposAvisoPrivacidad.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudieron leer los datos del aviso", e);
        }
    }

    private String escribirCampos(CamposAvisoPrivacidad campos) {
        try {
            return objectMapper.writeValueAsString(campos);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudieron guardar los datos del aviso", e);
        }
    }
}
