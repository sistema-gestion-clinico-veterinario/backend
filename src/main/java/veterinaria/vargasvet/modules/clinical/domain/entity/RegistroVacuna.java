package veterinaria.vargasvet.modules.clinical.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import veterinaria.vargasvet.modules.users.domain.entity.Empleado;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "registro_vacunas")
public class RegistroVacuna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "historia_clinica_id", nullable = false)
    private HistoriaClinica historiaClinica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_id", nullable = true)
    private Consulta consulta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinario_id")
    private Empleado veterinario;

    @Column(name = "nombre_vacuna", nullable = false, length = 200)
    private String nombreVacuna;

    @Column(length = 100)
    private String laboratorio;

    @Column(name = "numero_lote", length = 50)
    private String numeroLote;

    @Column(name = "fecha_aplicacion", nullable = false)
    private LocalDate fechaAplicacion;

    @Column(name = "fecha_proxima_dosis")
    private LocalDate fechaProximaDosis;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
