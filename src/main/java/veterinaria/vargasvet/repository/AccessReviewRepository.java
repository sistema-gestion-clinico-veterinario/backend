package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import veterinaria.vargasvet.domain.entity.AccessReview;

import java.util.List;
import java.util.Optional;

public interface AccessReviewRepository extends JpaRepository<AccessReview, Long> {

    Optional<AccessReview> findFirstByCompanyIdOrderByPerformedAtDescIdDesc(Integer companyId);

    List<AccessReview> findByCompanyIdOrderByPerformedAtDescIdDesc(Integer companyId, Pageable pageable);
}
