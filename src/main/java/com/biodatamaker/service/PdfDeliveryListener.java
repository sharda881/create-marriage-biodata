package com.biodatamaker.service;

import com.biodatamaker.entity.BioData;
import com.biodatamaker.event.BioDataPaidEvent;
import com.biodatamaker.repository.BioDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

/**
 * Emails the PDF once a bio-data is confirmed PAID, if the buyer opted in at
 * checkout. Runs after the payment transaction commits ({@code AFTER_COMMIT}) so
 * it never sees a PENDING row, and off the request thread ({@code @Async}) so
 * PDF rendering + SMTP never slow down the webhook / status response.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PdfDeliveryListener {

    private final BioDataRepository bioDataRepository;
    private final PdfService pdfService;
    private final EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBioDataPaid(BioDataPaidEvent event) {
        deliver(event.bioDataId());
    }

    private void deliver(Long bioDataId) {
        BioData bioData = bioDataRepository.findById(bioDataId).orElse(null);
        if (bioData == null) {
            return;
        }
        if (!Boolean.TRUE.equals(bioData.getDeliverByEmail())) {
            return;
        }
        if (bioData.getPdfEmailedAt() != null) {
            return; // already sent — idempotent
        }
        if (bioData.getPaymentStatus() != BioData.PaymentStatus.PAID) {
            return;
        }
        if (bioData.getPayerEmail() == null || bioData.getPayerEmail().isBlank()) {
            log.warn("Bio-data {} asked for email delivery but has no payer email", bioDataId);
            return;
        }
        try {
            byte[] pdf = pdfService.generatePdfFromBioData(bioData);
            emailService.sendBioDataPdf(bioData.getPayerEmail(), bioData.getFullName(), pdf);
            bioData.setPdfEmailedAt(LocalDateTime.now());
            bioDataRepository.save(bioData);
        } catch (Exception e) {
            log.error("Post-payment PDF email failed for bio-data {}: {}", bioDataId, e.getMessage());
        }
    }
}
