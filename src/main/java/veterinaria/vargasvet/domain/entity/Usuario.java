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

    /** Dato de contacto. Unico dentro de la misma empresa (o entre cuentas sin empresa,
     * SuperAdmin) - nunca globalmente, para que dos empresas nunca puedan cruzar datos
     * entre si por coincidencia de correo. Ver los indices unicos parciales en V74. */
    @Column(nullable = false)
    private String email;

    /** Identificador de login. Unico dentro de la misma empresa (o entre cuentas sin
     * empresa, SuperAdmin) - nunca globalmente, mismo motivo que el correo. La persona
     * lo elige al registrarse. Ver los indices unicos parciales en V74. */
    @Column(nullable = false)
    private String username;

    @Column
    private String nombre;

    @Column
    private String apellido;

    @Column
    private String dni;

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
}

