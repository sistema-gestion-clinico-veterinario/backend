package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.RealtimeTicket;

import java.time.Instant;

public interface RealtimeTicketRepository extends JpaRepository<RealtimeTicket, String> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RealtimeTicket t
               set t.usedAt = :usedAt
             where t.jti = :jti
               and t.usedAt is null
               and t.expiresAt >= :usedAt
            """)
    int consumeOnce(@Param("jti") String jti, @Param("usedAt") Instant usedAt);

    long deleteByExpiresAtBefore(Instant threshold);
}
