package com.biodatamaker.repository;

import com.biodatamaker.entity.BioData;
import com.biodatamaker.entity.PaymentTransaction;
import com.biodatamaker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for PaymentTransaction entity operations.
 */
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    /**
     * Find all transactions by user
     */
    List<PaymentTransaction> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Find all transactions by user ID
     */
    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find all transactions by status
     */
    List<PaymentTransaction> findByStatusOrderByCreatedAtDesc(PaymentTransaction.PaymentStatus status);

    /**
     * Find all pending transactions (for admin)
     */
    List<PaymentTransaction> findByStatusOrderByCreatedAtAsc(PaymentTransaction.PaymentStatus status);

    /**
     * Find transaction by bio-data
     */
    Optional<PaymentTransaction> findByBioData(BioData bioData);

    /**
     * Find transaction by bio-data ID
     */
    Optional<PaymentTransaction> findByBioDataId(Long bioDataId);

    /**
     * Find all transactions by bio-data
     */
    List<PaymentTransaction> findAllByBioDataOrderByCreatedAtDesc(BioData bioData);

    /**
     * Check if approved payment exists for bio-data
     */
    boolean existsByBioDataAndStatus(BioData bioData, PaymentTransaction.PaymentStatus status);

    /**
     * Check if transaction reference ID already exists
     */
    boolean existsByTransactionReferenceId(String transactionReferenceId);

    /**
     * Find by transaction reference ID
     */
    Optional<PaymentTransaction> findByTransactionReferenceId(String transactionReferenceId);

    /**
     * Count pending transactions
     */
    long countByStatus(PaymentTransaction.PaymentStatus status);

    /**
     * Find transactions created after a certain date
     */
    List<PaymentTransaction> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime date);

    /**
     * Get total successful payments amount
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentTransaction p WHERE p.status = :status")
    java.math.BigDecimal getTotalAmountByStatus(@Param("status") PaymentTransaction.PaymentStatus status);

    /**
     * Find pending payments for admin dashboard
     */
    @Query("SELECT p FROM PaymentTransaction p " +
            "JOIN FETCH p.user " +
            "JOIN FETCH p.bioData " +
            "WHERE p.status = :status " +
            "ORDER BY p.createdAt ASC")
    List<PaymentTransaction> findPendingPaymentsWithDetails(@Param("status") PaymentTransaction.PaymentStatus status);
}
