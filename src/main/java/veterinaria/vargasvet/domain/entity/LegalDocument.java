package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import veterinaria.vargasvet.domain.enums.LegalDocumentType;

import java.time.LocalDateTime;

/**
 * Una versión publicada de un documento legal. El texto, la versión y la fecha no cambian una vez
 * publicados (solo se retira, con {@code activo}); un cambio es una versión nueva.
 */
@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "legal_document", uniqueConstraints = {
        @UniqueConstraint(name = "ux_legal_document_tipo_version", columnNames = {"tipo", "version"})
})
public class LegalDocument {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private LegalDocumentType tipo;

    @Column(nullable = false, updatable = false)
    private String version;

    @Column(nullable = false, columnDefinition = "TEXT", updatable = false)
    private String contenido;

    /** SHA-256 (hex) del contenido publicado. */
    @Column(name = "contenido_hash", nullable = false, length = 64, updatable = false)
    private String contenidoHash;

    @Column(name = "vigente_desde", nullable = false, updatable = false)
    private LocalDateTime vigenteDesde;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;
}
