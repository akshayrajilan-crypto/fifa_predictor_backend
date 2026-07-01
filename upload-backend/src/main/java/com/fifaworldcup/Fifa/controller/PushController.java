package com.fifaworldcup.Fifa.controller;

import com.fifaworldcup.Fifa.model.PushSubscription;
import com.fifaworldcup.Fifa.repository.PushSubscriptionRepository;
import com.fifaworldcup.Fifa.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final PushSubscriptionRepository subscriptionRepository;
    private final PushNotificationService pushNotificationService;

    /**
     * Get the VAPID public key (needed by frontend to subscribe)
     */
    @GetMapping("/vapid-key")
    public ResponseEntity<Map<String, String>> getVapidKey() {
        return ResponseEntity.ok(Map.of("key", pushNotificationService.getPublicKey()));
    }

    /**
     * Subscribe to push notifications
     */
    @PostMapping("/subscribe")
    @Transactional
    public ResponseEntity<String> subscribe(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> body) {
        String endpoint = (String) body.get("endpoint");
        @SuppressWarnings("unchecked")
        Map<String, String> keys = (Map<String, String>) body.get("keys");

        if (endpoint == null || keys == null) {
            return ResponseEntity.badRequest().body("Invalid subscription data");
        }

        String p256dh = keys.get("p256dh");
        String auth = keys.get("auth");

        // Check if already subscribed with this endpoint
        if (subscriptionRepository.findByEndpoint(endpoint).isPresent()) {
            return ResponseEntity.ok("Already subscribed");
        }

        subscriptionRepository.save(PushSubscription.builder()
                .username(userDetails.getUsername())
                .endpoint(endpoint)
                .p256dh(p256dh)
                .auth(auth)
                .build());

        return ResponseEntity.ok("Subscribed to notifications");
    }

    /**
     * Unsubscribe from push notifications
     */
    @PostMapping("/unsubscribe")
    @Transactional
    public ResponseEntity<String> unsubscribe(@RequestBody Map<String, String> body) {
        String endpoint = body.get("endpoint");
        if (endpoint != null) {
            subscriptionRepository.deleteByEndpoint(endpoint);
        }
        return ResponseEntity.ok("Unsubscribed");
    }
}
