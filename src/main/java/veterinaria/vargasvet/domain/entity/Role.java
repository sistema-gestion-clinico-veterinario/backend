package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;

@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @EqualsAndHashCode.Include
    @Column(name = "name", nullable = false)
    private String name;

    @Column
    private String descripcion;

    @Column(name = "activo", nullable = false, columnDefinition = "boolean default true")
    private boolean activo = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 16)
    private RoleScope scope = RoleScope.STAFF;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 32)
    private RolePurpose purpose = RolePurpose.CUSTOM;

    @Column(name = "system_managed", nullable = false)
    private boolean systemManaged = false;

    @Column(name = "protected", nullable = false)
    private boolean protectedRole = false;

    @Column(name = "permission_version", nullable = false)
    private long permissionVersion = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;
}
