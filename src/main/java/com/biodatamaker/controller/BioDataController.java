package com.biodatamaker.controller;

import com.biodatamaker.dto.BioDataDTO;
import com.biodatamaker.dto.BioDataPreviewDTO;
import com.biodatamaker.entity.BioData;
import com.biodatamaker.entity.User;
import com.biodatamaker.service.BioDataService;
import com.biodatamaker.service.BioDataViewModel;
import com.biodatamaker.service.PdfService;
import com.biodatamaker.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints for bio-data CRUD, photo upload, template selection, preview and PDF
 * download. Create / read-by-id / update / preview / download all support anonymous users
 * (no token), mirroring the original application.
 */
@RestController
@RequestMapping("/api/biodata")
@RequiredArgsConstructor
@Slf4j
public class BioDataController {

    private final BioDataService bioDataService;
    private final PdfService pdfService;
    private final BioDataViewModel viewModel;

    /** Create a new draft (anonymous allowed). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BioDataDTO create(@RequestBody BioDataDTO dto) {
        requireFullName(dto);
        User user = currentUserOrNull();
        return BioDataDTO.fromEntity(bioDataService.createBioData(user, dto));
    }

    /** Fetch one bio-data. Scoped to the owner when authenticated, by id otherwise. */
    @GetMapping("/{id}")
    public BioDataDTO get(@PathVariable Long id) {
        User user = currentUserOrNull();
        BioData bioData = user != null
                ? bioDataService.getBioDataForUser(id, user)
                : bioDataService.getBioDataById(id);
        return BioDataDTO.fromEntity(bioData);
    }

    /** Update a bio-data (anonymous allowed). */
    @PutMapping("/{id}")
    public BioDataDTO update(@PathVariable Long id, @RequestBody BioDataDTO dto) {
        User user = currentUserOrNull();
        return BioDataDTO.fromEntity(bioDataService.updateBioData(id, user, dto));
    }

    /** List the authenticated user's bio-data. */
    @GetMapping
    public List<BioDataDTO> list() {
        return bioDataService.getUserBioDataList(requireUser());
    }

    /** Delete a bio-data (owner only). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        bioDataService.deleteBioData(id, requireUser());
    }

    /** Mark a bio-data complete (anonymous allowed). */
    @PostMapping("/{id}/complete")
    public BioDataDTO complete(@PathVariable Long id) {
        return BioDataDTO.fromEntity(bioDataService.completeBioData(id, currentUserOrNull()));
    }

    /** Upload a photo (multipart). */
    @PostMapping("/{id}/photo")
    public Map<String, String> uploadPhoto(@PathVariable Long id,
                                           @RequestParam("photo") MultipartFile photo) {
        try {
            String path = bioDataService.uploadPhoto(id, currentUserOrNull(), photo);
            return Map.of("photoPath", path);
        } catch (IOException e) {
            log.error("Photo upload failed for bio-data {}", id, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo upload failed: " + e.getMessage());
        }
    }

    /** Upload a photo from a base64 data URL (used by the anonymous preview flow). */
    @PostMapping("/{id}/photo-base64")
    public Map<String, String> uploadPhotoBase64(@PathVariable Long id,
                                                 @RequestBody Map<String, String> body) {
        String data = body.get("data");
        if (data == null || !data.startsWith("data:image")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected a data:image base64 string in 'data'");
        }
        try {
            String path = bioDataService.uploadPhotoFromBase64(id, currentUserOrNull(), data);
            return Map.of("photoPath", path);
        } catch (IOException e) {
            log.error("Base64 photo upload failed for bio-data {}", id, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo upload failed: " + e.getMessage());
        }
    }

    /** Change the selected template (anonymous allowed). */
    @PutMapping("/{id}/template")
    public BioDataDTO updateTemplate(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String templateId = body.get("templateId");
        if (templateId == null || templateId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "templateId is required");
        }
        return BioDataDTO.fromEntity(bioDataService.updateTemplate(id, currentUserOrNull(), templateId));
    }

    /** Presentation model for rendering the preview with the selected template. */
    @GetMapping("/{id}/preview-data")
    public BioDataPreviewDTO previewData(@PathVariable Long id) {
        User user = currentUserOrNull();
        BioData bioData = user != null
                ? bioDataService.getBioDataForUser(id, user)
                : bioDataService.getBioDataById(id);
        boolean needsPayment = bioDataService.needsPayment(user, id);
        return viewModel.buildPreview(bioData, bioDataService.getTemplateForBioData(bioData), needsPayment);
    }

    /** Whether the authenticated user must pay before downloading this bio-data. */
    @GetMapping("/{id}/needs-payment")
    public Map<String, Boolean> needsPayment(@PathVariable Long id) {
        User user = currentUserOrNull();
        boolean needs = bioDataService.needsPayment(user, id);
        return Map.of("needsPayment", needs);
    }

    /** Download the bio-data as a PDF, or 402 with a {@code Location} header if payment is required. */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        User user = currentUserOrNull();
        if (bioDataService.needsPayment(user, id)) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .header(HttpHeaders.LOCATION, "/payment/checkout/" + id)
                    .build();
        }
        try {
            byte[] pdf = pdfService.generatePdf(id, user);
            String filename = "biodata_"
                    + bioDataService.getBioDataById(id).getFullName().replaceAll("\\s+", "_") + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IOException e) {
            log.error("Error generating PDF for bio-data {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private void requireFullName(BioDataDTO dto) {
        if (dto.getFullName() == null || dto.getFullName().isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }
    }

    private User currentUserOrNull() {
        return SecurityUtils.getCurrentUser().orElse(null);
    }

    private User requireUser() {
        return SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }
}
