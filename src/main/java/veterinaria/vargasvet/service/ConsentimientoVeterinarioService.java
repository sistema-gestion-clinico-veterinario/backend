package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.*;
import veterinaria.vargasvet.domain.enums.EstadoConsentimientoVeterinario;
import veterinaria.vargasvet.domain.enums.ControlCumplimientoClinica;
import veterinaria.vargasvet.dto.request.CrearConsentimientoVeterinarioRequest;
import veterinaria.vargasvet.dto.request.DecidirConsentimientoVeterinarioRequest;
import veterinaria.vargasvet.dto.response.ConsentimientoVeterinarioResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.*;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.util.AppClock;
import veterinaria.vargasvet.util.LegalText;
import veterinaria.vargasvet.util.DispositivoInfo;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsentimientoVeterinarioService {
    private static final String MODULO = "Consentimiento veterinario";

    private final ConsentimientoVeterinarioRepository repository;
    private final MascotaRepository mascotaRepository;
    private final ConsultaRepository consultaRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final MascotaPersonaRelacionRepository relacionRepository;
    private final CompanyRepository companyRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditLogService auditLogService;
    private final CompanyComplianceService complianceService;

    @Transactional
    public ConsentimientoVeterinarioResponse crear(CrearConsentimientoVeterinarioRequest request) {
        Integer companyId = empresa();
        exigirGestionInterna(companyId);
        Mascota mascota = mascotaRepository.findByIdAndCompanyId(request.mascotaId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada"));
        Apoderado apoderado = apoderadoRepository.findByIdAndCompanyId(request.apoderadoId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Titular o autorizado no encontrado"));
        MascotaPersonaRelacion relacion = relacionRepository
                .findVigentesDeLaPersonaYMascota(apoderado.getId(), mascota.getId(), companyId, AppClock.today())
                .stream().filter(r -> Boolean.TRUE.equals(r.getPuedeAutorizarAtencion())).findFirst()
                .orElseThrow(() -> new AccessDeniedException("La persona seleccionada no puede autorizar la atención de esta mascota"));
        Consulta consulta = null;
        if (request.consultaId() != null) {
            consulta = consultaRepository.findByIdAndCompanyId(request.consultaId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Consulta no encontrada"));
            if (!consulta.getHistoriaClinica().getMascota().getId().equals(mascota.getId())) {
                throw new IllegalArgumentException("La consulta no corresponde a la mascota indicada");
            }
        }
        if (repository.existsByCompanyIdAndMascotaIdAndEstado(companyId, mascota.getId(),
                EstadoConsentimientoVeterinario.PENDIENTE)) {
            throw new IllegalStateException("La mascota ya tiene un consentimiento pendiente de decisión");
        }

        int version = repository.ultimaVersion(companyId, mascota.getId(), request.procedimiento().trim()) + 1;
        String contenido = contenido(request, mascota, apoderado, relacion, version);
        ConsentimientoInformadoVeterinario c = new ConsentimientoInformadoVeterinario();
        c.setUuid(UUID.randomUUID().toString());
        c.setCompany(companyRepository.getReferenceById(companyId));
        c.setMascota(mascota);
        c.setConsulta(consulta);
        c.setApoderado(apoderado);
        c.setCreadoPor(usuarioRepository.getReferenceById(usuario()));
        c.setProcedimiento(request.procedimiento().trim());
        c.setDiagnosticoIndicacion(request.diagnosticoIndicacion().trim());
        c.setDescripcionProcedimiento(request.descripcionProcedimiento().trim());
        c.setBeneficiosEsperados(request.beneficiosEsperados().trim());
        c.setRiesgosPrevisibles(request.riesgosPrevisibles().trim());
        c.setAlternativas(request.alternativas().trim());
        c.setConsecuenciasNoRealizar(request.consecuenciasNoRealizar().trim());
        c.setAutorizacionAnestesia(request.autorizacionAnestesia());
        c.setAutorizacionEmergencia(request.autorizacionEmergencia());
        c.setPreguntasAclaradas(false);
        c.setVersion(version);
        c.setContenido(contenido);
        c.setContenidoHash(LegalText.sha256Hex(contenido));
        c.setEstado(EstadoConsentimientoVeterinario.PENDIENTE);
        c.setCreadoAt(AppClock.now());
        repository.save(c);
        auditLogService.log(companyId, "CREAR_CONSENTIMIENTO_VETERINARIO", MODULO,
                "Consentimiento " + c.getUuid() + " v" + version + " para mascota " + mascota.getId()
                        + ", huella " + c.getContenidoHash());
        return response(c);
    }

    @Transactional(readOnly = true)
    public List<ConsentimientoVeterinarioResponse> listar(Long mascotaId, EstadoConsentimientoVeterinario estado) {
        Integer companyId = empresa();
        if (!complianceService.gestionadoPorSoftvet(companyId, ControlCumplimientoClinica.CONSENTIMIENTO_VETERINARIO)) return List.of();
        return repository.listar(companyId, mascotaId, estado).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<ConsentimientoVeterinarioResponse> listarMios() {
        Integer companyId = empresa();
        if (!complianceService.gestionadoPorSoftvet(companyId, ControlCumplimientoClinica.CONSENTIMIENTO_VETERINARIO)) return List.of();
        Apoderado apoderado = apoderadoRepository.findByUserIdAndCompanyId(usuario(), companyId)
                .orElseThrow(() -> new AccessDeniedException("La cuenta no es cliente de esta clínica"));
        return repository.listarDelApoderado(apoderado.getId(), companyId).stream().map(this::response).toList();
    }

    @Transactional
    public ConsentimientoVeterinarioResponse decidir(String uuid, DecidirConsentimientoVeterinarioRequest request,
                                                      String ip, String userAgent) {
        Integer companyId = empresa();
        exigirGestionInterna(companyId);
        Apoderado actual = apoderadoRepository.findByUserIdAndCompanyId(usuario(), companyId)
                .orElseThrow(() -> new AccessDeniedException("La cuenta no es cliente de esta clínica"));
        ConsentimientoInformadoVeterinario c = cargar(uuid, companyId);
        if (!c.getApoderado().getId().equals(actual.getId())) {
            throw new AccessDeniedException("Este consentimiento no fue dirigido a la persona autenticada");
        }
        if (c.getEstado() != EstadoConsentimientoVeterinario.PENDIENTE) {
            throw new IllegalStateException("El consentimiento ya fue decidido");
        }
        if (!actual.getNumeroDocumento().equalsIgnoreCase(request.documentoFirmante().trim())) {
            throw new IllegalArgumentException("El documento no coincide con el titular autenticado");
        }
        MascotaPersonaRelacion relacion = relacionRepository
                .findVigentesDeLaPersonaYMascota(actual.getId(), c.getMascota().getId(), companyId, AppClock.today())
                .stream().filter(r -> Boolean.TRUE.equals(r.getPuedeAutorizarAtencion())).findFirst()
                .orElseThrow(() -> new AccessDeniedException("Ya no cuenta con autorización vigente para decidir"));
        if (Boolean.TRUE.equals(request.aceptar()) && !Boolean.TRUE.equals(request.preguntasAclaradas())) {
            throw new IllegalArgumentException("Antes de aceptar debe confirmar que pudo formular preguntas y recibió aclaraciones");
        }
        if (!Boolean.TRUE.equals(request.aceptar()) && longitud(request.motivoRechazo()) < 10) {
            throw new IllegalArgumentException("Indique el motivo del rechazo (mínimo 10 caracteres)");
        }
        c.setEstado(Boolean.TRUE.equals(request.aceptar())
                ? EstadoConsentimientoVeterinario.ACEPTADO : EstadoConsentimientoVeterinario.RECHAZADO);
        c.setDecididoAt(AppClock.now());
        c.setNombreFirmante(request.nombreFirmante().trim());
        c.setDocumentoFirmante(request.documentoFirmante().trim());
        c.setRelacionFirmante(relacion.getTipoRelacion().name());
        c.setPreguntasAclaradas(Boolean.TRUE.equals(request.preguntasAclaradas()));
        c.setDeclaracionAceptacion(request.aceptar());
        c.setMotivoRechazo(Boolean.TRUE.equals(request.aceptar()) ? null : request.motivoRechazo().trim());
        c.setIpDecision(recortar(ip, 64));
        c.setDispositivoDecision(recortar(DispositivoInfo.describir(userAgent), 255));
        repository.save(c);
        auditLogService.log(companyId, "DECIDIR_CONSENTIMIENTO_VETERINARIO", MODULO,
                "Consentimiento " + uuid + " decidido como " + c.getEstado() + "; huella " + c.getContenidoHash());
        return response(c);
    }

    @Transactional
    public ConsentimientoVeterinarioResponse revocarMio(String uuid, String motivo) {
        Integer companyId = empresa();
        exigirGestionInterna(companyId);
        Apoderado actual = apoderadoRepository.findByUserIdAndCompanyId(usuario(), companyId)
                .orElseThrow(() -> new AccessDeniedException("La cuenta no es cliente de esta clínica"));
        ConsentimientoInformadoVeterinario c = cargar(uuid, companyId);
        if (!c.getApoderado().getId().equals(actual.getId())) {
            throw new AccessDeniedException("Este consentimiento no corresponde a la persona autenticada");
        }
        return revocar(c, motivo, companyId);
    }

    @Transactional
    public ConsentimientoVeterinarioResponse revocarPorPersonal(String uuid, String motivo) {
        Integer companyId = empresa();
        exigirGestionInterna(companyId);
        return revocar(cargar(uuid, companyId), motivo, companyId);
    }

    private ConsentimientoVeterinarioResponse revocar(ConsentimientoInformadoVeterinario c, String motivo, Integer companyId) {
        if (c.getEstado() != EstadoConsentimientoVeterinario.ACEPTADO) {
            throw new IllegalStateException("Solo puede revocarse un consentimiento aceptado");
        }
        if (longitud(motivo) < 10) throw new IllegalArgumentException("Indique el motivo de revocación");
        c.setEstado(EstadoConsentimientoVeterinario.REVOCADO);
        c.setRevocadoAt(AppClock.now());
        c.setMotivoRevocacion(motivo.trim());
        repository.save(c);
        auditLogService.log(companyId, "REVOCAR_CONSENTIMIENTO_VETERINARIO", MODULO,
                "Consentimiento " + c.getUuid() + " revocado. Motivo: " + motivo.trim());
        return response(c);
    }

    private ConsentimientoInformadoVeterinario cargar(String uuid, Integer companyId) {
        return repository.findByUuidAndCompanyId(uuid, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Consentimiento no encontrado"));
    }

    private String contenido(CrearConsentimientoVeterinarioRequest r, Mascota m, Apoderado a,
                             MascotaPersonaRelacion relacion, int version) {
        return LegalText.normalize("CONSENTIMIENTO INFORMADO VETERINARIO\n"
                + "Versión: " + version + "\nMascota: " + m.getNombreCompleto() + " (ID " + m.getId() + ")\n"
                + "Titular/autorizado: " + nombre(a.getUser()) + " - " + a.getNumeroDocumento() + "\n"
                + "Relación: " + relacion.getTipoRelacion() + "\nProcedimiento: " + r.procedimiento().trim() + "\n"
                + "Diagnóstico o indicación: " + r.diagnosticoIndicacion().trim() + "\n"
                + "Descripción: " + r.descripcionProcedimiento().trim() + "\n"
                + "Beneficios esperados: " + r.beneficiosEsperados().trim() + "\n"
                + "Riesgos previsibles: " + r.riesgosPrevisibles().trim() + "\n"
                + "Alternativas: " + r.alternativas().trim() + "\n"
                + "Consecuencias de no realizarlo: " + r.consecuenciasNoRealizar().trim() + "\n"
                + "Incluye autorización de anestesia: " + siNo(r.autorizacionAnestesia()) + "\n"
                + "Incluye maniobras de emergencia: " + siNo(r.autorizacionEmergencia()) + "\n"
                + "La decisión es libre, puede formular preguntas y puede revocar el consentimiento antes del procedimiento.");
    }

    private ConsentimientoVeterinarioResponse response(ConsentimientoInformadoVeterinario c) {
        return new ConsentimientoVeterinarioResponse(c.getUuid(), c.getMascota().getId(), c.getMascota().getNombreCompleto(),
                c.getConsulta() == null ? null : c.getConsulta().getId(), c.getApoderado().getId(), nombre(c.getApoderado().getUser()),
                c.getProcedimiento(), c.getDiagnosticoIndicacion(), c.getDescripcionProcedimiento(), c.getBeneficiosEsperados(),
                c.getRiesgosPrevisibles(), c.getAlternativas(), c.getConsecuenciasNoRealizar(), c.isAutorizacionAnestesia(),
                c.isAutorizacionEmergencia(), c.getVersion(), c.getContenido(), c.getContenidoHash(), c.getEstado(), c.getCreadoAt(),
                c.getDecididoAt(), c.getNombreFirmante(), c.getDocumentoFirmante(), c.getRelacionFirmante(), c.isPreguntasAclaradas(),
                c.getMotivoRechazo(), c.getRevocadoAt(), c.getMotivoRevocacion());
    }

    private Integer empresa() { Integer id = SecurityUtils.getCurrentCompanyId(); if (id == null) throw new AccessDeniedException("Seleccione una empresa"); return id; }
    private Integer usuario() { Integer id = SecurityUtils.getCurrentUserId(); if (id == null) throw new AccessDeniedException("Se requiere autenticación"); return id; }
    private int longitud(String s) { return s == null ? 0 : s.trim().length(); }
    private String nombre(Usuario u) { return ((u.getNombre() == null ? "" : u.getNombre()) + " " + (u.getApellido() == null ? "" : u.getApellido())).trim(); }
    private String siNo(boolean valor) { return valor ? "SÍ" : "NO"; }
    private String recortar(String texto, int maximo) { return texto == null ? null : texto.substring(0, Math.min(texto.length(), maximo)); }
    private void exigirGestionInterna(Integer companyId) {
        if (!complianceService.gestionadoPorSoftvet(companyId, ControlCumplimientoClinica.CONSENTIMIENTO_VETERINARIO))
            throw new IllegalStateException("El consentimiento veterinario digital no está habilitado para esta clínica");
    }
}
