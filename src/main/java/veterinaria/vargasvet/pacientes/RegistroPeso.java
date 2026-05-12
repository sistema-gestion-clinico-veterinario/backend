package veterinaria.vargasvet.pacientes;

import jakarta.persistence.*;
import lombok.Data;
import veterinaria.vargasvet.clinica.HistoriaClinica;
import veterinaria.vargasvet.admin.Empleado;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "registro_peso")
public class RegistroPeso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "historia_clinica_id", nullable = false)
    private HistoriaClinica historiaClinica;

    @Column(nullable = false)
    private Double peso;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinario_id")
    private Empleado veterinario;
}
