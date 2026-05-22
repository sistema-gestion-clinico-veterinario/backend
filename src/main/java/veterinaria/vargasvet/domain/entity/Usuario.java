package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

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

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Empleado empleado;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Apoderado apoderado;

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

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;
}

