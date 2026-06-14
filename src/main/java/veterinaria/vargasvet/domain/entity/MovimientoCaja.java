package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.ConceptoMovimiento;
import veterinaria.vargasvet.domain.enums.TipoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "movimiento_caja")
public class MovimientoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimiento tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConceptoMovimiento concepto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "cita_id")
    private Long citaId;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "registrado_por", length = 150)
    private String registradoPor;

    @Column(name = "company_id", nullable = false)
    private Integer companyId;

    @PrePersist
    protected void onCreate() {
        if (fecha == null) fecha = veterinaria.vargasvet.util.AppClock.now();
    }
}
