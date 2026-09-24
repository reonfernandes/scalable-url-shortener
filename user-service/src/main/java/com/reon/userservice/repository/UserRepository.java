package com.reon.userservice.repository;

import com.reon.userservice.model.User;
import com.reon.userservice.model.type.Tier;
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

    @Modifying
    @Query("UPDATE User u SET u.tier = :tier WHERE u.userId = :userId")
    void updateTier(@Param("userId") String userId, @Param("tier") Tier tier);

    @Modifying
    @Query("UPDATE User u SET u.urlCount = u.urlCount + 1 WHERE u.userId = :userId")
    void incrementUrlCount(@Param("userId") String userId);

    @Modifying
    @Query("UPDATE User u SET u.urlCount = GREATEST(u.urlCount - 1, 0) WHERE u.userId = :userId")
    void decrementUserUrlCount(@Param("userId") String userId);

    // admin specific
    @Modifying
    @Query("UPDATE User u SET u.active = false WHERE u.userId = :userId")
    void deactivateUser(@Param("userId") String userId);

    @Modifying
    @Query("UPDATE User u SET u.active = true WHERE u.userId = :userId")
    void activateUser(@Param("userId") String userId);
}
