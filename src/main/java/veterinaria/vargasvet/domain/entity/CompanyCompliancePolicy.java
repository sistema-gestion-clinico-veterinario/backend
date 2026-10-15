package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import veterinaria.vargasvet.domain.enums.ControlCumplimientoClinica;
import veterinaria.vargasvet.domain.enums.ModoGestionCumplimiento;

import java.time.LocalDateTime;

@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "company_compliance_policy")
public class CompanyCompliancePolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "company_id") private Company company;
    @Enumerated(EnumType.STRING) @Column(name = "control_code", nullable = false, length = 40)
    private ControlCumplimientoClinica control;
    @Column(nullable = false) private int version;
    @Column(nullable = false) private boolean habilitado;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private ModoGestionCumplimiento modo;
    @Column(nullable = false) private boolean bloqueante;
    @Column(nullable = false, columnDefinition = "TEXT") private String justificacion;
    @Column(name = "referencia_contractual", length = 200) private String referenciaContractual;
    @Column(name = "confirmo_revision_legal", nullable = false) private boolean confirmoRevisionLegal;
    @Column(name = "vigente_desde", nullable = false) private LocalDateTime vigenteDesde;
    @Column(name = "creada_at", nullable = false, updatable = false) private LocalDateTime creadaAt;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "creada_por_usuario_id") private Usuario creadaPor;
}
