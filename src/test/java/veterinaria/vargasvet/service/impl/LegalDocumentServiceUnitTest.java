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
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.util.LegalText;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private AuditLogService auditLogService;

    private LegalDocumentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LegalDocumentServiceImpl(legalDocumentRepository, userConsentRepository, usuarioRepository, auditLogService);
        ReflectionTestUtils.setField(service, "gracePeriodDays", GRACE_PERIOD_DAYS);
    }

    private LegalDocument buildDocument(Long id, LocalDateTime vigenteDesde) {
        LegalDocument doc = new LegalDocument();
        doc.setId(id);
        doc.setTipo(LegalDocumentType.TERMINOS_Y_CONDICIONES);
        doc.setVersion("1.0");
        doc.setContenido("contenido");
        doc.setContenidoHash(LegalText.sha256Hex("contenido"));
        doc.setVigenteDesde(vigenteDesde);
        doc.setActivo(true);
        return doc;
    }

    @Test
    void getStatus_sinDocumentosPendientes_noRequiereAceptacionNiEstaVencido() {
        LegalDocument documento = buildDocument(1L, LocalDateTime.now());
        when(legalDocumentRepository.findByActivoTrueOrderByIdAsc()).thenReturn(List.of(documento));

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
        when(legalDocumentRepository.findByActivoTrueOrderByIdAsc()).thenReturn(List.of(documento));
        when(userConsentRepository.findByUsuarioId(USUARIO_ID)).thenReturn(List.of());

        LegalStatusDTO status = service.getStatus(USUARIO_ID);

        assertTrue(status.isNeedsAcceptance());
        assertFalse(status.isOverdue());
        assertEquals(1, status.getPendingDocuments().size());
    }

    @Test
    void getStatus_documentoPendienteFueraDelPeriodoDeGracia_quedaVencido() {
        LegalDocument documento = buildDocument(1L, LocalDateTime.now().minusDays(GRACE_PERIOD_DAYS + 1));
        when(legalDocumentRepository.findByActivoTrueOrderByIdAsc()).thenReturn(List.of(documento));
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
        verify(userConsentRepository, times(1)).saveAndFlush(captor.capture());
        UserConsent saved = captor.getValue();
        assertEquals(usuario, saved.getUsuario());
        assertEquals(documento, saved.getLegalDocument());
        assertEquals("127.0.0.1", saved.getIpAddress());
        assertEquals("JUnit-Agent", saved.getUserAgent());
        assertEquals(LegalDocumentType.TERMINOS_Y_CONDICIONES, saved.getDocumentoTipo());
        assertEquals("1.0", saved.getDocumentoVersion());
        assertEquals(documento.getContenidoHash(), saved.getContenidoHash());
        assertTrue(saved.isTextoRecuperable());
        assertEquals(veterinaria.vargasvet.domain.enums.TipoConstanciaLegal.ACEPTACION,
                saved.getTipoConstancia());
    }

    @Test
    void accept_politicaInformativaRegistraLecturaYNoAceptacionContractual() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        LegalDocument politica = buildDocument(2L, LocalDateTime.now());
        politica.setTipo(LegalDocumentType.POLITICA_PRIVACIDAD);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(legalDocumentRepository.findById(2L)).thenReturn(Optional.of(politica));

        service.accept(USUARIO_ID, List.of(2L), "127.0.0.1", "JUnit-Agent");

        ArgumentCaptor<UserConsent> captor = ArgumentCaptor.forClass(UserConsent.class);
        verify(userConsentRepository).saveAndFlush(captor.capture());
        assertEquals(veterinaria.vargasvet.domain.enums.TipoConstanciaLegal.CONSTANCIA_LECTURA,
                captor.getValue().getTipoConstancia());
    }

    @Test
    void accept_unaVersionRetirada_seRechazaYNoRegistraNada() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        LegalDocument retirada = buildDocument(1L, LocalDateTime.now());
        retirada.setActivo(false);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(legalDocumentRepository.findById(1L)).thenReturn(Optional.of(retirada));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.accept(USUARIO_ID, List.of(1L), "127.0.0.1", "JUnit-Agent"));

        assertTrue(error.getMessage().contains("ya no está vigente"));
        verify(userConsentRepository, never()).saveAndFlush(any());
    }

    @Test
    void accept_recortaElNavegadorYLaIpALoQueCabeEnLaBaseDeDatos() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(legalDocumentRepository.findById(1L)).thenReturn(Optional.of(buildDocument(1L, LocalDateTime.now())));

        service.accept(USUARIO_ID, List.of(1L), "i".repeat(100), "u".repeat(400));

        ArgumentCaptor<UserConsent> captor = ArgumentCaptor.forClass(UserConsent.class);
        verify(userConsentRepository).saveAndFlush(captor.capture());
        assertEquals(255, captor.getValue().getUserAgent().length());
        assertEquals(64, captor.getValue().getIpAddress().length());
    }

    @Test
    void accept_siLaConstanciaSeDuplicaPorUnaCargaSimultanea_avisaEnVezDeFallarConUnErrorDeBaseDeDatos() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(legalDocumentRepository.findById(1L)).thenReturn(Optional.of(buildDocument(1L, LocalDateTime.now())));
        when(userConsentRepository.saveAndFlush(any()))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicada"));

        assertThrows(IllegalStateException.class,
                () -> service.accept(USUARIO_ID, List.of(1L), "127.0.0.1", "JUnit-Agent"));
    }

    @Test
    void accept_unMismoDocumentoRepetidoEnLaSolicitudSeRegistraUnaVez() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(legalDocumentRepository.findById(1L)).thenReturn(Optional.of(buildDocument(1L, LocalDateTime.now())));

        service.accept(USUARIO_ID, List.of(1L, 1L), "127.0.0.1", "JUnit-Agent");

        verify(userConsentRepository, times(1)).saveAndFlush(any());
    }

    @Test
    void accept_documentoYaAceptado_esIdempotenteYNoDuplicaElConsentimiento() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(legalDocumentRepository.findById(1L)).thenReturn(Optional.of(buildDocument(1L, LocalDateTime.now())));
        when(userConsentRepository.existsByUsuarioIdAndLegalDocumentId(USUARIO_ID, 1L)).thenReturn(true);

        service.accept(USUARIO_ID, List.of(1L), "127.0.0.1", "JUnit-Agent");

        verify(userConsentRepository, never()).saveAndFlush(any());
    }

    @Test
    void accept_usuarioInexistente_lanzaResourceNotFoundException() {
        when(usuarioRepository.findById(anyInt())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.accept(USUARIO_ID, List.of(1L), "127.0.0.1", "JUnit-Agent"));
    }

    private static final String TEXTO_FINAL = "TÉRMINOS Y CONDICIONES\nTexto definitivo y completo de la versión siguiente.";

    @Test
    void publish_retiraLaVigenteYPublicaLaNuevaConSuHuella() {
        LegalDocument vigente = buildDocument(1L, LocalDateTime.now().minusDays(30));
        vigente.setVersion("1.0");
        when(legalDocumentRepository.existsByTipoAndVersion(LegalDocumentType.TERMINOS_Y_CONDICIONES, "2.0")).thenReturn(false);
        when(legalDocumentRepository.findActiveByTipoForUpdate(LegalDocumentType.TERMINOS_Y_CONDICIONES))
                .thenReturn(Optional.of(vigente));
        when(legalDocumentRepository.save(any(LegalDocument.class))).thenAnswer(inv -> {
            LegalDocument d = inv.getArgument(0);
            d.setId(2L);
            return d;
        });

        var publicado = service.publish(LegalDocumentType.TERMINOS_Y_CONDICIONES, " 2.0 ", TEXTO_FINAL + "\r\n");

        assertFalse(vigente.isActivo());
        ArgumentCaptor<LegalDocument> captor = ArgumentCaptor.forClass(LegalDocument.class);
        verify(legalDocumentRepository).save(captor.capture());
        LegalDocument nuevo = captor.getValue();
        assertTrue(nuevo.isActivo());
        assertEquals("2.0", nuevo.getVersion());
        assertEquals(TEXTO_FINAL, nuevo.getContenido());
        assertEquals(LegalText.sha256Hex(TEXTO_FINAL), nuevo.getContenidoHash());
        assertEquals("2.0", publicado.getVersion());
        verify(auditLogService).log(eq("PUBLICAR_DOCUMENTO_LEGAL"), eq("Seguridad"), contains("reemplaza la 1.0"));
    }

    @Test
    void publish_siOtraPublicacionGanoPorUnaCargaSimultanea_avisaEnVezDeFallarConUnErrorDeBaseDeDatos() {
        when(legalDocumentRepository.findActiveByTipoForUpdate(LegalDocumentType.TERMINOS_Y_CONDICIONES))
                .thenReturn(Optional.empty());
        when(legalDocumentRepository.save(any(LegalDocument.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("ux_legal_document_tipo_activo"));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.publish(LegalDocumentType.TERMINOS_Y_CONDICIONES, "3.0", TEXTO_FINAL));

        assertTrue(error.getMessage().contains("Otra publicación"));
        verify(auditLogService, never()).log(eq("PUBLICAR_DOCUMENTO_LEGAL"), any(), any());
    }

    @Test
    void publish_sinVersionVigentePublicaLaPrimera() {
        when(legalDocumentRepository.findActiveByTipoForUpdate(LegalDocumentType.POLITICA_PRIVACIDAD))
                .thenReturn(Optional.empty());
        when(legalDocumentRepository.save(any(LegalDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        service.publish(LegalDocumentType.POLITICA_PRIVACIDAD, "1.0", TEXTO_FINAL);

        verify(legalDocumentRepository).save(any(LegalDocument.class));
    }

    @Test
    void publish_rechazaUnTextoConBorradorOCamposSinCompletar() {
        for (String texto : List.of(
                "VERSIÓN 2.0 — BORRADOR PROVISIONAL del documento",
                "El titular es [Razón Social], con domicilio en [POR COMPLETAR]",
                "Plazo de [PENDIENTE DE DEFINIR] días")) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> service.publish(LegalDocumentType.TERMINOS_Y_CONDICIONES, "2.0", texto));
            assertTrue(error.getMessage().contains("borrador o tiene campos sin completar"), texto);
        }
        verify(legalDocumentRepository, never()).save(any());
    }

    @Test
    void publish_rechazaUnaVersionQueYaExiste() {
        when(legalDocumentRepository.existsByTipoAndVersion(LegalDocumentType.TERMINOS_Y_CONDICIONES, "1.0")).thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.publish(LegalDocumentType.TERMINOS_Y_CONDICIONES, "1.0", TEXTO_FINAL));

        assertTrue(error.getMessage().contains("publica una versión nueva"));
        verify(legalDocumentRepository, never()).save(any());
    }

    @Test
    void publish_rechazaUnTextoIdenticoAlVigente() {
        LegalDocument vigente = buildDocument(1L, LocalDateTime.now());
        vigente.setContenidoHash(LegalText.sha256Hex(TEXTO_FINAL));
        when(legalDocumentRepository.findActiveByTipoForUpdate(LegalDocumentType.TERMINOS_Y_CONDICIONES))
                .thenReturn(Optional.of(vigente));

        assertThrows(IllegalArgumentException.class,
                () -> service.publish(LegalDocumentType.TERMINOS_Y_CONDICIONES, "2.0", TEXTO_FINAL));

        assertTrue(vigente.isActivo());
        verify(legalDocumentRepository, never()).save(any());
    }

    @Test
    void publish_rechazaUnaVersionMalFormadaOUnTextoVacio() {
        for (String version : new String[] {"", "  ", "2 0", "-1", "x".repeat(21), null}) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.publish(LegalDocumentType.TERMINOS_Y_CONDICIONES, version, TEXTO_FINAL));
        }
        assertThrows(IllegalArgumentException.class,
                () -> service.publish(LegalDocumentType.TERMINOS_Y_CONDICIONES, "2.0", "  \n "));
    }

    @Test
    void getMyAcceptances_devuelveLoQueSeAceptoConSuHuellaYMarcaLasNoRecuperables() {
        UserConsent reciente = new UserConsent();
        reciente.setDocumentoTipo(LegalDocumentType.POLITICA_PRIVACIDAD);
        reciente.setTipoConstancia(veterinaria.vargasvet.domain.enums.TipoConstanciaLegal.CONSTANCIA_LECTURA);
        reciente.setDocumentoVersion("2.0");
        reciente.setContenidoHash("abc");
        reciente.setTextoRecuperable(true);
        reciente.setFechaAceptacion(LocalDateTime.now());
        UserConsent antigua = new UserConsent();
        antigua.setDocumentoTipo(LegalDocumentType.TERMINOS_Y_CONDICIONES);
        antigua.setTipoConstancia(veterinaria.vargasvet.domain.enums.TipoConstanciaLegal.ACEPTACION);
        antigua.setDocumentoVersion("1.0");
        antigua.setTextoRecuperable(false);
        antigua.setFechaAceptacion(LocalDateTime.now().minusDays(40));
        when(userConsentRepository.findByUsuarioIdOrderByFechaAceptacionDesc(USUARIO_ID))
                .thenReturn(List.of(reciente, antigua));

        var historial = service.getMyAcceptances(USUARIO_ID);

        assertEquals(2, historial.size());
        assertEquals("2.0", historial.get(0).version());
        assertEquals("abc", historial.get(0).contenidoHash());
        assertEquals(veterinaria.vargasvet.domain.enums.TipoConstanciaLegal.CONSTANCIA_LECTURA,
                historial.get(0).tipoConstancia());
        assertEquals(veterinaria.vargasvet.domain.enums.TipoConstanciaLegal.ACEPTACION,
                historial.get(1).tipoConstancia());
        assertFalse(historial.get(1).textoRecuperable());
        assertEquals(null, historial.get(1).contenidoHash());
    }
}
