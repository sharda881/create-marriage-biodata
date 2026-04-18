package com.biodatamaker.repository;

import com.biodatamaker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email address
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by phone number
     */
    Optional<User> findByPhone(String phone);

    /**
     * Find user by email or phone
     */
    Optional<User> findByEmailOrPhone(String email, String phone);

    /**
     * Find user by OAuth provider and provider ID
     */
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Check if phone exists
     */
    boolean existsByPhone(String phone);

    /**
     * Find all users by role
     */
    List<User> findByRole(User.Role role);

    /**
     * Find all enabled users
     */
    List<User> findByEnabledTrue();

    /**
     * Count users by role
     */
    long countByRole(User.Role role);

    /**
     * Search users by name (case-insensitive)
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> searchByName(@Param("searchTerm") String searchTerm);

    /**
     * Find users who have created bio-data
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.bioDatas b WHERE b IS NOT NULL")
    List<User> findUsersWithBioData();
}
