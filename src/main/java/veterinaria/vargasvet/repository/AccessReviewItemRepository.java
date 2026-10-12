package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import veterinaria.vargasvet.domain.entity.AccessReviewItem;

import java.util.List;

public interface AccessReviewItemRepository extends JpaRepository<AccessReviewItem, Long> {

    List<AccessReviewItem> findByReviewIdOrderByIdAsc(Long reviewId);
}
