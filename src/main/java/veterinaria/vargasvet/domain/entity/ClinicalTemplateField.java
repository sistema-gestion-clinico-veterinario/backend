package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "clinical_template_field")
public class ClinicalTemplateField {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private ClinicalTemplateSection section;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concept_id", nullable = false)
    private ClinicalConcept concept;

    @Column(name = "visible_label", nullable = false, length = 160)
    private String visibleLabel;

    @Column(name = "help_text", length = 500)
    private String helpText;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Column(name = "visible", nullable = false)
    private boolean visible = true;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "applicable_species", length = 300)
    private String applicableSpecies;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "visibility_rules", nullable = false)
    private String visibilityRules = "{}";
}
