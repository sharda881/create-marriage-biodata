package com.biodatamaker.service;

import com.biodatamaker.entity.BioData;
import com.biodatamaker.entity.User;
import com.biodatamaker.repository.BioDataRepository;
import com.biodatamaker.template.BioDataTemplate;
import com.biodatamaker.template.BioDataTemplateFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Single source of truth for "does this bio-data need to be paid for before the
 * PDF can be downloaded?". Rules (first match wins):
 *
 * <ol>
 *   <li>Paywall globally disabled -> free.</li>
 *   <li>Already PAID -> free.</li>
 *   <li>Premium template -> must pay (the free quota never covers premium designs).</li>
 *   <li>Free template + signed-in user still within the free download quota -> free.</li>
 *   <li>Otherwise (anonymous, or quota exhausted) -> must pay.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaywallService {

    private final SystemConfigService configService;
    private final BioDataRepository bioDataRepository;
    private final BioDataTemplateFactory templateFactory;

    public boolean needsPayment(BioData bioData, User user) {
        if (!configService.isPaywallEnabled()) {
            return false;
        }
        if (bioData.getPaymentStatus() == BioData.PaymentStatus.PAID || Boolean.TRUE.equals(bioData.getIsPaid())) {
            return false;
        }

        BioDataTemplate template = templateFactory.getTemplateOrDefault(bioData.getSelectedTemplateId());
        if (template.isPremium()) {
            return true;
        }

        if (user != null) {
            long used = bioDataRepository.countDownloadedByUser(user);
            if (used < configService.getFreeLimitCount()) {
                return false;
            }
        }
        return true;
    }

    /** Current price for a paid download, from {@code system_config.download_price}. */
    public BigDecimal price() {
        try {
            return new BigDecimal(configService.getDownloadPrice().trim());
        } catch (NumberFormatException e) {
            log.warn("download_price config is not a number: '{}' — falling back to 99", configService.getDownloadPrice());
            return new BigDecimal("99");
        }
    }
}
