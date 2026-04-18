package com.biodatamaker.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * BioData entity containing all marriage bio-data information.
 * Stores personal, educational, family, and contact details.
 */
@Entity
@Table(name = "bio_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BioData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    // ================== Personal Details ==================
    @Column(nullable = false)
    private String fullName;

    private LocalDate dateOfBirth;

    private String birthTime;

    private String birthPlace;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private String religion;

    private String caste;

    private String subCaste;

    private String gotra;

    private String rashi; // Zodiac sign

    private String nakshatra; // Birth star

    private String manglikStatus; // Yes/No/Partial

    private String bloodGroup;

    private String complexion;

    private String height;

    private String weight;

    @Enumerated(EnumType.STRING)
    private MaritalStatus maritalStatus;

    private String physicalStatus; // Normal/Physically Challenged

    private String motherTongue;

    private String knownLanguages;

    private String diet; // Vegetarian/Non-Vegetarian/Eggetarian

    private String smokingHabit;

    private String drinkingHabit;

    // ================== Education Details ==================
    private String highestQualification;

    private String educationDetails;

    private String collegeName;

    private String universityName;

    private String specialization;

    private Integer passingYear;

    // ================== Professional Details ==================
    private String occupation;

    private String employerName;

    private String designation;

    private String workingCity;

    private String annualIncome;

    // ================== Family Details ==================
    private String fatherName;

    private String fatherOccupation;

    private String motherName;

    private String motherOccupation;

    private Integer numberOfBrothers;

    private Integer numberOfSisters;

    private Integer brothersMarried;

    private Integer sistersMarried;

    private String familyType; // Joint/Nuclear

    private String familyStatus; // Middle Class/Upper Middle Class/Rich/Affluent

    private String familyValues; // Traditional/Moderate/Liberal

    private String nativePlace;

    private String familyDetails; // Additional family info

    // ================== Contact Details ==================
    private String contactNumber;

    private String alternateNumber;

    private String emailAddress;

    private String currentAddress;

    private String permanentAddress;

    private String city;

    private String state;

    private String country;

    private String pincode;

    // ================== Partner Preferences ==================
    private String preferredAgeRange;

    private String preferredHeightRange;

    private String preferredEducation;

    private String preferredOccupation;

    private String preferredLocation;

    private String preferredMaritalStatus;

    private String otherPreferences;

    // ================== About Me ==================
    @Column(length = 2000)
    private String aboutMe;

    @Column(length = 2000)
    private String hobbiesAndInterests;

    // ================== Custom Fields ==================
    // Stored as JSON string: [{"fieldName": "Field Name", "fieldValue": "Value"}, ...]
    @Column(length = 4000)
    private String customFields;

    // ================== Photo ==================
    private String photoPath;

    // ================== Template Selection ==================
    @Column(nullable = false)
    @Builder.Default
    private String selectedTemplateId = "modern";

    // ================== Status Flags ==================
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BioDataStatus status = BioDataStatus.DRAFT;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPaid = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer downloadCount = 0;

    // ================== Timestamps ==================
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime lastDownloadedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Gender enumeration
     */
    public enum Gender {
        MALE,
        FEMALE,
        OTHER
    }

    /**
     * Marital status enumeration
     */
    public enum MaritalStatus {
        NEVER_MARRIED,
        DIVORCED,
        WIDOWED,
        AWAITING_DIVORCE,
        ANNULLED
    }

    /**
     * BioData status enumeration
     */
    public enum BioDataStatus {
        DRAFT,
        COMPLETED,
        PUBLISHED
    }

    /**
     * Get formatted age from date of birth
     */
    public Integer getAge() {
        if (dateOfBirth == null) return null;
        return java.time.Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    /**
     * Check if bio-data has minimum required fields filled
     */
    public boolean isMinimallyComplete() {
        return fullName != null && !fullName.isBlank()
                && dateOfBirth != null
                && gender != null;
    }
}
