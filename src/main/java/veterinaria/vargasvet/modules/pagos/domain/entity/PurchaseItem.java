package veterinaria.vargasvet.modules.pagos.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import veterinaria.vargasvet.modules.inventory.domain.entity.Producto_Tienda;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "purchase_items")
public class PurchaseItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "quantity")
    private Integer quantity;

    @ManyToOne
    @JoinColumn(name = "producto_id", referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "FK_purchase_item_producto"))
    private Producto_Tienda producto;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "purchase_id", referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "FK_purhcase_item_purchase"))
    public Purchase purchase;
}
