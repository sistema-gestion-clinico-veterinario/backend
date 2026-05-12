package veterinaria.vargasvet.pacientes;

import jakarta.persistence.*;
import lombok.Data;
import veterinaria.vargasvet.shared.SeveridadAlergia;
import veterinaria.vargasvet.shared.TipoAlergia;
import veterinaria.vargasvet.clinica.HistoriaClinica;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "registro_alergias")
public class RegistroAlergia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "historia_clinica_id", nullable = false)
    private HistoriaClinica historiaClinica;

    @Column(nullable = false, length = 200)
    private String agente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAlergia tipo;

    @Column(columnDefinition = "TEXT")
    private String reaccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeveridadAlergia severidad;

    @Column(name = "fecha_deteccion")
    private LocalDate fechaDeteccion;

    @Column(name = "activa", nullable = false)
    private Boolean activa = true;
}
