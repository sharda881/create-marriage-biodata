package com.biodatamaker.service;

import com.biodatamaker.entity.BioData;
import com.biodatamaker.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Single source of truth for "does this bio-data need to be paid for before the
 * PDF can be downloaded?". Every template has an effective price (see
 * {@link TemplatePricingService}) — a price of 0 means the template is free for
 * everyone; anything above must be paid, per bio-data, regardless of who's asking.
 * Rules (first match wins):
 *
 * <ol>
 *   <li>Paywall globally disabled -> free.</li>
 *   <li>Already PAID -> free.</li>
 *   <li>Template price is 0 -> free.</li>
 *   <li>Otherwise -> must pay.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaywallService {

    private final SystemConfigService configService;
    private final TemplatePricingService templatePricingService;

    /** {@code user} is accepted for API symmetry with callers but no longer changes the outcome. */
    public boolean needsPayment(BioData bioData, User user) {
        if (!configService.isPaywallEnabled()) {
            return false;
        }
        if (bioData.getPaymentStatus() == BioData.PaymentStatus.PAID || Boolean.TRUE.equals(bioData.getIsPaid())) {
            return false;
        }
        return priceFor(bioData).signum() > 0;
    }

    /** Effective price for this bio-data's selected template. */
    public BigDecimal priceFor(BioData bioData) {
        return templatePricingService.priceFor(bioData.getSelectedTemplateId());
    }
}
