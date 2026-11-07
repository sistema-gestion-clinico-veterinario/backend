package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import veterinaria.vargasvet.domain.enums.ClinicalConceptOrigin;
import veterinaria.vargasvet.domain.enums.ClinicalValueType;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "clinical_concept")
public class ClinicalConcept {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "code", nullable = false, length = 120)
    private String code;

    @Column(name = "base_name", nullable = false, length = 160)
    private String baseName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 24)
    private ClinicalValueType valueType;

    @Column(name = "unit", length = 40)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, length = 16)
    private ClinicalConceptOrigin origin;

    @Column(name = "reportable", nullable = false)
    private boolean reportable;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_config", nullable = false)
    private String validationConfig = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = veterinaria.vargasvet.util.AppClock.now();
    }
}
