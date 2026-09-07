package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.entity.LegalDocument;
import veterinaria.vargasvet.domain.entity.UserConsent;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.LegalDocumentType;
import veterinaria.vargasvet.dto.response.LegalStatusDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.LegalDocumentRepository;
import veterinaria.vargasvet.repository.UserConsentRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalDocumentServiceUnitTest {

    private static final int GRACE_PERIOD_DAYS = 14;
    private static final Integer USUARIO_ID = 1;

    @Mock
    private LegalDocumentRepository legalDocumentRepository;

    @Mock
    private UserConsentRepository userConsentRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private LegalDocumentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LegalDocumentServiceImpl(legalDocumentRepository, userConsentRepository, usuarioRepository);
        ReflectionTestUtils.setField(service, "gracePeriodDays", GRACE_PERIOD_DAYS);
    }

    private LegalDocument buildDocument(Long id, LocalDateTime vigenteDesde) {
        LegalDocument doc = new LegalDocument();
        doc.setId(id);
        doc.setTipo(LegalDocumentType.TERMINOS_Y_CONDICIONES);
        doc.setVersion("1.0");
        doc.setContenido("contenido");
        doc.setVigenteDesde(vigenteDesde);
        doc.setActivo(true);
        return doc;
    }

    @Test
    void getStatus_sinDocumentosPendientes_noRequiereAceptacionNiEstaVencido() {
        LegalDocument documento = buildDocument(1L, LocalDateTime.now());
        when(legalDocumentRepository.findByActivoTrue()).thenReturn(List.of(documento));

        UserConsent consent = new UserConsent();
        consent.setLegalDocument(documento);
        when(userConsentRepository.findByUsuarioId(USUARIO_ID)).thenReturn(List.of(consent));

        LegalStatusDTO status = service.getStatus(USUARIO_ID);

        assertFalse(status.isNeedsAcceptance());
        assertFalse(status.isOverdue());
        assertTrue(status.getPendingDocuments().isEmpty());
    }

    @Test
    void getStatus_documentoPendienteDentroDelPeriodoDeGracia_requiereAceptacionPeroNoEstaVencido() {
        LegalDocument documento = buildDocument(1L, LocalDateTime.now().minusDays(5));
        when(legalDocumentRepository.findByActivoTrue()).thenReturn(List.of(documento));
        when(userConsentRepository.findByUsuarioId(USUARIO_ID)).thenReturn(List.of());

        LegalStatusDTO status = service.getStatus(USUARIO_ID);

        assertTrue(status.isNeedsAcceptance());
        assertFalse(status.isOverdue());
        assertEquals(1, status.getPendingDocuments().size());
    }

    @Test
    void getStatus_documentoPendienteFueraDelPeriodoDeGracia_quedaVencido() {
        LegalDocument documento = buildDocument(1L, LocalDateTime.now().minusDays(GRACE_PERIOD_DAYS + 1));
        when(legalDocumentRepository.findByActivoTrue()).thenReturn(List.of(documento));
        when(userConsentRepository.findByUsuarioId(USUARIO_ID)).thenReturn(List.of());

        LegalStatusDTO status = service.getStatus(USUARIO_ID);

        assertTrue(status.isNeedsAcceptance());
        assertTrue(status.isOverdue());
    }

    @Test
    void isPastGracePeriod_justoEnElLimite_todaviaNoEstaVencido() {
        LegalDocument documento = buildDocument(1L, LocalDateTime.now().minusDays(GRACE_PERIOD_DAYS).plusMinutes(1));
        when(userConsentRepository.findPendingActiveDocuments(USUARIO_ID)).thenReturn(List.of(documento));

        assertFalse(service.isPastGracePeriod(USUARIO_ID));
    }

    @Test
    void isPastGracePeriod_sinPendientes_devuelveFalse() {
        when(userConsentRepository.findPendingActiveDocuments(USUARIO_ID)).thenReturn(List.of());

        assertFalse(service.isPastGracePeriod(USUARIO_ID));
        assertFalse(service.hasPendingConsent(USUARIO_ID));
    }

    @Test
    void accept_documentoNuevo_creaConsentimientoConDatosDeAuditoria() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        LegalDocument documento = buildDocument(1L, LocalDateTime.now());

        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(legalDocumentRepository.findById(1L)).thenReturn(Optional.of(documento));
        when(userConsentRepository.existsByUsuarioIdAndLegalDocumentId(USUARIO_ID, 1L)).thenReturn(false);

        service.accept(USUARIO_ID, List.of(1L), "127.0.0.1", "JUnit-Agent");

        ArgumentCaptor<UserConsent> captor = ArgumentCaptor.forClass(UserConsent.class);
        verify(userConsentRepository, times(1)).save(captor.capture());
        UserConsent saved = captor.getValue();
        assertEquals(usuario, saved.getUsuario());
        assertEquals(documento, saved.getLegalDocument());
        assertEquals("127.0.0.1", saved.getIpAddress());
        assertEquals("JUnit-Agent", saved.getUserAgent());
    }

    @Test
    void accept_documentoYaAceptado_esIdempotenteYNoDuplicaElConsentimiento() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(legalDocumentRepository.findById(1L)).thenReturn(Optional.of(buildDocument(1L, LocalDateTime.now())));
        when(userConsentRepository.existsByUsuarioIdAndLegalDocumentId(USUARIO_ID, 1L)).thenReturn(true);

        service.accept(USUARIO_ID, List.of(1L), "127.0.0.1", "JUnit-Agent");

        verify(userConsentRepository, never()).save(any());
    }

    @Test
    void accept_usuarioInexistente_lanzaResourceNotFoundException() {
        when(usuarioRepository.findById(anyInt())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.accept(USUARIO_ID, List.of(1L), "127.0.0.1", "JUnit-Agent"));
    }
}
