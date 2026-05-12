package veterinaria.vargasvet.modules.clinical.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import veterinaria.vargasvet.modules.clinical.domain.enums.EstadoDiagnostico;
import veterinaria.vargasvet.modules.clinical.domain.enums.TipoDiagnostico;

@Data
@Entity
@Table(name = "diagnosticos")
public class Diagnostico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_id", nullable = false)
    private Consulta consulta;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(name = "codigo_cie", length = 20)
    private String codigoCIE;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoDiagnostico tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoDiagnostico estado;
}
