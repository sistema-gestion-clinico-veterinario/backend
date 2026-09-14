package veterinaria.vargasvet.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "usuario_por_rol", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"usuario_id", "rol_id"})
})
public class UsuarioPorRol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    @JsonIgnoreProperties({"usuariosPorRol", "empleado", "apoderado", "password", "verificationToken"})
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id")
    @JsonIgnoreProperties({"company"})
    private Role rol;

    /** Empresa para la que aplica esta asignacion (null solo para roles globales,
     * ej. PLATFORM_ADMIN). Poblado con la misma empresa de la relacion (Empleado/
     * Apoderado/UsuarioMembresia) activa al momento de crear la asignacion - ver
     * CompanyMembershipService. Fuente de verdad para autorizacion por empresa;
     * ya no depende de Usuario.company ni de cruzar con Role.company. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @OneToMany(mappedBy = "usuarioPorRol", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UsuarioPorRolPermiso> permisos;
}
