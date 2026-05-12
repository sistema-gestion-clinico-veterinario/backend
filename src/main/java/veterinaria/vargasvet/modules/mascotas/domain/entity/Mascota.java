package veterinaria.vargasvet.modules.mascotas.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import veterinaria.vargasvet.modules.mascotas.domain.enums.EspecieMascota;
import veterinaria.vargasvet.modules.mascotas.domain.enums.MotivoBajaMascota;
import veterinaria.vargasvet.modules.mascotas.domain.enums.SexoMascota;
import veterinaria.vargasvet.modules.users.domain.entity.Apoderado;
import veterinaria.vargasvet.modules.clinical.domain.entity.HistoriaClinica;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "mascota")
public class Mascota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombreCompleto;

    @Column(nullable = false)
    private String raza;

    @Enumerated(EnumType.STRING)
    @Column(name = "especie", nullable = false)
    private EspecieMascota especie;

    @Column(name = "otra_especie")
    private String otraEspecie; 

    private LocalDate fechaNacimiento;

    @Column(name = "peso")
    private Double peso;

    @Enumerated(EnumType.STRING)
    @Column(name = "sexo")
    private SexoMascota sexo;

    @Column(name = "color")
    private String color;

    @Column(name = "senas_particulares", columnDefinition = "TEXT")
    private String senasParticulares;

    @Column(name = "foto_url")
    private String fotoUrl;

    @Column(name = "numero_microchip", length = 50)
    private String numeroMicrochip;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "esterilizado", nullable = false)
    private Boolean esterilizado = false;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apoderado_id", nullable = false)
    @JsonIgnore
    private Apoderado apoderado;

    @OneToOne(mappedBy = "mascota", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private HistoriaClinica historiaClinica;

    @Column(unique = true, nullable = false, length = 36)
    private String uuid;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_baja")
    private MotivoBajaMascota motivoBaja;

    @Column(name = "otro_motivo_baja")
    private String otroMotivoBaja;

    @Column(name = "estado_modificado_por")
    private String estadoModificadoPor;

    @Column(name = "fecha_modificacion_estado")
    private LocalDateTime fechaModificacionEstado;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
