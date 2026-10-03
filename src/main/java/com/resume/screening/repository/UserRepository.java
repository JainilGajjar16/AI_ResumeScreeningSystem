package com.resume.screening.repository;

import com.resume.screening.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameIgnoreCase(String username);
    Optional<User> findByEmailIgnoreCase(String email);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE LOWER(TRIM(u.username)) = LOWER(TRIM(:identifier)) OR LOWER(TRIM(u.email)) = LOWER(TRIM(:identifier))")
    Optional<User> findByUsernameOrEmail(@org.springframework.data.repository.query.Param("identifier") String identifier);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);
}
