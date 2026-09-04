package com.biodatamaker.event;

/**
 * Published once a bio-data transitions to PAID (webhook, reconcile, or admin
 * override). Listened to by {@code PdfDeliveryListener} to email the PDF if the
 * buyer asked for it — kept as an event so delivery only fires after the payment
 * transaction actually commits.
 */
public record BioDataPaidEvent(Long bioDataId) {
}
