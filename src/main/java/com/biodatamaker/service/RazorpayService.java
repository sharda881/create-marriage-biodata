package com.biodatamaker.service;

import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around the Razorpay Java SDK. Keeps the SDK/JSON types out of the
 * rest of the codebase and centralises credential handling.
 */
@Service
@Slf4j
public class RazorpayService {

    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final String currency;

    private volatile RazorpayClient client;

    public RazorpayService(
            @Value("${razorpay.key-id:}") String keyId,
            @Value("${razorpay.key-secret:}") String keySecret,
            @Value("${razorpay.webhook-secret:}") String webhookSecret,
            @Value("${razorpay.currency:INR}") String currency) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
        this.currency = currency;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(keyId) && StringUtils.hasText(keySecret);
    }

    public String getKeyId() {
        return keyId;
    }

    public String getCurrency() {
        return currency;
    }

    private RazorpayClient client() {
        RazorpayClient c = client;
        if (c == null) {
            synchronized (this) {
                if (client == null) {
                    if (!isConfigured()) {
                        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                "Payments are not configured on this server");
                    }
                    try {
                        client = new RazorpayClient(keyId, keySecret);
                    } catch (RazorpayException e) {
                        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                "Could not initialise the payment gateway", e);
                    }
                }
                c = client;
            }
        }
        return c;
    }

    /**
     * Create a Razorpay order for the given rupee amount.
     *
     * @return the Razorpay order id (e.g. {@code order_ABC123})
     */
    public String createOrder(BigDecimal amountInr, String receipt, Map<String, String> notes) {
        JSONObject request = new JSONObject();
        request.put("amount", amountInr.movePointRight(2).longValueExact()); // paise
        request.put("currency", currency);
        request.put("receipt", receipt);
        request.put("payment_capture", true);
        if (notes != null && !notes.isEmpty()) {
            request.put("notes", new JSONObject(notes));
        }
        try {
            Order order = client().orders.create(request);
            return order.get("id");
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed for receipt {}", receipt, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not start the payment", e);
        }
    }

    /** Order status as reported by Razorpay: {@code created}, {@code attempted}, {@code paid}. */
    public String fetchOrderStatus(String orderId) {
        try {
            return client().orders.fetch(orderId).get("status");
        } catch (RazorpayException e) {
            log.warn("Could not fetch Razorpay order {}: {}", orderId, e.getMessage());
            return null;
        }
    }

    /** The id of the first captured payment on an order, or {@code null}. */
    public String fetchCapturedPaymentId(String orderId) {
        try {
            List<Payment> payments = client().orders.fetchPayments(orderId);
            for (Payment p : payments) {
                if ("captured".equals(p.get("status"))) {
                    return p.get("id");
                }
            }
        } catch (RazorpayException e) {
            log.warn("Could not fetch payments for Razorpay order {}: {}", orderId, e.getMessage());
        }
        return null;
    }

    public boolean verifyWebhookSignature(String rawBody, String signature) {
        if (!StringUtils.hasText(webhookSecret)) {
            log.error("razorpay.webhook-secret is not set — rejecting webhook");
            return false;
        }
        try {
            return Utils.verifyWebhookSignature(rawBody, signature, webhookSecret);
        } catch (RazorpayException e) {
            log.warn("Webhook signature verification threw: {}", e.getMessage());
            return false;
        }
    }
}
