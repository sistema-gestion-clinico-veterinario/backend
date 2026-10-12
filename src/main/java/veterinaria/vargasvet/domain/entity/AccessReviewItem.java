package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import veterinaria.vargasvet.domain.enums.AccessReviewDecision;

@Getter
@Setter
@Entity
@Table(name = "access_review_item")
public class AccessReviewItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private AccessReview review;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(name = "user_name", nullable = false, length = 200)
    private String userName;

    @Column(name = "user_email", length = 254)
    private String userEmail;

    @Column(name = "role_id")
    private Integer roleId;

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    @Column(name = "reasons", nullable = false, length = 600)
    private String reasons;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 10)
    private AccessReviewDecision decision;

    @Column(name = "comment", length = 300)
    private String comment;
}
