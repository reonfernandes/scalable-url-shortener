package com.reon.urlservice.respository;

import com.reon.urlservice.model.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<UrlMapping, String> {
    Optional<UrlMapping> findByShortCode(String shortCode);
    boolean existsByShortCode(String shortCode);
    List<UrlMapping> findAllByUserId(String userId);

    long countByUserIdAndActiveTrue(String userId);

    @Modifying
    @Query("UPDATE UrlMapping u SET u.active = false WHERE u.urlId = :urlId")
    int deactivateUrl(@Param("urlId") String urlId);

    @Modifying
    @Query("UPDATE UrlMapping u SET u.active = true WHERE u.urlId = :urlId")
    int activateUrl(@Param("urlId") String urlId);

    @Modifying
    @Query("UPDATE UrlMapping u SET u.active = false WHERE u.expiresAt <= :now AND u.active = true")
    int deactivateExpiredUrls(@Param("now") LocalDateTime now);

    @Query("SELECT u FROM UrlMapping u WHERE u.expiresAt <= :now AND u.expiresAt > :since AND u.active = false")
    List<UrlMapping> findRecentlyExpired(@Param("now") LocalDateTime now, @Param("since") LocalDateTime since);

    @Modifying
    @Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + 1 WHERE u.shortCode = :shortCode")
    void incrementClickCount(@Param("shortCode") String shortCode);
}
