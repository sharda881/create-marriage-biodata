package com.biodatamaker.controller;

import com.biodatamaker.dto.BioDataDTO;
import com.biodatamaker.dto.ProfileUpdateRequest;
import com.biodatamaker.dto.UserDTO;
import com.biodatamaker.entity.BioData;
import com.biodatamaker.entity.User;
import com.biodatamaker.service.BioDataService;
import com.biodatamaker.service.SystemConfigService;
import com.biodatamaker.service.UserService;
import com.biodatamaker.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Dashboard summary and user profile endpoints.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final BioDataService bioDataService;
    private final UserService userService;
    private final SystemConfigService configService;

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        User user = requireUser();
        List<BioDataDTO> bioDatas = bioDataService.getUserBioDataList(user);

        long completed = bioDatas.stream()
                .filter(b -> b.getStatus() == BioData.BioDataStatus.COMPLETED).count();
        long draft = bioDatas.stream()
                .filter(b -> b.getStatus() == BioData.BioDataStatus.DRAFT).count();

        int freeLimit = configService.getFreeLimitCount();
        long downloadedCount = bioDatas.stream()
                .filter(b -> Boolean.TRUE.equals(b.getIsPaid())).count();
        int freeRemaining = Math.max(0, freeLimit - (int) downloadedCount);

        return Map.of(
                "user", userService.getUserDTO(user.getId()),
                "bioDatas", bioDatas,
                "totalBioData", bioDatas.size(),
                "completedBioData", completed,
                "draftBioData", draft,
                "freeLimit", freeLimit,
                "freeRemaining", freeRemaining
        );
    }

    @GetMapping("/profile")
    public UserDTO profile() {
        return userService.getUserDTO(requireUser().getId());
    }

    @PutMapping("/profile")
    public UserDTO updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        User user = requireUser();
        User updated = userService.updateProfile(user.getId(), request.name(), request.phone());
        return UserDTO.fromEntity(updated);
    }

    private User requireUser() {
        return SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }
}
