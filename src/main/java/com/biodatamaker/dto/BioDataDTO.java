package com.biodatamaker.dto;

import com.biodatamaker.entity.BioData;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Data Transfer Object for BioData entity.
 * Used for form binding and data transfer between layers.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BioDataDTO {

    private Long id;

    // ================== Personal Details ==================
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String fullName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    private String birthTime;

    private String birthPlace;

    @NotNull(message = "Gender is required")
    private BioData.Gender gender;

    private String religion;

    private String caste;

    private String subCaste;

    private String gotra;

    private String rashi;

    private String nakshatra;

    private String manglikStatus;

    private String bloodGroup;

    private String complexion;

    private String height;

    private String weight;

    private BioData.MaritalStatus maritalStatus;

    private String physicalStatus;

    private String motherTongue;

    private String knownLanguages;

    private String diet;

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

    @Min(value = 0, message = "Number of brothers cannot be negative")
    private Integer numberOfBrothers;

    @Min(value = 0, message = "Number of sisters cannot be negative")
    private Integer numberOfSisters;

    @Min(value = 0, message = "Brothers married cannot be negative")
    private Integer brothersMarried;

    @Min(value = 0, message = "Sisters married cannot be negative")
    private Integer sistersMarried;

    private String familyType;

    private String familyStatus;

    private String familyValues;

    private String nativePlace;

    private String familyDetails;

    // ================== Contact Details ==================
    @Pattern(regexp = "^[0-9]{10}$", message = "Contact number must be 10 digits")
    private String contactNumber;

    private String alternateNumber;

    @Email(message = "Please provide a valid email address")
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
    @Size(max = 2000, message = "About me cannot exceed 2000 characters")
    private String aboutMe;

    @Size(max = 2000, message = "Hobbies cannot exceed 2000 characters")
    private String hobbiesAndInterests;

    // ================== Custom Fields ==================
    // Stored as JSON string: [{"fieldName": "Field Name", "fieldValue": "Value"}, ...]
    private String customFields;

    // ================== Photo & Template ==================
    private String photoPath;

    private String selectedTemplateId;

    // ================== Status ==================
    private BioData.BioDataStatus status;

    private Boolean isPaid;

    /**
     * Convert DTO to Entity
     */
    public BioData toEntity() {
        return BioData.builder()
                .id(this.id)
                .fullName(this.fullName)
                .dateOfBirth(this.dateOfBirth)
                .birthTime(this.birthTime)
                .birthPlace(this.birthPlace)
                .gender(this.gender)
                .religion(this.religion)
                .caste(this.caste)
                .subCaste(this.subCaste)
                .gotra(this.gotra)
                .rashi(this.rashi)
                .nakshatra(this.nakshatra)
                .manglikStatus(this.manglikStatus)
                .bloodGroup(this.bloodGroup)
                .complexion(this.complexion)
                .height(this.height)
                .weight(this.weight)
                .maritalStatus(this.maritalStatus)
                .physicalStatus(this.physicalStatus)
                .motherTongue(this.motherTongue)
                .knownLanguages(this.knownLanguages)
                .diet(this.diet)
                .smokingHabit(this.smokingHabit)
                .drinkingHabit(this.drinkingHabit)
                .highestQualification(this.highestQualification)
                .educationDetails(this.educationDetails)
                .collegeName(this.collegeName)
                .universityName(this.universityName)
                .specialization(this.specialization)
                .passingYear(this.passingYear)
                .occupation(this.occupation)
                .employerName(this.employerName)
                .designation(this.designation)
                .workingCity(this.workingCity)
                .annualIncome(this.annualIncome)
                .fatherName(this.fatherName)
                .fatherOccupation(this.fatherOccupation)
                .motherName(this.motherName)
                .motherOccupation(this.motherOccupation)
                .numberOfBrothers(this.numberOfBrothers)
                .numberOfSisters(this.numberOfSisters)
                .brothersMarried(this.brothersMarried)
                .sistersMarried(this.sistersMarried)
                .familyType(this.familyType)
                .familyStatus(this.familyStatus)
                .familyValues(this.familyValues)
                .nativePlace(this.nativePlace)
                .familyDetails(this.familyDetails)
                .contactNumber(this.contactNumber)
                .alternateNumber(this.alternateNumber)
                .emailAddress(this.emailAddress)
                .currentAddress(this.currentAddress)
                .permanentAddress(this.permanentAddress)
                .city(this.city)
                .state(this.state)
                .country(this.country)
                .pincode(this.pincode)
                .preferredAgeRange(this.preferredAgeRange)
                .preferredHeightRange(this.preferredHeightRange)
                .preferredEducation(this.preferredEducation)
                .preferredOccupation(this.preferredOccupation)
                .preferredLocation(this.preferredLocation)
                .preferredMaritalStatus(this.preferredMaritalStatus)
                .otherPreferences(this.otherPreferences)
                .aboutMe(this.aboutMe)
                .hobbiesAndInterests(this.hobbiesAndInterests)
                .customFields(this.customFields)
                .photoPath(this.photoPath)
                .selectedTemplateId(this.selectedTemplateId != null && !this.selectedTemplateId.isBlank() ? this.selectedTemplateId : "traditional")
                .status(this.status != null ? this.status : BioData.BioDataStatus.DRAFT)
                .isPaid(this.isPaid != null ? this.isPaid : false)
                .build();
    }

    /**
     * Create DTO from Entity
     */
    public static BioDataDTO fromEntity(BioData entity) {
        return BioDataDTO.builder()
                .id(entity.getId())
                .fullName(entity.getFullName())
                .dateOfBirth(entity.getDateOfBirth())
                .birthTime(entity.getBirthTime())
                .birthPlace(entity.getBirthPlace())
                .gender(entity.getGender())
                .religion(entity.getReligion())
                .caste(entity.getCaste())
                .subCaste(entity.getSubCaste())
                .gotra(entity.getGotra())
                .rashi(entity.getRashi())
                .nakshatra(entity.getNakshatra())
                .manglikStatus(entity.getManglikStatus())
                .bloodGroup(entity.getBloodGroup())
                .complexion(entity.getComplexion())
                .height(entity.getHeight())
                .weight(entity.getWeight())
                .maritalStatus(entity.getMaritalStatus())
                .physicalStatus(entity.getPhysicalStatus())
                .motherTongue(entity.getMotherTongue())
                .knownLanguages(entity.getKnownLanguages())
                .diet(entity.getDiet())
                .smokingHabit(entity.getSmokingHabit())
                .drinkingHabit(entity.getDrinkingHabit())
                .highestQualification(entity.getHighestQualification())
                .educationDetails(entity.getEducationDetails())
                .collegeName(entity.getCollegeName())
                .universityName(entity.getUniversityName())
                .specialization(entity.getSpecialization())
                .passingYear(entity.getPassingYear())
                .occupation(entity.getOccupation())
                .employerName(entity.getEmployerName())
                .designation(entity.getDesignation())
                .workingCity(entity.getWorkingCity())
                .annualIncome(entity.getAnnualIncome())
                .fatherName(entity.getFatherName())
                .fatherOccupation(entity.getFatherOccupation())
                .motherName(entity.getMotherName())
                .motherOccupation(entity.getMotherOccupation())
                .numberOfBrothers(entity.getNumberOfBrothers())
                .numberOfSisters(entity.getNumberOfSisters())
                .brothersMarried(entity.getBrothersMarried())
                .sistersMarried(entity.getSistersMarried())
                .familyType(entity.getFamilyType())
                .familyStatus(entity.getFamilyStatus())
                .familyValues(entity.getFamilyValues())
                .nativePlace(entity.getNativePlace())
                .familyDetails(entity.getFamilyDetails())
                .contactNumber(entity.getContactNumber())
                .alternateNumber(entity.getAlternateNumber())
                .emailAddress(entity.getEmailAddress())
                .currentAddress(entity.getCurrentAddress())
                .permanentAddress(entity.getPermanentAddress())
                .city(entity.getCity())
                .state(entity.getState())
                .country(entity.getCountry())
                .pincode(entity.getPincode())
                .preferredAgeRange(entity.getPreferredAgeRange())
                .preferredHeightRange(entity.getPreferredHeightRange())
                .preferredEducation(entity.getPreferredEducation())
                .preferredOccupation(entity.getPreferredOccupation())
                .preferredLocation(entity.getPreferredLocation())
                .preferredMaritalStatus(entity.getPreferredMaritalStatus())
                .otherPreferences(entity.getOtherPreferences())
                .aboutMe(entity.getAboutMe())
                .hobbiesAndInterests(entity.getHobbiesAndInterests())
                .customFields(entity.getCustomFields())
                .photoPath(entity.getPhotoPath())
                .selectedTemplateId(entity.getSelectedTemplateId())
                .status(entity.getStatus())
                .isPaid(entity.getIsPaid())
                .build();
    }
}
