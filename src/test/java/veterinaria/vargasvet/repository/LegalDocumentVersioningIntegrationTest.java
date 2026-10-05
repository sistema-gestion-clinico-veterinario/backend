package veterinaria.vargasvet.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import veterinaria.vargasvet.domain.entity.LegalDocument;
import veterinaria.vargasvet.domain.entity.UserConsent;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.LegalDocumentType;
import veterinaria.vargasvet.util.LegalText;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Las restricciones que el mapeo expone a la aplicación: una versión por tipo, una constancia por
 * persona y documento, y un texto publicado que Hibernate nunca reescribe. Los disparadores que lo
 * hacen cumplir en la base de datos están en la migración V96 y se probaron contra PostgreSQL.
 */
@DataJpaTest
class LegalDocumentVersioningIntegrationTest {

    @Autowired private LegalDocumentRepository documentRepository;
    @Autowired private UserConsentRepository consentRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EntityManager entityManager;

    private LegalDocument documento(LegalDocumentType tipo, String version, String contenido, boolean activo) {
        LegalDocument doc = new LegalDocument();
        doc.setTipo(tipo);
        doc.setVersion(version);
        doc.setContenido(contenido);
        doc.setContenidoHash(LegalText.sha256Hex(contenido));
        doc.setVigenteDesde(LocalDateTime.now());
        doc.setCreadoEn(LocalDateTime.now());
        doc.setActivo(activo);
        return documentRepository.saveAndFlush(doc);
    }

    private Usuario persona() {
        Usuario usuario = new Usuario();
        usuario.setEmail("u-" + UUID.randomUUID() + "@vargasvet.test");
        usuario.setUsername("u-" + UUID.randomUUID());
        usuario.setNombre("Ana");
        usuario.setApellido("Test");
        usuario.setDni(String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private UserConsent constancia(Usuario usuario, LegalDocument doc) {
        UserConsent consent = new UserConsent();
        consent.setUsuario(usuario);
        consent.setLegalDocument(doc);
        consent.setDocumentoTipo(doc.getTipo());
        consent.setDocumentoVersion(doc.getVersion());
        consent.setContenidoHash(doc.getContenidoHash());
        consent.setFechaAceptacion(LocalDateTime.now());
        return consent;
    }

    @Test
    void noSePuedePublicarDosVecesLaMismaVersionDeUnTipo() {
        documento(LegalDocumentType.TERMINOS_Y_CONDICIONES, "2.0", "uno", false);

        assertThatThrownBy(() -> documento(LegalDocumentType.TERMINOS_Y_CONDICIONES, "2.0", "otro", false))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void lamismaVersionPuedeExistirEnOtroTipo() {
        documento(LegalDocumentType.TERMINOS_Y_CONDICIONES, "2.0", "uno", false);

        documento(LegalDocumentType.POLITICA_PRIVACIDAD, "2.0", "otro", false);

        assertThat(documentRepository.count()).isEqualTo(2);
    }

    @Test
    void unaPersonaNoPuedeAceptarDosVecesElMismoDocumento() {
        Usuario ana = persona();
        LegalDocument doc = documento(LegalDocumentType.TERMINOS_Y_CONDICIONES, "2.0", "texto", true);
        consentRepository.saveAndFlush(constancia(ana, doc));

        assertThatThrownBy(() -> consentRepository.saveAndFlush(constancia(ana, doc)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void dosPersonasPuedenAceptarElMismoDocumento() {
        LegalDocument doc = documento(LegalDocumentType.TERMINOS_Y_CONDICIONES, "2.0", "texto", true);

        consentRepository.saveAndFlush(constancia(persona(), doc));
        consentRepository.saveAndFlush(constancia(persona(), doc));

        assertThat(consentRepository.count()).isEqualTo(2);
    }

    @Test
    void hibernateNuncaReescribeElTextoDeUnDocumentoPublicado() {
        LegalDocument doc = documento(LegalDocumentType.TERMINOS_Y_CONDICIONES, "2.0", "texto original", true);
        Long id = doc.getId();

        doc.setContenido("texto alterado");
        doc.setVersion("9.9");
        doc.setActivo(false);
        documentRepository.saveAndFlush(doc);
        entityManager.clear();

        LegalDocument recargado = documentRepository.findById(id).orElseThrow();
        assertThat(recargado.getContenido()).isEqualTo("texto original");
        assertThat(recargado.getVersion()).isEqualTo("2.0");
        assertThat(recargado.isActivo()).isFalse();
    }

    @Test
    void laVersionVigenteSeBuscaPorTipoParaPublicarLaSiguiente() {
        LegalDocument terminos = documento(LegalDocumentType.TERMINOS_Y_CONDICIONES, "2.0", "t", true);
        documento(LegalDocumentType.POLITICA_PRIVACIDAD, "2.0", "p", true);
        documento(LegalDocumentType.TERMINOS_Y_CONDICIONES, "1.0", "viejo", false);

        var vigente = documentRepository.findActiveByTipoForUpdate(LegalDocumentType.TERMINOS_Y_CONDICIONES);

        assertThat(vigente).map(LegalDocument::getId).contains(terminos.getId());
        assertThat(documentRepository.existsByTipoAndVersion(LegalDocumentType.TERMINOS_Y_CONDICIONES, "1.0")).isTrue();
        assertThat(documentRepository.existsByTipoAndVersion(LegalDocumentType.POLITICA_PRIVACIDAD, "1.0")).isFalse();
    }

    @Test
    void elHistorialDeUnaPersonaVieneDelMasRecienteAlMasAntiguo() {
        Usuario ana = persona();
        LegalDocument viejo = documento(LegalDocumentType.TERMINOS_Y_CONDICIONES, "1.0", "v1", false);
        LegalDocument nuevo = documento(LegalDocumentType.TERMINOS_Y_CONDICIONES, "2.0", "v2", true);
        UserConsent primera = constancia(ana, viejo);
        primera.setFechaAceptacion(LocalDateTime.now().minusDays(30));
        consentRepository.saveAndFlush(primera);
        consentRepository.saveAndFlush(constancia(ana, nuevo));
        consentRepository.saveAndFlush(constancia(persona(), nuevo));

        var historial = consentRepository.findByUsuarioIdOrderByFechaAceptacionDesc(ana.getId());

        assertThat(historial).extracting(UserConsent::getDocumentoVersion).containsExactly("2.0", "1.0");
    }
}
