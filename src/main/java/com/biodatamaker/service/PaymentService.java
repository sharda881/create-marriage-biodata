package com.biodatamaker.service;

import com.biodatamaker.dto.PaymentDTO;
import com.biodatamaker.entity.BioData;
import com.biodatamaker.entity.User;
import com.biodatamaker.repository.BioDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Razorpay-backed payment flow for unlocking bio-data PDF downloads.
 *
 * <p>Payment state lives directly on {@link BioData}. The webhook is the source of
 * truth; {@link #status(Long)} additionally reconciles against Razorpay so a
 * dropped webhook still resolves.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final BioDataRepository bioDataRepository;
    private final BioDataService bioDataService;
    private final RazorpayService razorpay;
    private final PaywallService paywallService;

    // ---------------------------------------------------------------- checkout

    /** What the SPA needs to decide whether/what to charge before opening checkout. */
    public Map<String, Object> quote(Long bioDataId, User user) {
        BioData bioData = bioDataService.getBioDataById(bioDataId);
        boolean needsPayment = paywallService.needsPayment(bioData, user);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("bioDataId", bioDataId);
        out.put("needsPayment", needsPayment);
        out.put("alreadyPaid", bioData.getPaymentStatus() == BioData.PaymentStatus.PAID);
        out.put("price", paywallService.price());
        out.put("currency", razorpay.getCurrency());
        out.put("keyId", razorpay.getKeyId());
        return out;
    }

    /**
     * Create (or replace) the Razorpay order for a bio-data and return the payload
     * the Razorpay Checkout widget needs.
     */
    @Transactional
    public Map<String, Object> createCheckout(Long bioDataId, User user, String name, String email, String phone) {
        BioData bioData = bioDataService.getBioDataById(bioDataId);

        if (bioData.getPaymentStatus() == BioData.PaymentStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This bio-data is already paid for");
        }
        if (!paywallService.needsPayment(bioData, user)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No payment is required for this download");
        }

        BigDecimal amount = paywallService.price();
        String orderId = razorpay.createOrder(
                amount,
                "biodata_" + bioDataId,
                Map.of("bioDataId", String.valueOf(bioDataId)));

        bioData.setRazorpayOrderId(orderId);
        bioData.setRazorpayPaymentId(null);
        bioData.setPaymentStatus(BioData.PaymentStatus.PENDING);
        bioData.setPaymentAmount(amount);
        bioData.setPayerName(blankToNull(name));
        bioData.setPayerEmail(blankToNull(email));
        bioData.setPayerPhone(blankToNull(phone));
        bioDataRepository.save(bioData);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("orderId", orderId);
        out.put("keyId", razorpay.getKeyId());
        out.put("amount", amount.movePointRight(2).longValueExact()); // paise, for the widget
        out.put("currency", razorpay.getCurrency());
        out.put("bioDataId", bioDataId);
        out.put("prefillName", bioData.getPayerName());
        out.put("prefillEmail", bioData.getPayerEmail());
        out.put("prefillContact", bioData.getPayerPhone());
        return out;
    }

    // ---------------------------------------------------------------- webhook

    /** Handle a Razorpay webhook. Verifies the signature, then applies the event idempotently. */
    @Transactional
    public void handleWebhook(String rawBody, String signature) {
        if (!razorpay.verifyWebhookSignature(rawBody, signature)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook signature");
        }

        JSONObject event = new JSONObject(rawBody);
        String type = event.optString("event");

        JSONObject paymentEntity = nested(nested(nested(event, "payload"), "payment"), "entity");
        String orderId = emptyToNull(paymentEntity.optString("order_id", ""));
        String paymentId = emptyToNull(paymentEntity.optString("id", ""));

        if (orderId == null) {
            log.info("Ignoring Razorpay webhook '{}' with no order id", type);
            return;
        }

        switch (type) {
            case "payment.captured", "order.paid" -> applyPaid(orderId, paymentId);
            case "payment.failed" -> applyFailed(orderId);
            default -> log.debug("Ignoring Razorpay webhook event '{}'", type);
        }
    }

    // ---------------------------------------------------------------- status / reconcile

    /** Current payment state for a bio-data; reconciles a stale PENDING against Razorpay. */
    @Transactional
    public Map<String, Object> status(Long bioDataId) {
        BioData bioData = bioDataService.getBioDataById(bioDataId);

        if (bioData.getPaymentStatus() == BioData.PaymentStatus.PENDING
                && bioData.getRazorpayOrderId() != null) {
            String orderStatus = razorpay.fetchOrderStatus(bioData.getRazorpayOrderId());
            if ("paid".equals(orderStatus)) {
                applyPaid(bioData.getRazorpayOrderId(),
                        razorpay.fetchCapturedPaymentId(bioData.getRazorpayOrderId()));
                bioData = bioDataService.getBioDataById(bioDataId);
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("bioDataId", bioDataId);
        out.put("paymentStatus", bioData.getPaymentStatus());
        out.put("canDownload", bioData.getPaymentStatus() == BioData.PaymentStatus.PAID);
        return out;
    }

    // ---------------------------------------------------------------- admin

    public List<PaymentDTO> listByStatus(BioData.PaymentStatus status) {
        return bioDataRepository.findByPaymentStatusOrderByUpdatedAtDesc(status)
                .stream().map(PaymentDTO::fromEntity).toList();
    }

    public long countByStatus(BioData.PaymentStatus status) {
        return bioDataRepository.countByPaymentStatus(status);
    }

    public BigDecimal totalRevenue() {
        return bioDataRepository.sumPaymentAmountByStatus(BioData.PaymentStatus.PAID);
    }

    public PaymentDTO getPaymentView(Long bioDataId) {
        return PaymentDTO.fromEntity(bioDataService.getBioDataById(bioDataId));
    }

    /** Admin override: force a bio-data to PAID (e.g. UPI paid out-of-band). */
    @Transactional
    public PaymentDTO markPaidManually(Long bioDataId, User admin) {
        BioData bioData = bioDataService.getBioDataById(bioDataId);
        if (bioData.getPaymentStatus() != BioData.PaymentStatus.PAID) {
            bioData.markPaid(bioData.getRazorpayPaymentId());
            if (bioData.getPaymentAmount() == null) {
                bioData.setPaymentAmount(paywallService.price());
            }
            bioDataRepository.save(bioData);
            log.info("Bio-data {} marked PAID manually by admin {}", bioDataId,
                    admin != null ? admin.getId() : "?");
        }
        return PaymentDTO.fromEntity(bioData);
    }

    // ---------------------------------------------------------------- internals

    private void applyPaid(String orderId, String paymentId) {
        bioDataRepository.findByRazorpayOrderId(orderId).ifPresentOrElse(bioData -> {
            if (bioData.getPaymentStatus() == BioData.PaymentStatus.PAID) {
                return; // idempotent
            }
            bioData.markPaid(paymentId);
            bioDataRepository.save(bioData);
            log.info("Bio-data {} marked PAID (order {}, payment {})", bioData.getId(), orderId, paymentId);
        }, () -> log.warn("Razorpay order {} does not match any bio-data", orderId));
    }

    private void applyFailed(String orderId) {
        bioDataRepository.findByRazorpayOrderId(orderId).ifPresent(bioData -> {
            if (bioData.getPaymentStatus() == BioData.PaymentStatus.PENDING) {
                bioData.setPaymentStatus(BioData.PaymentStatus.FAILED);
                bioDataRepository.save(bioData);
                log.info("Bio-data {} payment FAILED (order {})", bioData.getId(), orderId);
            }
        });
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }

    private static JSONObject nested(JSONObject parent, String key) {
        JSONObject child = parent.optJSONObject(key);
        return child != null ? child : new JSONObject();
    }
}
