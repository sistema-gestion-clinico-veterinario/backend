package veterinaria.vargasvet.clinica;

import jakarta.persistence.*;
import lombok.Data;
import veterinaria.vargasvet.pacientes.Mascota;
import veterinaria.vargasvet.pacientes.RegistroVacuna;
import veterinaria.vargasvet.pacientes.RegistroAlergia;
import veterinaria.vargasvet.pacientes.RegistroPeso;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "historia_clinica")
public class HistoriaClinica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_hc", unique = true, nullable = false, length = 20)
    private String numeroHc;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mascota_id", nullable = false, unique = true)
    private Mascota mascota;

    @Column(name = "antecedentes_personales", columnDefinition = "TEXT")
    private String antecedentesPersonales;

    @Column(name = "antecedentes_familiares", columnDefinition = "TEXT")
    private String antecedentesFamiliares;

    @Column(name = "enfermedades", columnDefinition = "TEXT")
    private String enfermedades;

    @Column(name = "procedimientos", columnDefinition = "TEXT")
    private String procedimientos;

    @Column(name = "grupo_sanguineo", length = 30)
    private String grupoSanguineo;

    @Column(name = "activa", nullable = false)
    private Boolean activa = true;

    @OneToMany(mappedBy = "historiaClinica", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Consulta> consultas = new ArrayList<>();

    @OneToMany(mappedBy = "historiaClinica", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RegistroVacuna> vacunas = new ArrayList<>();

    @OneToMany(mappedBy = "historiaClinica", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RegistroAlergia> alergias = new ArrayList<>();

    @OneToMany(mappedBy = "historiaClinica", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RegistroPeso> historialPeso = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
