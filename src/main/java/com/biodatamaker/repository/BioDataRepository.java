package com.biodatamaker.repository;

import com.biodatamaker.entity.BioData;
import com.biodatamaker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for BioData entity operations.
 */
@Repository
public interface BioDataRepository extends JpaRepository<BioData, Long> {

    /**
     * Find all bio-data by user
     */
    List<BioData> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Find all bio-data by user ID
     */
    List<BioData> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Count bio-data by user
     */
    long countByUser(User user);

    /**
     * Count bio-data by user ID
     */
    long countByUserId(Long userId);

    /**
     * Find bio-data by ID and user
     */
    Optional<BioData> findByIdAndUser(Long id, User user);

    /**
     * Find bio-data by ID and user ID
     */
    Optional<BioData> findByIdAndUserId(Long id, Long userId);

    /**
     * Find all bio-data with a specific status
     */
    List<BioData> findByStatus(BioData.BioDataStatus status);

    /**
     * Find all bio-data by user and status
     */
    List<BioData> findByUserAndStatus(User user, BioData.BioDataStatus status);

    /**
     * Find all paid bio-data by user
     */
    List<BioData> findByUserAndIsPaidTrue(User user);

    /**
     * Find all unpaid bio-data by user
     */
    List<BioData> findByUserAndIsPaidFalse(User user);

    /**
     * Count paid bio-data by user
     */
    @Query("SELECT COUNT(b) FROM BioData b WHERE b.user = :user AND b.isPaid = true")
    long countPaidByUser(@Param("user") User user);

    /**
     * Count downloaded bio-data by user (downloadCount > 0)
     */
    @Query("SELECT COUNT(b) FROM BioData b WHERE b.user = :user AND b.downloadCount > 0")
    long countDownloadedByUser(@Param("user") User user);

    /**
     * Find recent bio-data (limit 10)
     */
    @Query("SELECT b FROM BioData b ORDER BY b.createdAt DESC LIMIT 10")
    List<BioData> findRecentBioData();

    /**
     * Search bio-data by full name (case-insensitive)
     */
    @Query("SELECT b FROM BioData b WHERE LOWER(b.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<BioData> searchByFullName(@Param("searchTerm") String searchTerm);
}
