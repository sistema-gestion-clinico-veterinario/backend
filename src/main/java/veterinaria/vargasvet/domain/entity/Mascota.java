package veterinaria.vargasvet.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.SexoMascota;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    private String especie;

    private LocalDate fechaNacimiento;

    @Column(name = "peso", nullable = true)
    private Double peso;

    @Enumerated(EnumType.STRING)
    @Column(name = "sexo")
    private SexoMascota sexo;

    @Column(name = "color")
    private String color;

    @Column(name = "senas_particulares", columnDefinition = "TEXT")
    private String senasParticulares;

    @Column(name = "esterilizado", nullable = false)
    private Boolean esterilizado;

    @ManyToMany
    @JoinTable(
            name = "mascota_apoderado",
            joinColumns = @JoinColumn(name = "mascota_id"),
            inverseJoinColumns = @JoinColumn(name = "apoderado_id")
    )
    private List<Apoderado> apoderados;

    @OneToOne(mappedBy = "mascota", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private HistoriaClinica historiaClinica;


    @Column(unique = true, nullable = false, length = 36)
    private String uuid;

    private LocalDateTime created_At;
    private LocalDateTime updated_At;

}
