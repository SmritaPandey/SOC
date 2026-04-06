package com.qshield.idam.repository;
import com.qshield.idam.model.IdamUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface IdamUserRepository extends JpaRepository<IdamUser, Long> {
    Optional<IdamUser> findByUsername(String username);
    Optional<IdamUser> findByEmail(String email);
    boolean existsByUsername(String username);
    long countByRole(String role);
    long countByEnabled(boolean enabled);
}
