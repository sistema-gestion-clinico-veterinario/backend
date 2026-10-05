package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import veterinaria.vargasvet.domain.enums.LegalDocumentType;

import java.time.LocalDateTime;

/**
 * Constancia de que una persona aceptó una versión de un documento legal. Solo se inserta: no se
 * modifica ni se elimina. Copia el tipo, la versión y la huella del texto para poder demostrar qué se
 * aceptó aunque el documento cambie después; las constancias anteriores a la versión inmutable no
 * tienen huella y se marcan como no recuperables.
 */
@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "user_consent", uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_consent_usuario_documento", columnNames = {"usuario_id", "legal_document_id"})
})
public class UserConsent {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_document_id", nullable = false)
    private LegalDocument legalDocument;

    @Enumerated(EnumType.STRING)
    @Column(name = "documento_tipo", nullable = false, length = 30)
    private LegalDocumentType documentoTipo;

    @Column(name = "documento_version", nullable = false, length = 20)
    private String documentoVersion;

    @Column(name = "contenido_hash", length = 64)
    private String contenidoHash;

    @Column(name = "texto_recuperable", nullable = false)
    private boolean textoRecuperable = true;

    @Column(name = "fecha_aceptacion", nullable = false)
    private LocalDateTime fechaAceptacion;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;
}
