package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.EstadoDispensacion;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dispensaciones_medicamento")
public class DispensacionMedicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescripcion_id", nullable = false, unique = true)
    private Prescripcion prescripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = true)
    private Producto_Tienda producto;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_item_id", nullable = true)
    private PurchaseItem purchaseItem;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoDispensacion estado = EstadoDispensacion.PENDIENTE;

    @Column(name = "fecha_dispensacion")
    private LocalDateTime fechaDispensacion;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
