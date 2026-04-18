package com.biodatamaker.controller;

import com.biodatamaker.dto.BioDataDTO;
import com.biodatamaker.entity.BioData;
import com.biodatamaker.entity.User;
import com.biodatamaker.service.BioDataService;
import com.biodatamaker.service.PdfService;
import com.biodatamaker.template.BioDataTemplateFactory;
import com.biodatamaker.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

/**
 * Controller for BioData CRUD operations.
 */
@Controller
@RequestMapping("/biodata")
@RequiredArgsConstructor
@Slf4j
public class BioDataController {

    private final BioDataService bioDataService;
    private final PdfService pdfService;
    private final BioDataTemplateFactory templateFactory;

    /**
     * Show create bio-data form (Step 1: Personal Details)
     * @param template Optional template ID from URL (e.g., from template selection page)
     */
    @GetMapping("/create")
    public String createForm(@RequestParam(required = false) String template, Model model) {
        BioDataDTO bioDataDTO = new BioDataDTO();

        // Pre-select template if provided via URL
        if (template != null && !template.isBlank()) {
            bioDataDTO.setSelectedTemplateId(template);
        }

        model.addAttribute("bioData", bioDataDTO);
        model.addAttribute("templates", templateFactory.getAllTemplates());
        model.addAttribute("step", 1);
        model.addAttribute("isNew", true);
        return "biodata/form";
    }

    /**
     * Show edit bio-data form - supports both authenticated and anonymous users
     */
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id,
                           @RequestParam(defaultValue = "1") int step,
                           Model model) {
        User currentUser = getCurrentUserOrNull();
        BioData bioData;

        if (currentUser != null) {
            bioData = bioDataService.getBioDataForUser(id, currentUser);
        } else {
            // Anonymous user - get by ID only
            bioData = bioDataService.getBioDataById(id);
        }

        model.addAttribute("bioData", BioDataDTO.fromEntity(bioData));
        model.addAttribute("templates", templateFactory.getAllTemplates());
        model.addAttribute("step", step);
        model.addAttribute("isNew", false);
        model.addAttribute("isAnonymous", currentUser == null);
        return "biodata/form";
    }

    /**
     * Save bio-data (create or update) - supports both authenticated and anonymous users
     */
    @PostMapping("/save")
    public String saveBioData(@Valid @ModelAttribute("bioData") BioDataDTO bioDataDTO,
                              BindingResult bindingResult,
                              @RequestParam(required = false) Long id,
                              @RequestParam(defaultValue = "1") int step,
                              @RequestParam(required = false) String action,
                              @RequestParam(required = false) MultipartFile photoFile,
                              @RequestParam(required = false) String photoBase64,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUserOrNull(); // Can be null for anonymous users

        // Handle validation errors for required fields
        if (step == 1 && bindingResult.hasFieldErrors("fullName")) {
            model.addAttribute("templates", templateFactory.getAllTemplates());
            model.addAttribute("step", step);
            model.addAttribute("isNew", id == null);
            model.addAttribute("isAnonymous", currentUser == null);
            return "biodata/form";
        }

        try {
            BioData bioData;
            if (id != null) {
                bioData = bioDataService.updateBioData(id, currentUser, bioDataDTO);
            } else {
                bioData = bioDataService.createBioData(currentUser, bioDataDTO);
            }

            // Handle photo upload if provided
            if (photoFile != null && !photoFile.isEmpty()) {
                try {
                    bioDataService.uploadPhoto(bioData.getId(), currentUser, photoFile);
                } catch (IOException e) {
                    log.error("Error uploading photo", e);
                    redirectAttributes.addFlashAttribute("warning", "Photo upload failed, but bio-data was saved.");
                }
            } else if (photoBase64 != null && photoBase64.startsWith("data:image") && (bioData.getPhotoPath() == null || !bioData.getPhotoPath().startsWith("/uploads"))) {
                // Handle base64 encoded photo from preview
                try {
                    bioDataService.uploadPhotoFromBase64(bioData.getId(), currentUser, photoBase64);
                } catch (IOException e) {
                    log.error("Error uploading photo from base64", e);
                    redirectAttributes.addFlashAttribute("warning", "Photo upload failed, but bio-data was saved.");
                }
            }

            // Handle different actions
            if ("save_draft".equals(action)) {
                redirectAttributes.addFlashAttribute("success", "Bio-data saved as draft!");
                if (currentUser != null) {
                    return "redirect:/dashboard";
                } else {
                    // Anonymous user - redirect to preview instead
                    return "redirect:/biodata/preview/" + bioData.getId();
                }
            } else if ("next".equals(action) && step < 6) {
                // Go to next step
                return "redirect:/biodata/edit/" + bioData.getId() + "?step=" + (step + 1);
            } else if ("complete".equals(action)) {
                bioDataService.completeBioData(bioData.getId(), currentUser);
                redirectAttributes.addFlashAttribute("success", "Bio-data completed successfully!");
                return "redirect:/biodata/preview/" + bioData.getId();
            }

            // Default: go to next step or stay on same step
            return "redirect:/biodata/edit/" + bioData.getId() + "?step=" + step;

        } catch (Exception e) {
            log.error("Error saving bio-data", e);
            model.addAttribute("error", "Error saving bio-data: " + e.getMessage());
            model.addAttribute("templates", templateFactory.getAllTemplates());
            model.addAttribute("step", step);
            model.addAttribute("isNew", id == null);
            model.addAttribute("isAnonymous", currentUser == null);
            return "biodata/form";
        }
    }

    /**
     * Upload photo for bio-data - supports both authenticated and anonymous users
     */
    @PostMapping("/{id}/upload-photo")
    public String uploadPhoto(@PathVariable Long id,
                              @RequestParam("photo") MultipartFile photo,
                              @RequestParam(defaultValue = "1") int step,
                              RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUserOrNull();

        try {
            String photoPath = bioDataService.uploadPhoto(id, currentUser, photo);
            redirectAttributes.addFlashAttribute("success", "Photo uploaded successfully!");
        } catch (IOException e) {
            log.error("Error uploading photo", e);
            redirectAttributes.addFlashAttribute("error", "Error uploading photo: " + e.getMessage());
        }

        return "redirect:/biodata/edit/" + id + "?step=" + step;
    }

    /**
     * Update template selection - supports both authenticated and anonymous users
     */
    @PostMapping("/{id}/template")
    public String updateTemplate(@PathVariable Long id,
                                 @RequestParam String templateId,
                                 RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUserOrNull();

        try {
            bioDataService.updateTemplate(id, currentUser, templateId);
            redirectAttributes.addFlashAttribute("success", "Template updated!");
        } catch (Exception e) {
            log.error("Error updating template", e);
            redirectAttributes.addFlashAttribute("error", "Error updating template: " + e.getMessage());
        }

        return "redirect:/biodata/preview/" + id;
    }

    /**
     * Preview bio-data with selected template - supports both authenticated and anonymous users
     */
    @GetMapping("/preview/{id}")
    public String preview(@PathVariable Long id, Model model) {
        User currentUser = getCurrentUserOrNull();
        BioData bioData;

        if (currentUser != null) {
            bioData = bioDataService.getBioDataForUser(id, currentUser);
            model.addAttribute("needsPayment", bioDataService.needsPayment(currentUser, id));
        } else {
            // Anonymous user - get by ID only
            bioData = bioDataService.getBioDataById(id);
            model.addAttribute("needsPayment", false); // Anonymous users don't have payment restrictions for preview
        }

        model.addAttribute("bioData", bioData);
        model.addAttribute("template", bioDataService.getTemplateForBioData(bioData));
        model.addAttribute("templates", templateFactory.getAllTemplates());
        model.addAttribute("isAnonymous", currentUser == null);

        return "biodata/preview";
    }

    /**
     * Download bio-data as PDF - supports both authenticated and anonymous users
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        User currentUser = getCurrentUserOrNull();

        try {
            // Check if payment is needed (only for authenticated users)
            if (currentUser != null && bioDataService.needsPayment(currentUser, id)) {
                return ResponseEntity.status(402)
                        .header("Location", "/payment/checkout/" + id)
                        .build();
            }

            byte[] pdfBytes = pdfService.generatePdf(id, currentUser);
            BioData bioData = bioDataService.getBioDataById(id);

            String filename = "biodata_" + bioData.getFullName().replaceAll("\\s+", "_") + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (IOException e) {
            log.error("Error generating PDF", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Delete bio-data
     */
    @PostMapping("/delete/{id}")
    public String deleteBioData(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();

        try {
            bioDataService.deleteBioData(id, currentUser);
            redirectAttributes.addFlashAttribute("success", "Bio-data deleted successfully!");
        } catch (Exception e) {
            log.error("Error deleting bio-data", e);
            redirectAttributes.addFlashAttribute("error", "Error deleting bio-data: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }

    /**
     * View a specific bio-data in read-only mode
     */
    @GetMapping("/view/{id}")
    public String viewBioData(@PathVariable Long id, Model model) {
        User currentUser = getCurrentUser();
        BioData bioData = bioDataService.getBioDataForUser(id, currentUser);

        model.addAttribute("bioData", bioData);
        model.addAttribute("template", bioDataService.getTemplateForBioData(bioData));
        model.addAttribute("readonly", true);

        return "biodata/view";
    }

    // Helper method to get current user (throws exception if not authenticated)
    private User getCurrentUser() {
        return SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("User not authenticated"));
    }

    // Helper method to get current user or null for anonymous users
    private User getCurrentUserOrNull() {
        return SecurityUtils.getCurrentUser().orElse(null);
    }
}
