package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "usuario")
public class Usuario {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column
    private String nombre;

    @Column
    private String apellido;

    @Column
    private String dni;

    @Column
    private String telefono;

    @Column
    private String direccion;

    /** Un Usuario puede tener varias filas Empleado a lo largo del tiempo (una por
     * empresa/periodo), pero como mucho una activa a la vez. Sin cascade/orphanRemoval
     * a proposito: la baja es logica (estado=false), nunca se borra via esta relacion.
     * Para "el empleado activo" usar EmpleadoRepository.findActiveByUserId(usuario.getId()),
     * no navegar esta lista asumiendo una sola fila. */
    @ToString.Exclude
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Empleado> empleados = new java.util.ArrayList<>();

    /** A diferencia de Empleado, puede haber varias filas Apoderado ACTIVAS a la vez
     * (cliente de varias empresas simultaneamente). Usar ApoderadoRepository segun el
     * caso: findByUserIdAndCompanyId para una empresa puntual, o findAllActiveByUserId
     * para la lista completa de empresas donde es cliente activo. */
    @ToString.Exclude
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Apoderado> apoderados = new java.util.ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private java.util.List<UsuarioPorRol> usuariosPorRol = new java.util.ArrayList<>();

    @Column(nullable = false)
    private boolean passwordChanged = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "activo", nullable = false)
    private boolean activo = false;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "verification_token_expires_at")
    private java.time.LocalDateTime verificationTokenExpiresAt;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "credentials_version", nullable = false)
    private long credentialsVersion = 0L;
}

