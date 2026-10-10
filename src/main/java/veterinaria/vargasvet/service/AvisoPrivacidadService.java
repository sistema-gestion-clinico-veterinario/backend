package veterinaria.vargasvet.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.AvisoPrivacidad;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.enums.AudienciaAvisoPrivacidad;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.dto.request.CamposAvisoPrivacidad;
import veterinaria.vargasvet.dto.request.PublicarAvisoPrivacidadRequest;
import veterinaria.vargasvet.dto.request.VistaPreviaAvisoRequest;
import veterinaria.vargasvet.dto.response.AvisoPrivacidadResponse;
import veterinaria.vargasvet.dto.response.AvisoPublicoResponse;
import veterinaria.vargasvet.dto.response.VistaPreviaAvisoResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.AvisoPrivacidadRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.util.AppClock;
import veterinaria.vargasvet.util.AvisoPrivacidadTexto;
import veterinaria.vargasvet.util.DispositivoInfo;
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
    @Autowired(required = false)
    private UsuarioPorRolRepository usuarioPorRolRepository;
    @Autowired(required = false)
    private AvisoPrivacidadEntregaService entregaAvisoService;

    @Transactional(readOnly = true)
    public Optional<AvisoPrivacidad> vigente(Integer companyId) {
        return vigente(companyId, AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS);
    }

    @Transactional(readOnly = true)
    public Optional<AvisoPrivacidad> vigente(Integer companyId, AudienciaAvisoPrivacidad audiencia) {
        return avisoRepository.findByCompanyIdAndAudienciaAndActivoTrue(companyId, audiencia);
    }

    @Transactional(readOnly = true)
    public AvisoPublicoResponse publico(String slug) {
        return publico(slug, AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS);
    }

    @Transactional(readOnly = true)
    public AvisoPublicoResponse publico(String slug, AudienciaAvisoPrivacidad audiencia) {
        Company company = companyRepository.findBySlug(slug == null ? "" : slug.trim().toLowerCase(java.util.Locale.ROOT))
                .filter(Company::isActivo)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos el aviso de privacidad"));
        AvisoPrivacidad aviso = avisoRepository.findByCompanyIdAndAudienciaAndActivoTrue(company.getId(), audiencia)
                .orElseThrow(() -> new ResourceNotFoundException("Esta clínica aún no publicó su aviso de privacidad"));
        return new AvisoPublicoResponse(company.getName(), company.getLogoUrl(), company.getColorPrimario(), aviso.getAudiencia(),
                aviso.getVersion(), aviso.getVigenteDesde(), aviso.getContenido());
    }

    @Transactional(readOnly = true)
    public AvisoPublicoResponse publico(String slug, AudienciaAvisoPrivacidad audiencia, Integer version) {
        Company company = companyRepository.findBySlug(slug == null ? "" : slug.trim().toLowerCase(java.util.Locale.ROOT))
                .filter(Company::isActivo)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos el aviso de privacidad"));
        AvisoPrivacidad aviso = avisoRepository
                .findByCompanyIdAndAudienciaAndVersion(company.getId(), audiencia, version)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos esa versión del aviso de privacidad"));
        return new AvisoPublicoResponse(company.getName(), company.getLogoUrl(), company.getColorPrimario(),
                aviso.getAudiencia(), aviso.getVersion(), aviso.getVigenteDesde(), aviso.getContenido());
    }

    @Transactional(readOnly = true)
    public AvisoPublicoResponse vigenteDeLaClinica(Integer companyId) {
        return vigenteDeLaClinica(companyId, AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS);
    }

    @Transactional(readOnly = true)
    public AvisoPublicoResponse vigenteDeLaClinica(Integer companyId, AudienciaAvisoPrivacidad audiencia) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        return avisoRepository.findByCompanyIdAndAudienciaAndActivoTrue(companyId, audiencia)
                .map(aviso -> new AvisoPublicoResponse(company.getName(), company.getLogoUrl(), company.getColorPrimario(),
                        aviso.getAudiencia(), aviso.getVersion(), aviso.getVigenteDesde(), aviso.getContenido()))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<AvisoPrivacidadResponse> historial(Integer companyId) {
        return historial(companyId, AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS);
    }

    @Transactional(readOnly = true)
    public List<AvisoPrivacidadResponse> historial(Integer companyId, AudienciaAvisoPrivacidad audiencia) {
        return avisoRepository.findByCompanyIdAndAudienciaOrderByVersionDesc(requerirEmpresa(companyId), audiencia).stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public CamposAvisoPrivacidad plantilla(Integer companyId) {
        return plantilla(companyId, AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS);
    }

    @Transactional(readOnly = true)
    public CamposAvisoPrivacidad plantilla(Integer companyId, AudienciaAvisoPrivacidad audiencia) {
        Integer id = requerirEmpresa(companyId);
        Optional<AvisoPrivacidad> vigente = avisoRepository.findByCompanyIdAndAudienciaAndActivoTrue(id, audiencia);
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
        if (audiencia == AudienciaAvisoPrivacidad.TRABAJADORES_Y_USUARIOS) {
            campos.setFinalidades(new ArrayList<>(List.of(
                    "Gestionar su vínculo laboral o profesional con la clínica",
                    "Asignar roles, permisos y credenciales de acceso al sistema",
                    "Organizar horarios, funciones y actividades del personal",
                    "Cumplir obligaciones administrativas, contractuales y legales")));
            campos.setDatosObligatorios(new ArrayList<>(List.of(
                    "Nombres y apellidos", "Tipo y número de documento de identidad", "Correo electrónico",
                    "Teléfono", "Dirección", "Cargo, rol y permisos asignados")));
            campos.setDatosFacultativos(new ArrayList<>(List.of(
                    "Fotografía", "Especialidades", "Número de colegiatura", "Observaciones")));
            campos.setDestinatarios(new ArrayList<>(List.of(
                    "El personal autorizado de la clínica encargado de la gestión administrativa",
                    "VetSoft, como encargado del tratamiento por alojar la información")));
        } else {
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
        }
        return campos;
    }

    @Transactional(readOnly = true)
    public VistaPreviaAvisoResponse vistaPrevia(Integer companyId, VistaPreviaAvisoRequest request) {
        Integer id = requerirEmpresa(companyId);
        AudienciaAvisoPrivacidad audiencia = request.getAudiencia();
        CamposAvisoPrivacidad campos = AvisoPrivacidadTexto.limpiar(request.getCampos());
        Optional<AvisoPrivacidad> vigente = avisoRepository.findByCompanyIdAndAudienciaAndActivoTrue(id, audiencia);
        int version = avisoRepository.ultimaVersion(id, audiencia) + 1;
        String texto = LegalText.normalize(AvisoPrivacidadTexto.componer(campos, version, audiencia));

        List<String> observaciones = new ArrayList<>();
        if (campos.getRazonSocial().isEmpty()) observaciones.add("Falta la razón social");
        if (campos.getRuc().isEmpty()) observaciones.add("Falta el RUC");
        if (campos.getDomicilio().isEmpty()) observaciones.add("Falta el domicilio");
        if (campos.getCorreoDerechos().isEmpty()) observaciones.add("Falta el correo para ejercer derechos");
        if (campos.getFinalidades().isEmpty()) observaciones.add("Indica al menos una finalidad");
        if (campos.getDatosObligatorios().isEmpty()) observaciones.add("Indica los datos obligatorios");
        if (campos.getDestinatarios().isEmpty()) observaciones.add("Indica a quiénes se comunican los datos");
        if (campos.getTransferencias().isEmpty()) observaciones.add("Indica si hay transferencias de datos");
        if (campos.getPlazoConservacion().isEmpty()) observaciones.add("Indica el plazo de conservación");
        LegalText.draftMarkers(texto).stream().limit(5)
                .forEach(marca -> observaciones.add("Texto de borrador o por completar: " + marca));

        boolean sinCambios = vigente.isPresent() && campos.equals(leerCampos(vigente.get()));
        return new VistaPreviaAvisoResponse(version, texto, sinCambios, observaciones);
    }

    @Transactional
    public AvisoPrivacidadResponse publicar(Integer companyId, PublicarAvisoPrivacidadRequest request) {
        return publicar(companyId, request, null, null);
    }

    @Transactional
    public AvisoPrivacidadResponse publicar(Integer companyId, PublicarAvisoPrivacidadRequest request,
                                            String ip, String userAgent) {
        Integer id = requerirEmpresa(companyId);
        companyRepository.lockById(id).orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        AudienciaAvisoPrivacidad audiencia = request.getAudiencia();

        CamposAvisoPrivacidad campos = AvisoPrivacidadTexto.limpiar(request.getCampos());
        verificarCompletos(campos);

        Optional<AvisoPrivacidad> vigente = avisoRepository.findByCompanyIdAndAudienciaAndActivoTrue(id, audiencia);
        if (vigente.isPresent() && campos.equals(leerCampos(vigente.get()))) {
            throw new IllegalArgumentException("No hay cambios respecto al aviso vigente; no hay nada que publicar");
        }

        int version = avisoRepository.ultimaVersion(id, audiencia) + 1;
        String texto = LegalText.normalize(AvisoPrivacidadTexto.componer(campos, version, audiencia));
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
        nuevo.setAudiencia(audiencia);
        nuevo.setVersion(version);
        nuevo.setContenido(texto);
        nuevo.setContenidoHash(LegalText.sha256Hex(texto));
        nuevo.setCampos(escribirCampos(campos));
        nuevo.setVigenteDesde(ahora);
        nuevo.setCreadoEn(ahora);
        nuevo.setCreadoPor(SecurityUtils.getCurrentUserEmail());
        String dispositivo = ip == null && userAgent == null ? null : DispositivoInfo.describir(userAgent);
        nuevo.setCreadoIp(ip == null ? null : (ip.length() > 64 ? ip.substring(0, 64) : ip));
        nuevo.setCreadoDispositivo(dispositivo);
        nuevo.setActivo(true);
        AvisoPrivacidad guardado;
        try {
            guardado = avisoRepository.saveAndFlush(nuevo);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Otra publicación del aviso se acaba de realizar. Actualiza la pantalla y revisa la versión vigente.");
        }

        auditLogService.log("PUBLICAR_AVISO_PRIVACIDAD", "Seguridad",
                "Se publicó la versión " + version + " del aviso de privacidad para " + audiencia.name()
                        + vigente.map(anterior -> " (reemplaza la " + anterior.getVersion() + ")").orElse("")
                        + ". Huella " + guardado.getContenidoHash()
                        + (dispositivo == null ? "" : ". Desde " + dispositivo));
        if (usuarioPorRolRepository != null && entregaAvisoService != null) {
            usuariosDeLaAudiencia(id, audiencia)
                    .forEach(usuario -> entregaAvisoService.programarCorreo(usuario, guardado));
        }
        return aRespuesta(guardado);
    }

    private List<veterinaria.vargasvet.domain.entity.Usuario> usuariosDeLaAudiencia(
            Integer companyId, AudienciaAvisoPrivacidad audiencia) {
        List<RolePurpose> propositos = audiencia == AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS
                ? List.of(RolePurpose.CLIENT_PORTAL)
                : List.of(RolePurpose.COMPANY_ADMIN, RolePurpose.CUSTOM);
        return propositos.stream()
                .flatMap(proposito -> usuarioPorRolRepository
                        .findUsersWithActiveRolePurpose(companyId, proposito).stream())
                .collect(Collectors.toMap(veterinaria.vargasvet.domain.entity.Usuario::getId,
                        usuario -> usuario, (primero, repetido) -> primero))
                .values().stream().toList();
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
        return new AvisoPrivacidadResponse(aviso.getAudiencia(), aviso.getVersion(), aviso.getContenido(), aviso.getContenidoHash(),
                aviso.getVigenteDesde(), aviso.isActivo(), aviso.getCreadoPor(), aviso.getCreadoDispositivo(),
                aviso.getCreadoIp(), leerCampos(aviso));
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
