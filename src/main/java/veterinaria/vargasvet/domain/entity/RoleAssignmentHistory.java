package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import veterinaria.vargasvet.domain.enums.RoleAssignmentAction;
import veterinaria.vargasvet.domain.enums.RoleScope;

import java.time.LocalDateTime;

/**
 * Evidencia inmutable de quién concedió o retiró un rol. Guarda instantáneas de
 * los datos descriptivos para que eliminar o renombrar un rol no altere el pasado.
 */
@Getter
@Setter
@Entity
@Table(name = "role_assignment_history")
public class RoleAssignmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(name = "rol_id")
    private Integer rolId;

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    @Column(name = "company_id")
    private Integer companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_scope", nullable = false, length = 20)
    private RoleScope roleScope;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 16)
    private RoleAssignmentAction action;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "performed_by_user_id")
    private Integer performedByUserId;

    @Column(name = "performed_by_email", length = 254)
    private String performedByEmail;
}
