package veterinaria.vargasvet.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "apoderado")
public class Apoderado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private Usuario user;

    /** Empresa de ESTA relacion cliente especifica. A diferencia de Empleado, un mismo
     * Usuario puede tener varias filas Apoderado ACTIVAS simultaneamente (cliente de
     * varias empresas a la vez) - por eso no hay un indice "a lo sumo una activa" aqui. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    @Column(name = "fecha_salida")
    private LocalDate fechaSalida;

    /** numero_documento se mantiene reservado dentro de la misma empresa incluso inactivo
     * (indice uq_apoderado_documento_empresa, sin filtro de estado) - por eso el reingreso
     * a la MISMA empresa reactiva esta fila (estado=true) en vez de crear una nueva. */
    @Column(name = "estado", nullable = false)
    private Boolean estado = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento_identidad", nullable = false)
    private TipoDocumentoIdentidad tipoDocumentoIdentidad;

    // Unicidad real: uq_apoderado_documento_empresa (numero_documento, company_id),
    // sin filtro de estado - no es un unique=true global (permite que la misma
    // persona sea cliente activo de varias empresas a la vez, ver clase de arriba).
    @Column(name = "numero_documento", nullable = false)
    private String numeroDocumento;

    @Enumerated(EnumType.STRING)
    @Column(name = "genero", nullable = false)
    private Genero genero;

    @Column(columnDefinition = "TEXT")
    private String referencias;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @OneToMany(mappedBy = "apoderado", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    private List<Mascota> mascotas;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "estado_modificado_por")
    private String estadoModificadoPor;

    @Column(name = "fecha_modificacion_estado")
    private LocalDateTime fechaModificacionEstado;

    @PrePersist
    protected void onCreate() {
        createdAt = veterinaria.vargasvet.util.AppClock.now();
        updatedAt = veterinaria.vargasvet.util.AppClock.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = veterinaria.vargasvet.util.AppClock.now();
    }
}
