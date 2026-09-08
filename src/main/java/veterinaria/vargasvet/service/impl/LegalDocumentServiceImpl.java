package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.LegalDocument;
import veterinaria.vargasvet.domain.entity.UserConsent;
import veterinaria.vargasvet.dto.response.LegalDocumentDTO;
import veterinaria.vargasvet.dto.response.LegalStatusDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.LegalDocumentRepository;
import veterinaria.vargasvet.repository.UserConsentRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.service.LegalDocumentService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LegalDocumentServiceImpl implements LegalDocumentService {

    private final LegalDocumentRepository legalDocumentRepository;
    private final UserConsentRepository userConsentRepository;
    private final UsuarioRepository usuarioRepository;

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
        LocalDateTime now = veterinaria.vargasvet.util.AppClock.now();
        return pendingDocuments.stream()
                .anyMatch(doc -> doc.getVigenteDesde().plusDays(gracePeriodDays).isBefore(now));
    }

    @Override
    @Transactional
    public void accept(Integer usuarioId, List<Long> legalDocumentIds, String ipAddress, String userAgent) {
        var usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        for (Long legalDocumentId : legalDocumentIds) {
            LegalDocument documento = legalDocumentRepository.findById(legalDocumentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Documento legal no encontrado: " + legalDocumentId));

            if (userConsentRepository.existsByUsuarioIdAndLegalDocumentId(usuarioId, legalDocumentId)) {
                continue;
            }

            UserConsent consent = new UserConsent();
            consent.setUsuario(usuario);
            consent.setLegalDocument(documento);
            consent.setFechaAceptacion(veterinaria.vargasvet.util.AppClock.now());
            consent.setIpAddress(ipAddress);
            consent.setUserAgent(userAgent);
            userConsentRepository.save(consent);
        }
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
}
