package com.biodatamaker.service;

import com.biodatamaker.dto.BioDataDTO;
import com.biodatamaker.dto.BioDataPreviewDTO;
import com.biodatamaker.dto.TemplateDTO;
import com.biodatamaker.entity.BioData;
import com.biodatamaker.template.BioDataTemplate;
import com.biodatamaker.template.PdfThemeRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the presentation model for a bio-data + template. Shared by {@code PdfService}
 * (Thymeleaf context for the PDF) and the REST layer ({@link BioDataPreviewDTO} for the
 * React preview) so both render paths derive the same fields the same way.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BioDataViewModel {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);

    /** Bundled brand mark, shown top-center on every PDF template regardless of theme. */
    private static final String GANESHA_LOGO_BASE64 = loadGaneshaLogoBase64();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PdfThemeRegistry pdfThemeRegistry;
    private final TemplatePricingService templatePricingService;

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * Thymeleaf context variables for the PDF templates (photo embedded as Base64).
     */
    public Map<String, Object> buildPdfContext(BioData bioData, BioDataTemplate template) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("bioData", bioData);
        vars.put("template", template);
        vars.put("theme", pdfThemeRegistry.themeFor(template.getTemplateId()));
        vars.put("formattedDob", formatDate(bioData.getDateOfBirth()));
        vars.put("age", bioData.getAge());
        vars.put("currentYear", LocalDate.now().getYear());

        String photoAsBase64 = null;
        if (bioData.getPhotoPath() != null && !bioData.getPhotoPath().isBlank()) {
            photoAsBase64 = encodeImageAsBase64("." + bioData.getPhotoPath());
        }
        vars.put("photoAsBase64", photoAsBase64);
        vars.put("hasPhoto", photoAsBase64 != null);
        vars.put("ganeshaLogoBase64", GANESHA_LOGO_BASE64);

        vars.put("hasEducation", hasEducation(bioData));
        vars.put("hasProfession", hasProfession(bioData));
        vars.put("hasFamily", hasFamily(bioData));
        vars.put("hasContact", hasContact(bioData));
        vars.put("hasPreferences", hasPreferences(bioData));
        vars.put("customFieldsMap", parseCustomFields(bioData.getCustomFields()));
        return vars;
    }

    /**
     * JSON preview model for the SPA (photo as an absolute URL).
     */
    public BioDataPreviewDTO buildPreview(BioData bioData, BioDataTemplate template, boolean needsPayment) {
        String photoUrl = null;
        if (bioData.getPhotoPath() != null && !bioData.getPhotoPath().isBlank()) {
            photoUrl = bioData.getPhotoPath().startsWith("http")
                    ? bioData.getPhotoPath()
                    : baseUrl + bioData.getPhotoPath();
        }

        BioDataDTO dto = BioDataDTO.fromEntity(bioData);
        List<String> lockedFields = needsPayment ? redactForPreview(dto) : List.of();
        String formattedDob = needsPayment ? "" : formatDate(bioData.getDateOfBirth());

        return new BioDataPreviewDTO(
                dto,
                TemplateDTO.fromTemplate(template, templatePricingService.priceFor(template.getTemplateId())),
                formattedDob,
                bioData.getAge(),
                LocalDate.now().getYear(),
                photoUrl,
                photoUrl != null,
                hasEducation(bioData),
                hasProfession(bioData),
                hasFamily(bioData),
                hasContact(bioData),
                hasPreferences(bioData),
                parseCustomFields(bioData.getCustomFields()),
                needsPayment,
                lockedFields
        );
    }

    /**
     * The rest of the detail fields, in section order. About half of whichever of these
     * are actually filled in get locked below, on top of the hard redactions, so a
     * screenshot of an unpaid preview is missing too much to substitute for the paid PDF.
     * Excludes identity fields (name, gender, photo) and the contact/address/exact-birth
     * fields, which are always redacted outright above.
     */
    private static final List<String> BLUR_ELIGIBLE_FIELDS = List.of(
            "religion", "caste", "subCaste", "gotra", "rashi", "nakshatra", "manglikStatus",
            "bloodGroup", "complexion", "height", "weight", "maritalStatus", "physicalStatus",
            "motherTongue", "knownLanguages", "diet", "smokingHabit", "drinkingHabit",
            "highestQualification", "educationDetails", "collegeName", "universityName", "specialization", "passingYear",
            "occupation", "employerName", "designation", "workingCity", "annualIncome",
            "fatherName", "fatherOccupation", "motherName", "motherOccupation",
            "numberOfBrothers", "numberOfSisters", "brothersMarried", "sistersMarried",
            "familyType", "familyStatus", "familyValues", "nativePlace", "familyDetails",
            "city", "state", "country",
            "preferredAgeRange", "preferredHeightRange", "preferredEducation", "preferredOccupation",
            "preferredLocation", "preferredMaritalStatus", "otherPreferences",
            "aboutMe", "hobbiesAndInterests"
    );

    /**
     * Strip contact / address / exact-birth details from an unpaid preview so a
     * screenshot can't be used in place of the paid PDF, then additionally lock about
     * half of every other filled-in field. Age is kept; the exact date of birth is not.
     * Returns the list of fields that were locked so the SPA can additionally blur/overlay them.
     */
    private List<String> redactForPreview(BioDataDTO dto) {
        List<String> locked = new ArrayList<>();

        if (isSet(dto.getContactNumber()))   { dto.setContactNumber(maskPhone(dto.getContactNumber())); locked.add("contactNumber"); }
        if (isSet(dto.getAlternateNumber())) { dto.setAlternateNumber(maskPhone(dto.getAlternateNumber())); locked.add("alternateNumber"); }
        if (isSet(dto.getEmailAddress()))    { dto.setEmailAddress(maskEmail(dto.getEmailAddress())); locked.add("emailAddress"); }
        if (isSet(dto.getCurrentAddress()))  { dto.setCurrentAddress(null); locked.add("currentAddress"); }
        if (isSet(dto.getPermanentAddress())){ dto.setPermanentAddress(null); locked.add("permanentAddress"); }
        if (isSet(dto.getPincode()))         { dto.setPincode(null); locked.add("pincode"); }
        if (dto.getDateOfBirth() != null)    { dto.setDateOfBirth(null); locked.add("dateOfBirth"); }
        if (isSet(dto.getBirthTime()))       { dto.setBirthTime(null); locked.add("birthTime"); }
        if (isSet(dto.getBirthPlace()))      { dto.setBirthPlace(null); locked.add("birthPlace"); }

        List<String> populated = BLUR_ELIGIBLE_FIELDS.stream()
                .filter(field -> isPropertySet(dto, field))
                .toList();
        for (int i = 0; i < populated.size(); i += 2) {
            clearProperty(dto, populated.get(i));
            locked.add(populated.get(i));
        }

        return locked;
    }

    private boolean isPropertySet(BioDataDTO dto, String property) {
        Object value = readProperty(dto, property);
        return value != null && !(value instanceof String s && s.isBlank());
    }

    private Object readProperty(BioDataDTO dto, String property) {
        try {
            return new PropertyDescriptor(property, BioDataDTO.class).getReadMethod().invoke(dto);
        } catch (ReflectiveOperationException | IntrospectionException e) {
            log.warn("Could not read preview field '{}': {}", property, e.getMessage());
            return null;
        }
    }

    private void clearProperty(BioDataDTO dto, String property) {
        try {
            new PropertyDescriptor(property, BioDataDTO.class).getWriteMethod().invoke(dto, (Object) null);
        } catch (ReflectiveOperationException | IntrospectionException e) {
            log.warn("Could not lock preview field '{}': {}", property, e.getMessage());
        }
    }

    private static boolean isSet(String s) {
        return s != null && !s.isBlank();
    }

    private static String maskPhone(String value) {
        String digits = value.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "••••";
        }
        String last2 = digits.substring(digits.length() - 2);
        return "•".repeat(Math.max(2, digits.length() - 2)) + last2;
    }

    private static String maskEmail(String value) {
        int at = value.indexOf('@');
        if (at <= 0) {
            return "••••";
        }
        String name = value.substring(0, at);
        String domain = value.substring(at);
        String head = name.length() <= 2 ? name.substring(0, 1) : name.substring(0, 2);
        return head + "•••" + domain;
    }

    public String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DATE_FORMATTER);
    }

    public Map<String, String> parseCustomFields(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse custom fields JSON: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private static String loadGaneshaLogoBase64() {
        try {
            byte[] bytes = StreamUtils.copyToByteArray(
                    new ClassPathResource("static/images/ganesha-logo.png").getInputStream());
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            log.warn("Could not load bundled Ganesha logo for PDF branding", e);
            return null;
        }
    }

    private String encodeImageAsBase64(String imagePath) {
        try {
            byte[] imageBytes = Files.readAllBytes(Paths.get(imagePath));
            String mimeType = "image/png";
            String lower = imagePath.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                mimeType = "image/jpeg";
            }
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            log.warn("Could not read image file for Base64 encoding: {}", imagePath, e);
            return null;
        }
    }

    private boolean hasEducation(BioData b) {
        return b.getHighestQualification() != null || b.getCollegeName() != null;
    }

    private boolean hasProfession(BioData b) {
        return b.getOccupation() != null || b.getEmployerName() != null;
    }

    private boolean hasFamily(BioData b) {
        return b.getFatherName() != null || b.getMotherName() != null;
    }

    private boolean hasContact(BioData b) {
        return b.getContactNumber() != null || b.getEmailAddress() != null;
    }

    private boolean hasPreferences(BioData b) {
        return b.getPreferredAgeRange() != null || b.getPreferredEducation() != null;
    }
}
