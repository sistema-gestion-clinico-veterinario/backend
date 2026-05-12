package veterinaria.vargasvet.admin;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import veterinaria.vargasvet.shared.Genero;
import veterinaria.vargasvet.shared.TipoDocumentoIdentidad;
import veterinaria.vargasvet.servicios.Especialidad;
import veterinaria.vargasvet.servicios.EmpleadoServicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "empleado")
public class Empleado {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @ToString.Exclude
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "empleado_especialidad",
            joinColumns = @JoinColumn(name = "empleado_id"),
            inverseJoinColumns = @JoinColumn(name = "especialidad_id"))
    private Set<Especialidad> especialidades = new HashSet<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "empleado", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<EmpleadoServicio> servicios = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento_identidad", nullable = false)
    private TipoDocumentoIdentidad tipoDocumentoIdentidad;

    @Column(name = "numero_documento_identidad", nullable = false)
    private String numeroDocumentoIdentidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "genero", nullable = false)
    private Genero genero;

    @Column(name = "estado", nullable = false)
    private Boolean estado = true;

    @Column(name = "numero_colegiatura", unique = true) // Nullable para no-veterinarios
    private String numeroColegiatura;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "foto_url")
    private String fotoUrl;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "empleado_tipo_empleado",
            joinColumns = @JoinColumn(name = "empleado_id"),
            inverseJoinColumns = @JoinColumn(name = "tipo_empleado_id")
    )
    private Set<TipoEmpleado> tiposEmpleado = new HashSet<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private Usuario user;

    @OneToMany(mappedBy = "empleado", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("diaSemana ASC")
    private List<HorarioEmpleado> horarios = new ArrayList<>();

    @Column(name = "estado_modificado_por")
    private String estadoModificadoPor;

    @Column(name = "fecha_modificacion_estado")
    private LocalDateTime fechaModificacionEstado;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
