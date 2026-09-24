package com.reon.urlservice.repository;

import com.reon.urlservice.model.UrlMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<UrlMapping, Long> {
    Optional<UrlMapping> findByShortCode(String shortCode);
    boolean existsByShortCode(String shortCode);

    Page<UrlMapping> findByUserId(String userId, Pageable pageable);

    long countByUserIdAndActiveTrue(String userId);

    @Modifying
    @Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + 1 WHERE u.shortCode = :shortCode")
    void incrementClickCount(@Param("shortCode") String shortCode);

    // for kafka events
    @Query("SELECT u.shortCode FROM UrlMapping u WHERE u.userId = :userId")
    List<String> findShortCodesByUserId(@Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM UrlMapping u WHERE u.userId = :userId")
    void deleteUserUrls(@Param("userId") String userId);

    @Modifying
    @Query("UPDATE UrlMapping u SET u.active = true WHERE u.userId = :userId")
    void activateUserUrls(@Param("userId") String userId);

    @Modifying
    @Query("UPDATE UrlMapping u SET u.active = false WHERE u.userId = :userId")
    void deactivateUserUrls(@Param("userId") String userId);
}
