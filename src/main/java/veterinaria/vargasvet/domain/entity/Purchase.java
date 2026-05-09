package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.MetodoPago;
import veterinaria.vargasvet.domain.enums.PaymentStatus;
import veterinaria.vargasvet.domain.enums.TipoDocumentoVenta;
import veterinaria.vargasvet.domain.enums.TipoPurchase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "purchases")
public class Purchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "total", precision = 10, scale = 2)
    private BigDecimal total;
    private LocalDateTime createdAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus;
    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL)
    private List<PurchaseItem> items;
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id"
            , foreignKey = @ForeignKey(name = "FK_purchase_user"))
    private Usuario user;

    /** Referencia a la cita cuando el pago corresponde a un servicio veterinario */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id", nullable = true,
            foreignKey = @ForeignKey(name = "FK_purchase_cita"))
    private Cita cita;

    /** Tipo de compra: TIENDA, SERVICIO_CITA o MEDICAMENTOS_CITA */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_purchase")
    private TipoPurchase tipoPurchase;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago")
    private MetodoPago metodoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento")
    private TipoDocumentoVenta tipoDocumento;

    @Column(name = "monto_recibido", precision = 10, scale = 2)
    private BigDecimal montoRecibido;

    @Column(name = "comprobante_transferencia")
    private String comprobanteTransferencia;

    @Column(name = "verificado_por")
    private String verificadoPor;

    @Column(name = "verificado_at")
    private LocalDateTime verificadoAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = true)
    private Usuario cliente;

    @Column(name = "cliente_nombre_walkin")
    private String clienteNombreWalkin;

    @Column(name = "cliente_doc_walkin")
    private String clienteDocWalkin;

}