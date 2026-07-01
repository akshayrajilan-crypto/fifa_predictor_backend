package com.fifaworldcup.Fifa.service;

import com.fifaworldcup.Fifa.model.PushSubscription;
import com.fifaworldcup.Fifa.repository.PushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.security.Security;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final PushSubscriptionRepository subscriptionRepository;

    // VAPID keys - generated for this app
    private static final String VAPID_PUBLIC_KEY = "BB3xCkVsqwU5pZk_wFjmZ_-vBXNh_MfCCHI2B1mvWxMUj4GorZ4mDX4DPh1SECuPY7JJYxHYhsJoAf72YXULMo0";
    private static final String VAPID_PRIVATE_KEY = "56pahWGG7y1CoLjVTb4kqjCzyZiSCvg4--J0FK-n7Q4";

    private PushService pushService;

    @PostConstruct
    public void init() {
        try {
            Security.addProvider(new BouncyCastleProvider());
            pushService = new PushService();
            pushService.setPublicKey(VAPID_PUBLIC_KEY);
            pushService.setPrivateKey(VAPID_PRIVATE_KEY);
            pushService.setSubject("mailto:admin@wc26predictor.com");
            log.info("✅ Web Push service initialized");
        } catch (Exception e) {
            log.error("Failed to initialize push service: {}", e.getMessage());
        }
    }

    public String getPublicKey() {
        return VAPID_PUBLIC_KEY;
    }

    /**
     * Send notification to all subscribed users
     */
    public void sendToAll(String title, String body) {
        List<PushSubscription> subscriptions = subscriptionRepository.findAll();
        log.info("📤 Sending push to {} subscription(s): {}", subscriptions.size(), title);

        for (PushSubscription sub : subscriptions) {
            sendToSubscription(sub, title, body);
        }
    }

    /**
     * Send notification to a specific user
     */
    public void sendToUser(String username, String title, String body) {
        List<PushSubscription> subscriptions = subscriptionRepository.findByUsername(username);
        for (PushSubscription sub : subscriptions) {
            sendToSubscription(sub, title, body);
        }
    }

    private void sendToSubscription(PushSubscription sub, String title, String body) {
        if (pushService == null) return;

        try {
            String payload = String.format("{\"title\":\"%s\",\"body\":\"%s\",\"icon\":\"/wc26-icon.png\"}",
                    escapeJson(title), escapeJson(body));

            Notification notification = new Notification(
                    sub.getEndpoint(),
                    sub.getP256dh(),
                    sub.getAuth(),
                    payload.getBytes()
            );

            pushService.send(notification);
        } catch (Exception e) {
            log.warn("Failed to send push to {}: {}", sub.getUsername(), e.getMessage());
            // If subscription is invalid (410 Gone), remove it
            if (e.getMessage() != null && e.getMessage().contains("410")) {
                subscriptionRepository.delete(sub);
                log.info("Removed invalid subscription for {}", sub.getUsername());
            }
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
