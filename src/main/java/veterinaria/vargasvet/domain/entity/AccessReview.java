package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "access_review")
public class AccessReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Integer companyId;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    @Column(name = "performed_by_user_id")
    private Integer performedByUserId;

    @Column(name = "performed_by_email", length = 254)
    private String performedByEmail;

    @Column(name = "reviewed_count", nullable = false)
    private int reviewedCount;

    @Column(name = "kept_count", nullable = false)
    private int keptCount;

    @Column(name = "revoked_count", nullable = false)
    private int revokedCount;

    @Column(name = "notes", length = 500)
    private String notes;
}
