package com.reon.userservice.repository;

import com.reon.userservice.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);

    // admin specific
    // search (name or email, any case) and active are both optional: null means "don't filter"
    @Query("""
            SELECT u FROM User u
            WHERE (:search IS NULL
                   OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:active IS NULL OR u.active = :active)
            """)
    Page<User> searchUsers(@Param("search") String search, @Param("active") Boolean active, Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.active = false WHERE u.userId = :userId")
    void deactivateUser(@Param("userId") String userId);

    @Modifying
    @Query("UPDATE User u SET u.active = true WHERE u.userId = :userId")
    void activateUser(@Param("userId") String userId);
}
