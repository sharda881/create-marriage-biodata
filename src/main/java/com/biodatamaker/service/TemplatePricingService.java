package com.biodatamaker.service;

import com.biodatamaker.entity.TemplatePrice;
import com.biodatamaker.repository.TemplatePriceRepository;
import com.biodatamaker.template.BioDataTemplate;
import com.biodatamaker.template.BioDataTemplateFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-template pricing. Every template has an effective price: whatever an admin
 * set in {@code template_price}, or (if none set) a computed default — free
 * templates are 0, premium templates fall back to {@code system_config.download_price}.
 * A price of 0 means the template is free; anything above means it must be paid for.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplatePricingService {

    private final TemplatePriceRepository repository;
    private final BioDataTemplateFactory templateFactory;
    private final SystemConfigService configService;

    /** Effective price for a template id (never null; falls back to the default template). */
    public BigDecimal priceFor(String templateId) {
        BioDataTemplate template = templateFactory.getTemplateOrDefault(templateId);
        return repository.findById(template.getTemplateId())
                .map(TemplatePrice::getPrice)
                .orElseGet(() -> defaultPrice(template));
    }

    private BigDecimal defaultPrice(BioDataTemplate template) {
        if (!template.isPremium()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(configService.getDownloadPrice().trim());
        } catch (NumberFormatException e) {
            log.warn("download_price config is not a number: '{}' — falling back to 99", configService.getDownloadPrice());
            return new BigDecimal("99");
        }
    }

    /** Full pricing table for the admin screen: every known template + its effective price. */
    public List<Map<String, Object>> listing() {
        return templateFactory.getAllTemplates().stream()
                .sorted(Comparator.comparing(BioDataTemplate::getDisplayName))
                .map(t -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("templateId", t.getTemplateId());
                    row.put("name", t.getDisplayName());
                    row.put("premium", t.isPremium());
                    row.put("price", priceFor(t.getTemplateId()));
                    row.put("customPrice", repository.existsById(t.getTemplateId()));
                    return row;
                })
                .toList();
    }

    /** Admin override — set (or reset, with {@code null}) a template's price. */
    @Transactional
    public void setPrice(String templateId, BigDecimal price) {
        if (!templateFactory.templateExists(templateId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown template: " + templateId);
        }
        if (price == null || price.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "price must be a number >= 0");
        }
        TemplatePrice row = repository.findById(templateId)
                .orElseGet(() -> TemplatePrice.builder().templateId(templateId).build());
        row.setPrice(price.setScale(2, RoundingMode.HALF_UP));
        repository.save(row);
        log.info("Template '{}' price set to {}", templateId, row.getPrice());
    }

    /** Removes the admin override so the template falls back to its computed default. */
    @Transactional
    public void resetPrice(String templateId) {
        repository.deleteById(templateId);
    }
}
