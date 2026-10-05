package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "aviso_privacidad", uniqueConstraints = {
        @UniqueConstraint(name = "uq_aviso_privacidad_company_version", columnNames = {"company_id", "version"})
})
public class AvisoPrivacidad {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, updatable = false)
    private Company company;

    @Column(nullable = false, updatable = false)
    private Integer version;

    @Column(nullable = false, columnDefinition = "TEXT", updatable = false)
    private String contenido;

    @Column(name = "contenido_hash", nullable = false, length = 64, updatable = false)
    private String contenidoHash;

    @Column(nullable = false, columnDefinition = "TEXT", updatable = false)
    private String campos;

    @Column(name = "vigente_desde", nullable = false, updatable = false)
    private LocalDateTime vigenteDesde;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "creado_por", length = 150, updatable = false)
    private String creadoPor;
}
