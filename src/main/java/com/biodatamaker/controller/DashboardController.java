package com.biodatamaker.controller;

import com.biodatamaker.dto.BioDataDTO;
import com.biodatamaker.entity.User;
import com.biodatamaker.service.BioDataService;
import com.biodatamaker.service.SystemConfigService;
import com.biodatamaker.service.UserService;
import com.biodatamaker.template.BioDataTemplateFactory;
import com.biodatamaker.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Controller for dashboard operations.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final BioDataService bioDataService;
    private final UserService userService;
    private final BioDataTemplateFactory templateFactory;
    private final SystemConfigService configService;

    /**
     * Main dashboard page
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("User not authenticated"));

        // Get user's bio-data list
        List<BioDataDTO> bioDatas = bioDataService.getUserBioDataList(currentUser);

        // Calculate stats
        long totalBioData = bioDatas.size();
        long completedBioData = bioDatas.stream()
                .filter(b -> b.getStatus() == com.biodatamaker.entity.BioData.BioDataStatus.COMPLETED)
                .count();
        long draftBioData = bioDatas.stream()
                .filter(b -> b.getStatus() == com.biodatamaker.entity.BioData.BioDataStatus.DRAFT)
                .count();

        // Free downloads info
        int freeLimit = configService.getFreeLimitCount();
        long downloadedCount = bioDatas.stream()
                .filter(b -> b.getIsPaid() != null && b.getIsPaid())
                .count();
        int freeRemaining = Math.max(0, freeLimit - (int) downloadedCount);

        model.addAttribute("user", currentUser);
        model.addAttribute("bioDatas", bioDatas);
        model.addAttribute("totalBioData", totalBioData);
        model.addAttribute("completedBioData", completedBioData);
        model.addAttribute("draftBioData", draftBioData);
        model.addAttribute("freeLimit", freeLimit);
        model.addAttribute("freeRemaining", freeRemaining);
        model.addAttribute("templates", templateFactory.getAllTemplates());

        return "dashboard";
    }

    /**
     * Template gallery page
     */
    @GetMapping("/templates")
    public String templateGallery(Model model) {
        model.addAttribute("templates", templateFactory.getAllTemplates());
        model.addAttribute("freeTemplates", templateFactory.getFreeTemplates());
        model.addAttribute("premiumTemplates", templateFactory.getPremiumTemplates());
        return "templates";
    }

    /**
     * Profile page
     */
    @GetMapping("/profile")
    public String profile(Model model) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("User not authenticated"));

        model.addAttribute("user", currentUser);
        return "profile";
    }
}
