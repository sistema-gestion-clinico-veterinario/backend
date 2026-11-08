package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import veterinaria.vargasvet.domain.enums.ClinicalTemplateStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "clinical_template")
public class ClinicalTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "template_key", nullable = false, length = 80)
    private String templateKey;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "applicable_species", length = 300)
    private String applicableSpecies;

    @Column(name = "is_default", nullable = false)
    private boolean defaultTemplate;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ClinicalTemplateStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "based_on_id")
    private ClinicalTemplate basedOn;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "published_by")
    private String publishedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @BatchSize(size = 32)
    private List<ClinicalTemplateSection> sections = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "clinical_template_service",
            joinColumns = @JoinColumn(name = "template_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id"))
    @OrderBy("nombre ASC")
    private Set<ServiciosVeterinarios> services = new LinkedHashSet<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = veterinaria.vargasvet.util.AppClock.now();
    }
}
