package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.enums.RolePurpose;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Role r where r.id = :id")
    Optional<Role> findByIdForPermissionUpdate(@Param("id") Integer id);

    Optional<Role> findByName(String name);
    List<Role> findAllByName(String name);
    /** Roles que pertenecen a una empresa específica */
    List<Role> findByCompanyId(Integer companyId);

    /** Roles del sistema (sin empresa = SUPER_ADMIN, CLIENTE, etc.) */
    List<Role> findByCompanyIsNull();

    Optional<Role> findByNameAndCompanyId(String name, Integer companyId);

    Optional<Role> findFirstByName(String name);
    Optional<Role> findFirstByCompanyIdAndPurpose(Integer companyId, RolePurpose purpose);
    Optional<Role> findFirstByCompanyIsNullAndPurpose(RolePurpose purpose);

    /** Verifica existencia de nombre dentro de una empresa */
    boolean existsByNameAndCompanyId(String name, Integer companyId);

    /** Verifica existencia de nombre en roles del sistema */
    boolean existsByNameAndCompanyIsNull(String name);
    boolean existsByName(String name);
}
