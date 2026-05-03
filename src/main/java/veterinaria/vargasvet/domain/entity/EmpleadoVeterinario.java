package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "empleado_veterinario")
public class EmpleadoVeterinario {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "apellido", nullable = false)
    private String apellido;

    @Column(name = "fecha_nacimiento", nullable = true)
    private LocalDate fechaNacimiento;

    @Column(name = "direccion", nullable = true)
    private String direccion;
    @ToString.Exclude
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "empleado_especialidad",
            joinColumns = @JoinColumn(name = "empleado_id"),
            inverseJoinColumns = @JoinColumn(name = "especialidad_id"))
    private Set<Especialidad> especialidades = new HashSet<>();
    @ToString.Exclude
    @OneToMany(mappedBy = "empleadoVeterinario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<EmpleadoServicio> servicios = new HashSet<>();
    @Enumerated(EnumType.STRING)
    @Column(name = "tipoDocumentoIdentidad", nullable = false)
    private TipoDocumentoIdentidad tipoDocumentoIdentidad;
    @Column(name = "numeroDocumentoIdentidad",nullable = false)
    private String numeroDocumentoIdentidad;
    @Enumerated(EnumType.STRING)
    @Column(name = "Genero", nullable = false)
    private Genero genero;

    @Column(name = "telefono", nullable = true)
    private String telefono;

    @Column(name = "estado", nullable = false)
    private Boolean estado;
    @Column(name = "email")
    private String email;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "empleado_tipo_empleado",
            joinColumns = @JoinColumn(name = "empleado_id"),
            inverseJoinColumns = @JoinColumn(name = "tipo_empleado_id")
    )
    private Set<TipoEmpleado> tiposEmpleado = new HashSet<>();


    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @OneToOne(cascade = CascadeType.ALL)
    private Usuario user;

    private LocalDateTime created_At;
    private LocalDateTime updated_At;
}
