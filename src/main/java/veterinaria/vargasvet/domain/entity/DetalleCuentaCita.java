package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.TipoDetalleCuenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "detalle_cuenta_cita")
public class DetalleCuentaCita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cita_id", nullable = false)
    private Cita cita;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoDetalleCuenta tipo;

    @Column(nullable = false, length = 180)
    private String descripcion;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "es_servicio_base", nullable = false)
    private Boolean esServicioBase = false;

    @Column(name = "registrado_por", length = 150)
    private String registradoPor;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = veterinaria.vargasvet.util.AppClock.now();
    }
}
