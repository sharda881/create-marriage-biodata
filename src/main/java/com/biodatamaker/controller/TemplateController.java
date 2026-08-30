package com.biodatamaker.controller;

import com.biodatamaker.dto.TemplateDTO;
import com.biodatamaker.template.BioDataTemplateFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Read-only catalogue of bio-data design templates for the SPA gallery / picker.
 */
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final BioDataTemplateFactory templateFactory;

    @GetMapping
    public Map<String, List<TemplateDTO>> all() {
        List<TemplateDTO> templates = templateFactory.getAllTemplates().stream()
                .map(TemplateDTO::fromTemplate)
                .sorted(Comparator.comparing(TemplateDTO::premium).thenComparing(TemplateDTO::name))
                .toList();
        return Map.of(
                "all", templates,
                "free", templates.stream().filter(t -> !t.premium()).toList(),
                "premium", templates.stream().filter(TemplateDTO::premium).toList()
        );
    }

    @GetMapping("/{id}")
    public TemplateDTO one(@PathVariable String id) {
        return TemplateDTO.fromTemplate(templateFactory.getTemplate(id));
    }
}
