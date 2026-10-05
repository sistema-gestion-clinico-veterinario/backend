package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.LegalDocument;
import veterinaria.vargasvet.domain.entity.UserConsent;
import veterinaria.vargasvet.domain.enums.LegalDocumentType;
import veterinaria.vargasvet.dto.response.LegalAcceptanceDTO;
import veterinaria.vargasvet.dto.response.LegalDocumentDTO;
import veterinaria.vargasvet.dto.response.LegalStatusDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.LegalDocumentRepository;
import veterinaria.vargasvet.repository.UserConsentRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.LegalDocumentService;
import veterinaria.vargasvet.util.AppClock;
import veterinaria.vargasvet.util.LegalText;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LegalDocumentServiceImpl implements LegalDocumentService {

    private static final int MAX_USER_AGENT = 255;
    private static final int MAX_IP = 64;
    private static final Pattern VERSION_VALIDA = Pattern.compile("^[0-9A-Za-z][0-9A-Za-z._-]{0,19}$");

    private final LegalDocumentRepository legalDocumentRepository;
    private final UserConsentRepository userConsentRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditLogService auditLogService;

    @Value("${legal.grace-period-days:14}")
    private int gracePeriodDays;

    @Override
    @Transactional(readOnly = true)
    public List<LegalDocumentDTO> getActiveDocuments() {
        return legalDocumentRepository.findByActivoTrueOrderByIdAsc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LegalStatusDTO getStatus(Integer usuarioId) {
        List<LegalDocument> activos = legalDocumentRepository.findByActivoTrueOrderByIdAsc();
        Set<Long> aceptados = userConsentRepository.findByUsuarioId(usuarioId).stream()
                .map(uc -> uc.getLegalDocument().getId())
                .collect(Collectors.toSet());

        List<LegalDocument> documentosPendientes = activos.stream()
                .filter(doc -> !aceptados.contains(doc.getId()))
                .collect(Collectors.toList());

        List<LegalDocumentDTO> pendientes = documentosPendientes.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return new LegalStatusDTO(!pendientes.isEmpty(), isOverdue(documentosPendientes), pendientes);
    }

    private boolean isOverdue(List<LegalDocument> pendingDocuments) {
        LocalDateTime now = AppClock.now();
        return pendingDocuments.stream()
                .anyMatch(doc -> doc.getVigenteDesde().plusDays(gracePeriodDays).isBefore(now));
    }

    @Override
    @Transactional
    public void accept(Integer usuarioId, List<Long> legalDocumentIds, String ipAddress, String userAgent) {
        var usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        for (Long legalDocumentId : legalDocumentIds.stream().distinct().toList()) {
            LegalDocument documento = legalDocumentRepository.findById(legalDocumentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Documento legal no encontrado: " + legalDocumentId));

            if (!documento.isActivo()) {
                throw new IllegalArgumentException("La versión " + documento.getVersion() + " de "
                        + etiqueta(documento.getTipo()) + " ya no está vigente. Actualiza la pantalla para ver la versión actual.");
            }
            if (userConsentRepository.existsByUsuarioIdAndLegalDocumentId(usuarioId, legalDocumentId)) {
                continue;
            }

            UserConsent consent = new UserConsent();
            consent.setUsuario(usuario);
            consent.setLegalDocument(documento);
            consent.setDocumentoTipo(documento.getTipo());
            consent.setDocumentoVersion(documento.getVersion());
            consent.setContenidoHash(documento.getContenidoHash());
            consent.setTextoRecuperable(true);
            consent.setFechaAceptacion(AppClock.now());
            consent.setIpAddress(truncate(ipAddress, MAX_IP));
            consent.setUserAgent(truncate(userAgent, MAX_USER_AGENT));
            try {
                userConsentRepository.saveAndFlush(consent);
            } catch (DataIntegrityViolationException e) {
                throw new IllegalStateException("La aceptación ya se registró. Actualiza la pantalla.");
            }
        }
    }

    @Override
    @Transactional
    public LegalDocumentDTO publish(LegalDocumentType tipo, String version, String contenido) {
        String versionLimpia = version == null ? "" : version.trim();
        if (!VERSION_VALIDA.matcher(versionLimpia).matches()) {
            throw new IllegalArgumentException(
                    "La versión debe tener hasta 20 caracteres: letras, números, punto, guion o guion bajo");
        }
        String texto = LegalText.normalize(contenido);
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("El texto del documento es obligatorio");
        }
        Set<String> borradores = LegalText.draftMarkers(texto);
        if (!borradores.isEmpty()) {
            throw new IllegalArgumentException("El texto todavía es un borrador o tiene campos sin completar ("
                    + borradores.stream().limit(3).collect(Collectors.joining(", "))
                    + "). Complétalo antes de publicarlo.");
        }
        if (legalDocumentRepository.existsByTipoAndVersion(tipo, versionLimpia)) {
            throw new IllegalArgumentException("Ya existe la versión " + versionLimpia + " de " + etiqueta(tipo)
                    + ". Un texto publicado no se modifica: publica una versión nueva.");
        }

        String huella = LegalText.sha256Hex(texto);
        var vigente = legalDocumentRepository.findActiveByTipoForUpdate(tipo);
        if (vigente.isPresent() && huella.equals(vigente.get().getContenidoHash())) {
            throw new IllegalArgumentException("El texto es idéntico al de la versión vigente; no hay nada que publicar");
        }
        vigente.ifPresent(documento -> {
            documento.setActivo(false);
            legalDocumentRepository.saveAndFlush(documento);
        });

        LocalDateTime ahora = AppClock.now();
        LegalDocument nuevo = new LegalDocument();
        nuevo.setTipo(tipo);
        nuevo.setVersion(versionLimpia);
        nuevo.setContenido(texto);
        nuevo.setContenidoHash(huella);
        nuevo.setVigenteDesde(ahora);
        nuevo.setCreadoEn(ahora);
        nuevo.setActivo(true);
        LegalDocument guardado;
        try {
            guardado = legalDocumentRepository.save(nuevo);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Otra publicación de " + etiqueta(tipo)
                    + " se acaba de realizar. Actualiza la pantalla y revisa la versión vigente antes de intentarlo de nuevo.");
        }

        auditLogService.log("PUBLICAR_DOCUMENTO_LEGAL", "Seguridad",
                "Se publicó la versión " + versionLimpia + " de " + etiqueta(tipo)
                        + vigente.map(anterior -> " (reemplaza la " + anterior.getVersion() + ")").orElse("")
                        + ". Huella " + huella);
        return toDTO(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LegalAcceptanceDTO> getMyAcceptances(Integer usuarioId) {
        return userConsentRepository.findByUsuarioIdOrderByFechaAceptacionDesc(usuarioId).stream()
                .map(uc -> new LegalAcceptanceDTO(uc.getDocumentoTipo(), uc.getDocumentoVersion(),
                        uc.getContenidoHash(), uc.isTextoRecuperable(), uc.getFechaAceptacion()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPendingConsent(Integer usuarioId) {
        return !userConsentRepository.findPendingActiveDocuments(usuarioId).isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPastGracePeriod(Integer usuarioId) {
        return isOverdue(userConsentRepository.findPendingActiveDocuments(usuarioId));
    }

    private LegalDocumentDTO toDTO(LegalDocument doc) {
        return new LegalDocumentDTO(doc.getId(), doc.getTipo(), doc.getVersion(), doc.getContenido(), doc.getVigenteDesde());
    }

    private String etiqueta(LegalDocumentType tipo) {
        return tipo == LegalDocumentType.TERMINOS_Y_CONDICIONES ? "los Términos y Condiciones" : "la Política de Privacidad";
    }

    private String truncate(String valor, int max) {
        return valor == null || valor.length() <= max ? valor : valor.substring(0, max);
    }
}
