package com.biodatamaker.service;

import com.biodatamaker.dto.BioDataDTO;
import com.biodatamaker.entity.BioData;
import com.biodatamaker.entity.User;
import com.biodatamaker.exception.PaymentRequiredException;
import com.biodatamaker.exception.ResourceNotFoundException;
import com.biodatamaker.repository.BioDataRepository;
import com.biodatamaker.template.BioDataTemplate;
import com.biodatamaker.template.BioDataTemplateFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imgscalr.Scalr;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for BioData management operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BioDataService {

    private final BioDataRepository bioDataRepository;
    private final BioDataTemplateFactory templateFactory;
    private final SystemConfigService configService;

    private static final String UPLOAD_DIR = "./uploads/photos";
    private static final int MAX_IMAGE_WIDTH = 1024;

    /**
     * Create a new bio-data draft
     */
    @Transactional
    public BioData createBioData(User user, BioDataDTO dto) {
        BioData bioData = dto.toEntity();
        bioData.setUser(user); // Can be null for anonymous users
        bioData.setStatus(BioData.BioDataStatus.DRAFT);
        bioData.setIsPaid(false);

        // Set default template if not specified
        if (bioData.getSelectedTemplateId() == null || bioData.getSelectedTemplateId().isBlank()) {
            bioData.setSelectedTemplateId("traditional");
        }

        BioData saved = bioDataRepository.save(bioData);
        if (user != null) {
            log.info("Created new bio-data draft for user {}: {}", user.getId(), saved.getId());
        } else {
            log.info("Created new bio-data draft for anonymous user: {}", saved.getId());
        }
        return saved;
    }

    /**
     * Update existing bio-data
     */
    @Transactional
    public BioData updateBioData(Long bioDataId, User user, BioDataDTO dto) {
        BioData bioData;
        if (user != null) {
            bioData = getBioDataForUser(bioDataId, user);
        } else {
            // Anonymous user - just get by ID
            bioData = getBioDataById(bioDataId);
        }

        // Update fields from DTO
        updateBioDataFields(bioData, dto);

        BioData saved = bioDataRepository.save(bioData);
        if (user != null) {
            log.info("Updated bio-data {} for user {}", bioDataId, user.getId());
        } else {
            log.info("Updated bio-data {} for anonymous user", bioDataId);
        }
        return saved;
    }

    /**
     * Save bio-data as draft (partial save)
     */
    @Transactional
    public BioData saveDraft(Long bioDataId, User user, BioDataDTO dto) {
        BioData bioData;

        if (bioDataId != null) {
            bioData = getBioDataForUser(bioDataId, user);
        } else {
            bioData = new BioData();
            bioData.setUser(user);
            bioData.setStatus(BioData.BioDataStatus.DRAFT);
        }

        updateBioDataFields(bioData, dto);
        bioData.setStatus(BioData.BioDataStatus.DRAFT);

        BioData saved = bioDataRepository.save(bioData);
        log.info("Saved bio-data draft {} for user {}", saved.getId(), user.getId());
        return saved;
    }

    /**
     * Mark bio-data as completed
     */
    @Transactional
    public BioData completeBioData(Long bioDataId, User user) {
        BioData bioData;
        if (user != null) {
            bioData = getBioDataForUser(bioDataId, user);
        } else {
            // Anonymous user - just get by ID
            bioData = getBioDataById(bioDataId);
        }

        if (!bioData.isMinimallyComplete()) {
            throw new IllegalArgumentException("Bio-data is missing required fields");
        }

        bioData.setStatus(BioData.BioDataStatus.COMPLETED);
        return bioDataRepository.save(bioData);
    }

    /**
     * Get bio-data by ID for a specific user
     */
    public BioData getBioDataForUser(Long bioDataId, User user) {
        return bioDataRepository.findByIdAndUser(bioDataId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Bio-data not found: " + bioDataId));
    }

    /**
     * Get bio-data by ID (for admin or PDF generation)
     */
    public BioData getBioDataById(Long bioDataId) {
        return bioDataRepository.findById(bioDataId)
                .orElseThrow(() -> new ResourceNotFoundException("Bio-data not found: " + bioDataId));
    }

    /**
     * Get all bio-datas for a user
     */
    public List<BioDataDTO> getUserBioDataList(User user) {
        return bioDataRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(BioDataDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Count bio-data by user
     */
    public long countUserBioData(User user) {
        return bioDataRepository.countByUser(user);
    }

    /**
     * Check if user has exceeded free limit and needs to pay
     */
    public boolean needsPayment(User user, Long bioDataId) {
        if (!configService.isPaywallEnabled()) {
            return false;
        }

        BioData bioData = getBioDataForUser(bioDataId, user);

        // Already paid
        if (bioData.getIsPaid()) {
            return false;
        }

        int freeLimit = configService.getFreeLimitCount();
        long downloadedCount = bioDataRepository.countDownloadedByUser(user);

        // User has free downloads remaining
        return downloadedCount >= freeLimit;
    }

    /**
     * Validate download access (throws exception if payment needed)
     */
    public void validateDownloadAccess(User user, Long bioDataId) {
        if (needsPayment(user, bioDataId)) {
            throw new PaymentRequiredException(bioDataId);
        }
    }

    /**
     * Increment download count
     */
    @Transactional
    public void incrementDownloadCount(Long bioDataId) {
        BioData bioData = getBioDataById(bioDataId);
        bioData.setDownloadCount(bioData.getDownloadCount() + 1);
        bioData.setLastDownloadedAt(LocalDateTime.now());
        bioDataRepository.save(bioData);
    }

    /**
     * Mark bio-data as paid
     */
    @Transactional
    public void markAsPaid(Long bioDataId) {
        BioData bioData = getBioDataById(bioDataId);
        bioData.setIsPaid(true);
        bioDataRepository.save(bioData);
        log.info("Bio-data {} marked as paid", bioDataId);
    }

    /**
     * Uploads and resizes a photo for a given bio-data.
     *
     * @param bioDataId The ID of the bio-data.
     * @param user      The current user (can be null for anonymous).
     * @param file      The uploaded image file.
     * @return The relative path to the saved, resized image.
     * @throws IOException If an error occurs during file processing.
     */
    @Transactional
    public String uploadPhoto(Long bioDataId, User user, MultipartFile file) throws IOException {
        BioData bioData = (user != null) ? getBioDataForUser(bioDataId, user) : getBioDataById(bioDataId);
        String extension = getFileExtension(file.getOriginalFilename());
        return resizeAndSaveImage(bioData, file.getInputStream(), extension);
    }

    /**
     * Uploads and resizes a photo from a Base64 encoded string.
     *
     * @param bioDataId  The ID of the bio-data.
     * @param user       The current user (can be null for anonymous).
     * @param base64Data The Base64 encoded image data string.
     * @return The relative path to the saved, resized image.
     * @throws IOException If an error occurs during file processing.
     */
    @Transactional
    public String uploadPhotoFromBase64(Long bioDataId, User user, String base64Data) throws IOException {
        BioData bioData = (user != null) ? getBioDataForUser(bioDataId, user) : getBioDataById(bioDataId);

        String[] parts = base64Data.split(",");
        if (parts.length != 2) throw new IllegalArgumentException("Invalid base64 image data");

        String mimeType = parts[0];
        String extension = ".jpg"; // Default
        if (mimeType.contains("png")) extension = ".png";
        else if (mimeType.contains("gif")) extension = ".gif";
        else if (mimeType.contains("webp")) extension = ".webp";

        byte[] imageBytes = java.util.Base64.getDecoder().decode(parts[1]);
        return resizeAndSaveImage(bioData, new ByteArrayInputStream(imageBytes), extension);
    }

    /**
     * Resizes an image to a standard width, saves it, and updates the bio-data record.
     *
     * @param bioData     The bio-data entity to update.
     * @param inputStream The input stream of the image data.
     * @param extension   The file extension (e.g., ".jpg").
     * @return The relative path to the saved image.
     * @throws IOException If an error occurs during image processing or saving.
     */
    private String resizeAndSaveImage(BioData bioData, InputStream inputStream, String extension) throws IOException {
        // Read the original image
        BufferedImage originalImage = ImageIO.read(inputStream);
        if (originalImage == null) {
            throw new IOException("Could not read image file.");
        }

        // Resize the image only if it's wider than the max width
        BufferedImage resizedImage = originalImage;
        if (originalImage.getWidth() > MAX_IMAGE_WIDTH) {
            resizedImage = Scalr.resize(originalImage, Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_WIDTH, MAX_IMAGE_WIDTH);
        }

        // Convert the resized image back to a byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String formatName = extension.startsWith(".") ? extension.substring(1) : extension;
        ImageIO.write(resizedImage, formatName, outputStream);
        byte[] resizedImageBytes = outputStream.toByteArray();

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate a unique filename and save the file
        String filename = "photo_" + bioData.getId() + "_" + UUID.randomUUID() + extension;
        Path filePath = uploadPath.resolve(filename);
        Files.write(filePath, resizedImageBytes);

        // Update the bio-data with the new photo path
        String relativePath = "/uploads/photos/" + filename;
        bioData.setPhotoPath(relativePath);
        bioDataRepository.save(bioData);

        log.info("Resized and uploaded photo for bio-data {}: {}", bioData.getId(), relativePath);
        return relativePath;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg"; // Default extension
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * Update template selection - supports both authenticated and anonymous users
     */
    @Transactional
    public BioData updateTemplate(Long bioDataId, User user, String templateId) {
        BioData bioData;
        if (user != null) {
            bioData = getBioDataForUser(bioDataId, user);
        } else {
            // Anonymous user - just get by ID
            bioData = getBioDataById(bioDataId);
        }

        // Validate template exists
        BioDataTemplate template = templateFactory.getTemplate(templateId);

        bioData.setSelectedTemplateId(templateId);
        BioData saved = bioDataRepository.save(bioData);

        if (user != null) {
            log.info("Updated template to {} for bio-data {} (user {})", templateId, bioDataId, user.getId());
        } else {
            log.info("Updated template to {} for bio-data {} (anonymous user)", templateId, bioDataId);
        }

        return saved;
    }

    /**
     * Delete bio-data
     */
    @Transactional
    public void deleteBioData(Long bioDataId, User user) {
        BioData bioData = getBioDataForUser(bioDataId, user);

        // Delete associated photo if exists
        if (bioData.getPhotoPath() != null) {
            try {
                Path photoPath = Paths.get("." + bioData.getPhotoPath());
                Files.deleteIfExists(photoPath);
            } catch (IOException e) {
                log.warn("Could not delete photo file: {}", bioData.getPhotoPath());
            }
        }

        bioDataRepository.delete(bioData);
        log.info("Deleted bio-data {} for user {}", bioDataId, user.getId());
    }

    /**
     * Get recent bio-datas (for admin dashboard)
     */
    public List<BioDataDTO> getRecentBioData() {
        return bioDataRepository.findRecentBioData().stream()
                .map(BioDataDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get template for bio-data
     */
    public BioDataTemplate getTemplateForBioData(BioData bioData) {
        String selectedId = bioData.getSelectedTemplateId();
        BioDataTemplate template = templateFactory.getTemplateOrDefault(selectedId);
        log.info("getTemplateForBioData - BioData ID: {}, selectedTemplateId: '{}', returned template ID: '{}'",
                bioData.getId(), selectedId, template.getTemplateId());
        return template;
    }

    // Helper method to update bio-data fields from DTO
    private void updateBioDataFields(BioData bioData, BioDataDTO dto) {
        if (dto.getFullName() != null) bioData.setFullName(dto.getFullName());
        if (dto.getDateOfBirth() != null) bioData.setDateOfBirth(dto.getDateOfBirth());
        if (dto.getBirthTime() != null) bioData.setBirthTime(dto.getBirthTime());
        if (dto.getBirthPlace() != null) bioData.setBirthPlace(dto.getBirthPlace());
        if (dto.getGender() != null) bioData.setGender(dto.getGender());
        if (dto.getReligion() != null) bioData.setReligion(dto.getReligion());
        if (dto.getCaste() != null) bioData.setCaste(dto.getCaste());
        if (dto.getSubCaste() != null) bioData.setSubCaste(dto.getSubCaste());
        if (dto.getGotra() != null) bioData.setGotra(dto.getGotra());
        if (dto.getRashi() != null) bioData.setRashi(dto.getRashi());
        if (dto.getNakshatra() != null) bioData.setNakshatra(dto.getNakshatra());
        if (dto.getManglikStatus() != null) bioData.setManglikStatus(dto.getManglikStatus());
        if (dto.getBloodGroup() != null) bioData.setBloodGroup(dto.getBloodGroup());
        if (dto.getComplexion() != null) bioData.setComplexion(dto.getComplexion());
        if (dto.getHeight() != null) bioData.setHeight(dto.getHeight());
        if (dto.getWeight() != null) bioData.setWeight(dto.getWeight());
        if (dto.getMaritalStatus() != null) bioData.setMaritalStatus(dto.getMaritalStatus());
        if (dto.getPhysicalStatus() != null) bioData.setPhysicalStatus(dto.getPhysicalStatus());
        if (dto.getMotherTongue() != null) bioData.setMotherTongue(dto.getMotherTongue());
        if (dto.getKnownLanguages() != null) bioData.setKnownLanguages(dto.getKnownLanguages());
        if (dto.getDiet() != null) bioData.setDiet(dto.getDiet());
        if (dto.getSmokingHabit() != null) bioData.setSmokingHabit(dto.getSmokingHabit());
        if (dto.getDrinkingHabit() != null) bioData.setDrinkingHabit(dto.getDrinkingHabit());

        // Education
        if (dto.getHighestQualification() != null) bioData.setHighestQualification(dto.getHighestQualification());
        if (dto.getEducationDetails() != null) bioData.setEducationDetails(dto.getEducationDetails());
        if (dto.getCollegeName() != null) bioData.setCollegeName(dto.getCollegeName());
        if (dto.getUniversityName() != null) bioData.setUniversityName(dto.getUniversityName());
        if (dto.getSpecialization() != null) bioData.setSpecialization(dto.getSpecialization());
        if (dto.getPassingYear() != null) bioData.setPassingYear(dto.getPassingYear());

        // Professional
        if (dto.getOccupation() != null) bioData.setOccupation(dto.getOccupation());
        if (dto.getEmployerName() != null) bioData.setEmployerName(dto.getEmployerName());
        if (dto.getDesignation() != null) bioData.setDesignation(dto.getDesignation());
        if (dto.getWorkingCity() != null) bioData.setWorkingCity(dto.getWorkingCity());
        if (dto.getAnnualIncome() != null) bioData.setAnnualIncome(dto.getAnnualIncome());

        // Family
        if (dto.getFatherName() != null) bioData.setFatherName(dto.getFatherName());
        if (dto.getFatherOccupation() != null) bioData.setFatherOccupation(dto.getFatherOccupation());
        if (dto.getMotherName() != null) bioData.setMotherName(dto.getMotherName());
        if (dto.getMotherOccupation() != null) bioData.setMotherOccupation(dto.getMotherOccupation());
        if (dto.getNumberOfBrothers() != null) bioData.setNumberOfBrothers(dto.getNumberOfBrothers());
        if (dto.getNumberOfSisters() != null) bioData.setNumberOfSisters(dto.getNumberOfSisters());
        if (dto.getBrothersMarried() != null) bioData.setBrothersMarried(dto.getBrothersMarried());
        if (dto.getSistersMarried() != null) bioData.setSistersMarried(dto.getSistersMarried());
        if (dto.getFamilyType() != null) bioData.setFamilyType(dto.getFamilyType());
        if (dto.getFamilyStatus() != null) bioData.setFamilyStatus(dto.getFamilyStatus());
        if (dto.getFamilyValues() != null) bioData.setFamilyValues(dto.getFamilyValues());
        if (dto.getNativePlace() != null) bioData.setNativePlace(dto.getNativePlace());
        if (dto.getFamilyDetails() != null) bioData.setFamilyDetails(dto.getFamilyDetails());

        // Contact
        if (dto.getContactNumber() != null) bioData.setContactNumber(dto.getContactNumber());
        if (dto.getAlternateNumber() != null) bioData.setAlternateNumber(dto.getAlternateNumber());
        if (dto.getEmailAddress() != null) bioData.setEmailAddress(dto.getEmailAddress());
        if (dto.getCurrentAddress() != null) bioData.setCurrentAddress(dto.getCurrentAddress());
        if (dto.getPermanentAddress() != null) bioData.setPermanentAddress(dto.getPermanentAddress());
        if (dto.getCity() != null) bioData.setCity(dto.getCity());
        if (dto.getState() != null) bioData.setState(dto.getState());
        if (dto.getCountry() != null) bioData.setCountry(dto.getCountry());
        if (dto.getPincode() != null) bioData.setPincode(dto.getPincode());

        // Partner Preferences
        if (dto.getPreferredAgeRange() != null) bioData.setPreferredAgeRange(dto.getPreferredAgeRange());
        if (dto.getPreferredHeightRange() != null) bioData.setPreferredHeightRange(dto.getPreferredHeightRange());
        if (dto.getPreferredEducation() != null) bioData.setPreferredEducation(dto.getPreferredEducation());
        if (dto.getPreferredOccupation() != null) bioData.setPreferredOccupation(dto.getPreferredOccupation());
        if (dto.getPreferredLocation() != null) bioData.setPreferredLocation(dto.getPreferredLocation());
        if (dto.getPreferredMaritalStatus() != null) bioData.setPreferredMaritalStatus(dto.getPreferredMaritalStatus());
        if (dto.getOtherPreferences() != null) bioData.setOtherPreferences(dto.getOtherPreferences());

        // About
        if (dto.getAboutMe() != null) bioData.setAboutMe(dto.getAboutMe());
        if (dto.getHobbiesAndInterests() != null) bioData.setHobbiesAndInterests(dto.getHobbiesAndInterests());

        // Custom Fields
        if (dto.getCustomFields() != null) bioData.setCustomFields(dto.getCustomFields());

        // Template - only update if a valid template ID is provided
        if (dto.getSelectedTemplateId() != null && !dto.getSelectedTemplateId().isBlank()) {
            log.debug("Updating template ID from '{}' to '{}'", bioData.getSelectedTemplateId(), dto.getSelectedTemplateId());
            bioData.setSelectedTemplateId(dto.getSelectedTemplateId());
        }
    }
}
