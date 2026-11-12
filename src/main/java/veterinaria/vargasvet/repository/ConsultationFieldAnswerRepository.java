package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.ConsultationFieldAnswer;

import java.util.List;

public interface ConsultationFieldAnswerRepository extends JpaRepository<ConsultationFieldAnswer, Long> {
    @Query("SELECT a FROM ConsultationFieldAnswer a JOIN FETCH a.templateField f JOIN FETCH f.concept WHERE a.consultation.id = :consultationId")
    List<ConsultationFieldAnswer> findAllByConsultationId(@Param("consultationId") Long consultationId);
}
